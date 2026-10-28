package JuegoPetNine.red;

import JuegoPetNine.BD.GestorBD;
import JuegoPetNine.logica.JuegoNineControlador;
import JuegoPetNine.modelo.TipoCarta;
import JuegoPetNine.modelo.Jugador;
import JuegoPetNine.modelo.Habilidad;
import JuegoPetNine.modelo.Color;
import JuegoPetNine.modelo.Carta;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import JuegoPetNine.modelo.TipoHabilidad;

/**
 * Servidor de Nine!.
 *
 * - Un hilo "aceptador" espera conexiones nuevas (ServerSocket.accept()).
 * - Cada jugador conectado tiene su propio ManejadorCliente (hilos lector y escritor).
 * - El servidor es la UNICA autoridad: guarda el JuegoNineControlador real, valida cada
 *   jugada y le manda a cada cliente solo lo que ese jugador puede ver (su mano, la mesa
 *   y cuantas cartas tienen los rivales).
 *
 * Como varios hilos llegan al mismo tiempo, todo lo que toca el juego esta dentro de
 * synchronized(lock): se procesa un mensaje a la vez.
 */
public class ServidorNine {

    private final int puerto;
    private final int numJugadores;
    private final Object lock = new Object();
    private final List<ManejadorCliente> clientes = new ArrayList<>();
    private final Random azar = new Random();

    private ServerSocket serverSocket;
    private volatile boolean activo = false;
    private JuegoNineControlador juego;
    private boolean partidaIniciada = false;
    private boolean partidaTerminada = false;

    // Temporizador de turno: si el jugador no actua en 'segundosTurno', se le pasa el turno
    private final ScheduledExecutorService reloj = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Nine-Reloj");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> tareaTurno;
    private int contadorTurno = 0;      // cada vez que se reinicia el reloj sube: una tarea vieja se ignora
    private int segundosTurno = JuegoNineControlador.SEGUNDOS_POR_TURNO;
    private final List<String> rendidos = new ArrayList<>();   // jugadores que se rindieron (cuentan como derrota)

    public ServidorNine(int puerto, int numJugadores) {
        this.puerto = puerto;
        this.numJugadores = numJugadores;
    }

    /** Cambia el tiempo por turno (se usa en las pruebas). */
    public void setSegundosTurno(int segundos) {
        this.segundosTurno = segundos;
    }

    // ------------------------------------------------------------------ arranque

    /** Abre el puerto y empieza a aceptar jugadores en un hilo aparte. */
    public void iniciar() throws IOException {
        serverSocket = new ServerSocket(puerto);
        activo = true;
        Thread aceptador = new Thread(this::aceptarConexiones, "Nine-Aceptador");
        aceptador.setDaemon(true);
        aceptador.start();
        log("Servidor escuchando en el puerto " + puerto + " (" + numJugadores + " jugadores)");
    }

    public void detener() {
        activo = false;
        reloj.shutdownNow();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // nada que hacer
        }
        synchronized (lock) {
            for (ManejadorCliente m : clientes) {
                m.cerrarYa();
            }
        }
    }

    private void aceptarConexiones() {
        while (activo) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                ManejadorCliente manejador = null;
                synchronized (lock) {
                    if (!partidaIniciada && clientes.size() < numJugadores) {
                        manejador = new ManejadorCliente(this, socket);
                        clientes.add(manejador);
                    }
                }
                if (manejador == null) {
                    rechazar(socket);
                } else {
                    Thread hilo = new Thread(manejador, "Nine-Lector-" + socket.getRemoteSocketAddress());
                    hilo.setDaemon(true);
                    hilo.start();
                }
            } catch (IOException e) {
                if (activo) {
                    log("Error al aceptar una conexion: " + e.getMessage());
                }
            }
        }
    }

    private void rechazar(Socket socket) {
        try (Socket s = socket) {
            String msg = error("La partida está llena o ya comenzó.") + "\n";
            s.getOutputStream().write(msg.getBytes(StandardCharsets.UTF_8));
            s.getOutputStream().flush();
        } catch (IOException e) {
            // da igual, se cierra de todos modos
        }
    }

    // ------------------------------------------------------------------ registro de jugadores

    private void registrar(ManejadorCliente m, String nombre) {
        if (m.getNombre() != null || partidaIniciada) {
            return;
        }
        m.setNombre(nombreUnico(Protocolo.limpiar(nombre)));
        // A cada jugador se le sortean 2 habilidades distintas y elige una
        TipoHabilidad[] opciones = TipoHabilidad.sortearDos(azar);
        m.setOpciones(opciones);
        m.enviar(Protocolo.armar(Protocolo.BIENVENIDA, m.getNombre()));
        m.enviar(Protocolo.armar(Protocolo.OPCIONES, opciones[0].name(), opciones[1].name()));
        log(m.getNombre() + " se conecto (" + contarRegistrados() + "/" + numJugadores + "), opciones: "
                + opciones[0] + " / " + opciones[1]);
        avisarEspera();
    }

    /** ELEGIR|HABILIDAD: el jugador solo puede elegir una de las 2 que se le ofrecieron. */
    private void elegirHabilidad(ManejadorCliente m, String texto) {
        if (m.getNombre() == null || partidaIniciada || m.isHabilidadElegida()) {
            return;
        }
        TipoHabilidad elegida = null;
        for (TipoHabilidad t : m.getOpciones()) {
            if (t.name().equals(texto)) {
                elegida = t;
            }
        }
        if (elegida == null) {
            m.enviar(error("Esa habilidad no es una de tus dos opciones."));
            return;
        }
        m.setHabilidad(elegida);
        m.setHabilidadElegida(true);
        log(m.getNombre() + " eligio " + elegida);

        // La partida empieza cuando estan todos y todos eligieron
        if (contarRegistrados() == numJugadores && contarListos() == numJugadores) {
            iniciarPartida();
        } else {
            avisarEspera();
        }
    }

    private String nombreUnico(String base) {
        String candidato = base;
        int n = 2;
        while (nombreEnUso(candidato)) {
            candidato = base + "_" + n++;
        }
        return candidato;
    }

    private boolean nombreEnUso(String nombre) {
        for (ManejadorCliente m : clientes) {
            if (m.getNombre() != null && m.getNombre().equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }

    private int contarRegistrados() {
        int n = 0;
        for (ManejadorCliente m : clientes) {
            if (m.getNombre() != null) {
                n++;
            }
        }
        return n;
    }

    private int contarListos() {
        int n = 0;
        for (ManejadorCliente m : clientes) {
            if (m.getNombre() != null && m.isHabilidadElegida()) {
                n++;
            }
        }
        return n;
    }

    private void avisarEspera() {
        String texto = contarRegistrados() < numJugadores
                ? "Esperando jugadores... (" + contarRegistrados() + "/" + numJugadores + ")"
                : "Esperando que todos elijan su habilidad... (" + contarListos() + "/" + numJugadores + ")";
        difundir(Protocolo.armar(Protocolo.ESPERANDO, texto));
    }

    private void iniciarPartida() {
        List<String> nombres = new ArrayList<>();
        for (ManejadorCliente m : clientes) {
            nombres.add(m.getNombre());
        }
        juego = new JuegoNineControlador(nombres);
        for (int i = 0; i < clientes.size(); i++) {
            Jugador j = juego.getJugadores().get(i);
            j.setHabilidad(new Habilidad(clientes.get(i).getHabilidad()));
            clientes.get(i).setJugador(j);
        }
        juego.iniciar();
        partidaIniciada = true;
        log("Comienza la partida: " + nombres);
        difundir(Protocolo.armar(Protocolo.MENSAJE,
                "¡Comienza la partida! Empieza " + juego.getJugadorActual().getNombre()));
        enviarEstadoATodos();
        programarTemporizador();
    }

    // ------------------------------------------------------------------ temporizador de turno

    /** Empieza a contar los segundos para que el jugador actual actue (reinicia el conteo anterior). */
    private void programarTemporizador() {
        if (tareaTurno != null) {
            tareaTurno.cancel(false);
        }
        final int token = ++contadorTurno;
        try {
            tareaTurno = reloj.schedule(() -> tiempoAgotado(token), segundosTurno, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            // el servidor ya se detuvo
        }
    }

    private void cancelarTemporizador() {
        contadorTurno++;
        if (tareaTurno != null) {
            tareaTurno.cancel(false);
        }
    }

    /** Se acabo el tiempo: si el jugador no hizo nada desde que empezo el conteo, pierde el turno. */
    private void tiempoAgotado(int token) {
        synchronized (lock) {
            if (token != contadorTurno || !partidaIniciada || partidaTerminada) {
                return;     // el jugador actuo a tiempo, o la partida ya termino
            }
            try {
                Jugador j = juego.getJugadorActual();
                log(j.getNombre() + " no jugo en " + segundosTurno + " s: se pasa el turno");
                difundir(Protocolo.armar(Protocolo.MENSAJE, j.getNombre() + " no jugó en "
                        + segundosTurno + " segundos: se pasa su turno."));
                juego.terminarTurno();
                terminarJugada(j);      // envia el estado nuevo y vuelve a programar el reloj
            } catch (RuntimeException e) {
                log("Error en el temporizador: " + e);
            }
        }
    }

    // ------------------------------------------------------------------ mensajes de los clientes

    /** Lo llaman los hilos lectores: llega una linea de un cliente. */
    void procesar(ManejadorCliente m, String linea) {
        synchronized (lock) {
            try {
                String[] p = Protocolo.partir(linea);

                if (Protocolo.HOLA.equals(p[0])) {
                    registrar(m, Protocolo.arg(p, 1));
                    return;
                }
                if (Protocolo.ELEGIR.equals(p[0])) {
                    elegirHabilidad(m, Protocolo.arg(p, 1));
                    return;
                }
                if (!partidaIniciada || partidaTerminada) {
                    m.enviar(error("La partida no está en curso."));
                    return;
                }
                if (Protocolo.RENDIRSE.equals(p[0])) {
                    accionRendirse(m);      // se puede en cualquier momento, no solo en tu turno
                    return;
                }
                Jugador yo = m.getJugador();
                if (yo == null || juego.getJugadorActual() != yo) {
                    m.enviar(error("No es tu turno."));
                    return;
                }

                switch (p[0]) {
                    case Protocolo.JUGAR:     accionJugar(m, yo, p); break;
                    case Protocolo.ROBAR:     accionRobar(m, yo); break;
                    case Protocolo.MENOS_DOS: accionMenosDos(m, yo, p); break;
                    case Protocolo.HABILIDAD: accionHabilidad(m, yo, p); break;
                    default: m.enviar(error("Comando desconocido."));
                }
            } catch (RuntimeException e) {
                // Un mensaje raro o un mazo vacio no debe tumbar el servidor
                log("Error procesando '" + linea + "': " + e);
                m.enviar(error("No se pudo procesar esa acción."));
            }
        }
    }

    private void accionJugar(ManejadorCliente m, Jugador yo, String[] p) {
        Carta carta = buscarEn(yo.getMano(), Protocolo.arg(p, 1));
        if (carta == null) {
            m.enviar(error("No tienes esa carta."));
            return;
        }
        if (carta.getTipo() == TipoCarta.MENOS_DOS) {
            m.enviar(error("Menos Dos se juega con su propia acción."));
            return;
        }

        Color elegido = Color.ROJO;
        if (carta.cambiaColor()) {
            elegido = parseColor(Protocolo.arg(p, 2));
            if (elegido == null) {
                m.enviar(error("Debes elegir un color válido."));
                return;
            }
        }

        // Lo que se "espia" se calcula ANTES de jugar, porque al jugar cambia el turno
        String secreto = null;
        if (carta.getTipo() == TipoCarta.ESPIAR_MAZO) {
            secreto = textoEspiarMano(juego.getSiguienteJugador());
        } else if (carta.getTipo() == TipoCarta.ESPIAR_BARAJA) {
            secreto = textoEspiarBaraja();
        }

        if (!juego.jugarCarta(yo, carta, elegido)) {
            m.enviar(error("¡No puedes tirar esa carta sobre la mesa!"));
            return;
        }

        String texto = yo.getNombre() + " jugó " + carta;
        if (carta.cambiaColor()) {
            texto += " y eligió " + elegido;
        }
        if (juego.getJugadorActual() == yo && !yo.getMano().isEmpty()) {
            texto += " (el rival pierde el turno y juega de nuevo)";
        }
        difundir(Protocolo.armar(Protocolo.MENSAJE, texto));
        if (secreto != null) {
            m.enviar(Protocolo.armar(Protocolo.PRIVADO, secreto));
        }
        terminarJugada(yo);
    }

    /** Robar: solo si no tienes cartas jugables, de a una y hasta MAX_ROBOS por turno. */
    private void accionRobar(ManejadorCliente m, Jugador yo) {
        JuegoNineControlador.Robo r = juego.robarCarta(yo);
        String n = yo.getNombre();
        switch (r) {
            case TIENE_JUGABLE:
                m.enviar(error("Tienes una carta que puedes jugar: no puedes robar."));
                return;
            case JUGABLE:
                difundir(Protocolo.armar(Protocolo.MENSAJE, n + " robó una carta y puede jugarla."));
                break;
            case SIGUE:
                difundir(Protocolo.armar(Protocolo.MENSAJE, n + " robó una carta que no le sirve; puede robar otra."));
                break;
            default:
                difundir(Protocolo.armar(Protocolo.MENSAJE, n + " robó " + JuegoNineControlador.MAX_ROBOS
                        + " cartas sin poder jugar: pierde el turno."));
        }
        terminarJugada(yo);
    }

    /** Rendirse: el jugador sale de la partida (sus cartas vuelven al mazo) y sigue viendo como espectador. */
    private void accionRendirse(ManejadorCliente m) {
        Jugador yo = m.getJugador();
        if (yo == null || !juego.getJugadores().contains(yo)) {
            m.enviar(error("Ya no estás en la partida."));
            return;
        }
        rendidos.add(yo.getNombre());
        juego.rendirse(yo);
        log(yo.getNombre() + " se rindio");
        difundir(Protocolo.armar(Protocolo.MENSAJE, yo.getNombre() + " se rindió."));
        m.enviar(Protocolo.armar(Protocolo.PRIVADO, "Te rendiste. Sigues viendo la partida como espectador."));
        if (juego.hayGanadorPorAbandono()) {
            enviarEstadoATodos();
            terminarPartida(juego.getJugadores().get(0));
        } else {
            enviarEstadoATodos();
            programarTemporizador();
        }
    }

    /** Menos Dos: se juega la carta y ademas se descartan 2 cartas de la mano; luego pasa el turno. */
    private void accionMenosDos(ManejadorCliente m, Jugador yo, String[] p) {
        List<Carta> copia = new ArrayList<>(yo.getMano());
        Carta menosDos = buscarEn(copia, Protocolo.arg(p, 1));
        if (menosDos == null || menosDos.getTipo() != TipoCarta.MENOS_DOS) {
            m.enviar(error("No tienes esa carta de Menos Dos."));
            return;
        }
        if (copia.size() < 3) {
            m.enviar(error("Necesitas al menos 2 cartas adicionales en tu mano para usar Menos Dos."));
            return;
        }
        // Se valida todo sobre una copia para no dejar la jugada a medias
        copia.remove(menosDos);
        Carta d1 = buscarEn(copia, Protocolo.arg(p, 2));
        if (d1 == null) {
            m.enviar(error("La primera carta a descartar no está en tu mano."));
            return;
        }
        copia.remove(d1);
        Carta d2 = buscarEn(copia, Protocolo.arg(p, 3));
        if (d2 == null) {
            m.enviar(error("La segunda carta a descartar no está en tu mano."));
            return;
        }

        yo.removerCarta(menosDos);
        juego.descartarEnMesa(menosDos);
        yo.removerCarta(d1);
        yo.removerCarta(d2);
        difundir(Protocolo.armar(Protocolo.MENSAJE,
                yo.getNombre() + " jugó [MENOS_DOS] y descartó 2 cartas."));

        if (!yo.getMano().isEmpty()) {
            juego.terminarTurno();
        }
        terminarJugada(yo);
    }

    private void accionHabilidad(ManejadorCliente m, Jugador yo, String[] p) {
        Habilidad hab = yo.getHabilidad();
        if (hab == null || hab.getUsosRestantes() <= 0) {
            m.enviar(error("No te quedan usos de habilidad."));
            return;
        }
        TipoHabilidad pedida;
        try {
            pedida = TipoHabilidad.valueOf(Protocolo.arg(p, 1));
        } catch (IllegalArgumentException e) {
            m.enviar(error("Habilidad desconocida."));
            return;
        }
        if (pedida != hab.getTipo()) {
            m.enviar(error("Esa no es tu habilidad."));
            return;
        }

        switch (hab.getTipo()) {
            case DUPEO: {
                Carta elegida = buscarEn(yo.getMano(), Protocolo.arg(p, 2));
                if (elegida == null) {
                    m.enviar(error("Elige una carta de tu mano para duplicar."));
                    return;
                }
                hab.usar();
                yo.agregarCarta(new Carta(elegida.getColor(), elegida.getTipo(), elegida.getValor()));
                difundir(Protocolo.armar(Protocolo.MENSAJE, yo.getNombre() + " usó DUPEO."));
                m.enviar(Protocolo.armar(Protocolo.PRIVADO, "¡Has duplicado " + elegida + "!"));
                break;
            }
            case INTERCAMBIO: {
                Jugador rival = juego.getSiguienteJugador();
                Carta dar = buscarEn(yo.getMano(), Protocolo.arg(p, 2));
                if (dar == null) {
                    m.enviar(error("Elige una carta de tu mano para entregar."));
                    return;
                }
                if (rival.getMano().isEmpty()) {
                    m.enviar(error("Tu rival no tiene cartas para robar."));
                    return;
                }
                hab.usar();
                Carta robada = rival.getMano().remove(azar.nextInt(rival.getMano().size()));
                yo.removerCarta(dar);
                yo.agregarCarta(robada);
                rival.agregarCarta(dar);
                difundir(Protocolo.armar(Protocolo.MENSAJE,
                        yo.getNombre() + " usó INTERCAMBIO con " + rival.getNombre() + "."));
                m.enviar(Protocolo.armar(Protocolo.PRIVADO,
                        "Robaste " + robada + " y entregaste " + dar + "."));
                break;
            }
            case PROTECCION:
                m.enviar(Protocolo.armar(Protocolo.PRIVADO,
                        "Protección se activa sola ante el próximo +2 o +4 que te lancen."));
                return;
            case ESPEJO:
                m.enviar(Protocolo.armar(Protocolo.PRIVADO,
                        "Espejo se activa solo cuando te ataquen con un +2 o +4."));
                return;
            default:
                m.enviar(error("Esa habilidad todavía no está implementada."));
                return;
        }
        terminarJugada(yo);
    }

    // ------------------------------------------------------------------ fin de jugada / estado

    private void terminarJugada(Jugador actor) {
        enviarEstadoATodos();

        Jugador ganador = null;
        if (actor.getMano().isEmpty()) {
            ganador = actor;
        } else {
            for (Jugador j : juego.getJugadores()) {
                if (j.getMano().isEmpty()) {
                    ganador = j;
                    break;
                }
            }
        }
        if (ganador != null) {
            terminarPartida(ganador);
        } else {
            programarTemporizador();    // el jugador (o el siguiente) tiene el tiempo completo para actuar
        }
    }

    /** Fin de la partida: avisa a todos, guarda el resultado y cierra las conexiones. */
    private void terminarPartida(Jugador ganador) {
        partidaTerminada = true;
        log(ganador.getNombre() + " gano la partida");
        difundir(Protocolo.armar(Protocolo.FIN, ganador.getNombre(), ""));
        guardarResultadoEnBD(ganador);
        cancelarTemporizador();
        for (ManejadorCliente c : clientes) {
            c.cerrarSuave();
        }
    }

    /**
     * Guarda en la base de datos quien gano y quienes perdieron. Se hace en otro hilo para no
     * frenar el juego (el servidor esta dentro de synchronized(lock)) y si la base falla,
     * GestorBD solo imprime el error: la partida no se ve afectada.
     * La base de datos se usa en la computadora que hace de SERVIDOR.
     */
    private void guardarResultadoEnBD(Jugador ganador) {
        final String nombreGanador = ganador.getNombre();
        final List<String> perdedores = new ArrayList<>();
        for (Jugador j : juego.getJugadores()) {
            if (j != ganador) {
                perdedores.add(j.getNombre());
            }
        }
        for (String r : rendidos) {
            if (!perdedores.contains(r) && !r.equals(nombreGanador)) {
                perdedores.add(r);
            }
        }
        Thread hilo = new Thread(() -> {
            GestorBD.registrarVictoria(nombreGanador);
            for (String perdedor : perdedores) {
                GestorBD.registrarDerrota(perdedor);
            }
        }, "Nine-BaseDatos");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void enviarEstadoATodos() {
        for (ManejadorCliente m : clientes) {
            if (m.getJugador() != null) {
                m.enviar(estadoPara(m));
            }
        }
    }

    /** ESTADO|turnoDe|esMiTurno|colorMesa|cartaMesa|jugadores|mano|habilidad  (cada uno ve SU mano) */
    private String estadoPara(ManejadorCliente m) {
        Jugador yo = m.getJugador();
        Jugador turno = juego.getJugadorActual();

        StringBuilder jugadores = new StringBuilder();
        for (Jugador j : juego.getJugadores()) {
            if (jugadores.length() > 0) {
                jugadores.append(';');
            }
            jugadores.append(j.getNombre()).append(':').append(j.getMano().size());
        }

        StringBuilder mano = new StringBuilder();
        for (Carta c : yo.getMano()) {
            if (mano.length() > 0) {
                mano.append(';');
            }
            mano.append(Protocolo.cartaATexto(c));
        }

        Habilidad h = yo.getHabilidad();
        String hab = (h == null) ? "NINGUNA" : h.getTipo() + ":" + h.getUsosRestantes();

        return Protocolo.armar(Protocolo.ESTADO,
                turno.getNombre(),
                turno == yo ? "1" : "0",
                juego.getColorMesa().name(),
                Protocolo.cartaATexto(juego.getCartaMesa()),
                jugadores.toString(),
                mano.toString(),
                hab,
                String.valueOf(segundosTurno));
    }

    private void difundir(String mensaje) {
        for (ManejadorCliente m : clientes) {
            m.enviar(mensaje);
        }
    }

    /** Lo llama el hilo lector cuando se cae o se cierra una conexion. */
    void desconectar(ManejadorCliente m) {
        synchronized (lock) {
            if (!clientes.contains(m)) {
                return;
            }
            if (!partidaIniciada) {
                clientes.remove(m);
                if (m.getNombre() != null) {
                    log(m.getNombre() + " se desconecto antes de empezar");
                }
                avisarEspera();
            } else if (!partidaTerminada) {
                partidaTerminada = true;
                cancelarTemporizador();
                String quien = m.getNombre() == null ? "Un jugador" : m.getNombre();
                log(quien + " se desconecto: fin de la partida");
                difundir(Protocolo.armar(Protocolo.FIN, "-", quien + " se desconectó. La partida terminó."));
                for (ManejadorCliente otro : clientes) {
                    otro.cerrarSuave();
                }
            }
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static String error(String texto) {
        return Protocolo.armar(Protocolo.ERROR, texto);
    }

    private static Color parseColor(String texto) {
        try {
            Color c = Color.valueOf(texto);
            return c == Color.ESPECIAL ? null : c;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Busca en la lista una carta igual (color, tipo y valor) a la descrita en el texto. */
    private static Carta buscarEn(List<Carta> lista, String texto) {
        Carta buscada;
        try {
            buscada = Protocolo.textoACarta(texto);
        } catch (RuntimeException e) {
            return null;
        }
        for (Carta c : lista) {
            if (c.getColor() == buscada.getColor()
                    && c.getTipo() == buscada.getTipo()
                    && c.getValor() == buscada.getValor()) {
                return c;
            }
        }
        return null;
    }

    private String textoEspiarMano(Jugador rival) {
        List<Carta> mano = rival.getMano();
        if (mano.isEmpty()) {
            return rival.getNombre() + " no tiene cartas.";
        }
        String texto = "Cartas espiadas de " + rival.getNombre() + ": " + mano.get(0);
        if (mano.size() >= 2) {
            texto += " y " + mano.get(1);
        }
        return texto;
    }

    private String textoEspiarBaraja() {
        try {
            return "La siguiente carta de la baraja es: " + juego.verSiguienteCartaMazo();
        } catch (RuntimeException e) {
            return "La baraja de robo está vacía.";
        }
    }

    private void log(String texto) {
        System.out.println("[Servidor] " + texto);
    }

    /** Direcciones IPv4 de esta computadora en la red local (las que debe escribir el otro jugador). */
    public static List<String> direccionesLocales() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> direcciones = ni.getInetAddresses();
                while (direcciones.hasMoreElements()) {
                    InetAddress d = direcciones.nextElement();
                    if (d instanceof Inet4Address && !d.isLinkLocalAddress()) {
                        ips.add(d.getHostAddress());
                    }
                }
            }
        } catch (IOException e) {
            // sin interfaces de red visibles: se devuelve la lista vacia
        }
        return ips;
    }
}
