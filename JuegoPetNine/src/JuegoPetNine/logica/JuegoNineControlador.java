/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JuegoPetNine.logica;

import JuegoPetNine.modelo.Mazo;
import JuegoPetNine.modelo.Jugador;
import JuegoPetNine.modelo.Habilidad;
import JuegoPetNine.modelo.Color;
import JuegoPetNine.modelo.Carta;
import java.util.ArrayList;
import java.util.List;
import JuegoPetNine.modelo.TipoCarta;
import JuegoPetNine.modelo.TipoHabilidad;

/**
 *
 * @author ULISES
 */
public class JuegoNineControlador {
    /** Tiempo que tiene un jugador para actuar; si no juega en ese tiempo, pasa el turno. */
    public static final int SEGUNDOS_POR_TURNO = 30;
    /** Maximo de cartas que se pueden robar en un mismo turno. */
    public static final int MAX_ROBOS = 3;

    /** Resultado de robar: lo usan la ventana local y el servidor para avisar al jugador. */
    public enum Robo {
        TIENE_JUGABLE,   // no se roba: ya tienes una carta que puedes jugar
        JUGABLE,         // robo una carta que puede jugar
        SIGUE,           // robo y no le sirve, pero todavia puede robar otra
        PIERDE_TURNO     // llego al limite sin poder jugar: el turno pasa
    }

    private List<Jugador> jugadores;
    private Mazo mazo;
    private int turnoActual = 0;
    private int sentido = 1; 
    private Color colorMesaActual;
    private int robosEnTurno = 0;       // cartas robadas en el turno actual
    private int saltosPendientes = 0;   // jugadores que perderan el turno cuando termine el turno actual

    public JuegoNineControlador(List<String> nombres) {
        jugadores = new ArrayList<>();
        for (String nombre : nombres) {
            jugadores.add(new Jugador(nombre));
        }
        mazo = new Mazo();
    }

    public void iniciar() {
        // Repartir 9 cartas a cada jugador
        for (Jugador j : jugadores) {
            for (int i = 0; i < 9; i++) {
                j.agregarCarta(mazo.robar());
            }
        }
        Carta inicial = mazo.robar();
        while(inicial.getColor() == Color.ESPECIAL){
            mazo.descartar(inicial);
            inicial = mazo.robar();
        }
        mazo.descartar(inicial);
        colorMesaActual = inicial.getColor();
    }

    /**
     * Juega una carta. Como en el UNO original, UNA carta por turno: despues de jugarla el turno termina.
     *  - Bloqueo y Reversa (2 jugadores): el rival pierde el turno y vuelve a jugar quien la tiro.
     *  - +2 y +4: el rival roba pero SI juega.
     */
    public boolean jugarCarta(Jugador jugador, Carta carta, Color nuevoColor) {
        if (!carta.esJugableSobre(mazo.getCartaMesa(), colorMesaActual)) {
            return false;
        }
        jugador.removerCarta(carta);
        mazo.descartar(carta);

        if (carta.cambiaColor()) {
            colorMesaActual = nuevoColor;            // Comodin y +4: el jugador elige
        } else if (carta.getColor() != Color.ESPECIAL) {
            colorMesaActual = carta.getColor();      // carta normal: el color de la carta
        }                                            // Espiar, Cambio de Mano...: el color de la mesa no cambia

        saltosPendientes += ejecutarEfecto(carta, jugador);
        terminarTurno();
        return true;
    }

    /** Efecto de la carta. Devuelve cuantos jugadores perderan el turno por ella (+2 y +4: el rival roba pero SI juega). */
    private int ejecutarEfecto(Carta carta, Jugador actual) {
        switch (carta.getTipo()) {
            case SALTO: return 1;
            case REVERSA: sentido *= -1; return jugadores.size() == 2 ? 1 : 0;
            case SUMA_DOS: atacar(getSiguienteJugador(), 2, actual); return 0;
            case SUMA_CUATRO: atacar(getSiguienteJugador(), 4, actual); return 0;
            case ESPIAR_MAZO: System.out.println("--> Espiaste el mazo: La próxima carta es " + mazo.verSiguiente()); return 0;
            case CAMBIO_MANO: 
                Jugador rival = getSiguienteJugador();
                List<Carta> temp = new ArrayList<>(actual.getMano());
                actual.getMano().clear(); actual.getMano().addAll(rival.getMano());
                rival.getMano().clear(); rival.getMano().addAll(temp);
                System.out.println("¡Cambiaste tu mano con " + rival.getNombre() + "!");
                return 0;
            default: return 0;
        }
    }

    /** Pasa el turno al siguiente jugador, saltando a los que hayan perdido el turno (Bloqueo y Reversa; solo con 3 o mas jugadores). */
    public void terminarTurno() {
        robosEnTurno = 0;
        // Bloqueo/Reversa: con 2 jugadores el rival pierde el turno y vuelve a jugar quien la tiro.
        // Con 3 o mas, los saltos nunca pueden devolverle el turno a quien acaba de jugar.
        saltosPendientes = Math.min(saltosPendientes, jugadores.size() == 2 ? 1 : jugadores.size() - 2);
        avanzarTurno();
        while (saltosPendientes > 0) {
            avanzarTurno();
            saltosPendientes--;
        }
    }

    private void atacar(Jugador objetivo, int cantidad, Jugador atacante) {
        Habilidad h = objetivo.getHabilidad();
        if (h != null && h.getTipo() == TipoHabilidad.ESPEJO && h.getUsosRestantes() > 0) {
            System.out.println(objetivo.getNombre() + " usó ESPEJO! El ataque vuelve a " + atacante.getNombre());
            for (int i = 0; i < cantidad; i++) atacante.agregarCarta(mazo.robar());
            h.usar();
        } else if (h != null && h.getTipo() == TipoHabilidad.PROTECCION && h.getUsosRestantes() > 0) {
            System.out.println(objetivo.getNombre() + " se PROTEGIÓ del ataque.");
            h.usar();
        } else {
            System.out.println(objetivo.getNombre() + " recibe " + cantidad + " cartas.");
            for (int i = 0; i < cantidad; i++) objetivo.agregarCarta(mazo.robar());
        }
    }

    /** True si el jugador tiene al menos una carta que puede tirar ahora (Menos Dos necesita 3 cartas o mas). */
    public boolean puedeJugarAlgo(Jugador j) {
        for (Carta c : j.getMano()) {
            if (c.getTipo() == TipoCarta.MENOS_DOS) {
                if (j.getMano().size() >= 3) {
                    return true;
                }
            } else if (c.esJugableSobre(mazo.getCartaMesa(), colorMesaActual)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Robar del mazo. Solo se puede si NO tienes ninguna carta jugable. Se roba de a una: si la carta
     * sirve, el jugador puede tirarla; si no, puede robar otra, hasta MAX_ROBOS por turno. Si llega al
     * limite sin poder jugar, pierde el turno.
     */
    public Robo robarCarta(Jugador j) {
        if (puedeJugarAlgo(j)) {
            return Robo.TIENE_JUGABLE;
        }
        j.agregarCarta(mazo.robar());
        robosEnTurno++;
        if (puedeJugarAlgo(j)) {
            return Robo.JUGABLE;
        }
        if (robosEnTurno >= MAX_ROBOS) {
            terminarTurno();
            return Robo.PIERDE_TURNO;
        }
        return Robo.SIGUE;
    }

    public int getRobosEnTurno() { return robosEnTurno; }

    /**
     * El jugador se rinde: sale de la partida y sus cartas vuelven al mazo. Si era su turno, pasa al
     * siguiente. Con un solo jugador restante la partida termina (ver hayGanadorPorAbandono).
     */
    public void rendirse(Jugador j) {
        int idx = jugadores.indexOf(j);
        if (idx < 0) {
            return;
        }
        boolean eraSuTurno = (idx == turnoActual);
        mazo.devolver(new ArrayList<>(j.getMano()));
        j.getMano().clear();
        jugadores.remove(idx);
        saltosPendientes = 0;
        robosEnTurno = 0;
        if (jugadores.isEmpty()) {
            return;
        }
        if (eraSuTurno) {
            turnoActual = sentido == 1 ? idx % jugadores.size()
                                       : (idx - 1 + jugadores.size()) % jugadores.size();
        } else if (idx < turnoActual) {
            turnoActual--;
        }
    }

    /** True si solo queda un jugador en la partida (los demas se rindieron). */
    public boolean hayGanadorPorAbandono() { return jugadores.size() == 1; }
    public void avanzarTurno() { turnoActual = (turnoActual + sentido + jugadores.size()) % jugadores.size(); }
    
    public Jugador getJugadorActual() { return jugadores.get(turnoActual); }
    public Jugador getSiguienteJugador() { return jugadores.get((turnoActual + sentido + jugadores.size()) % jugadores.size()); }
    public Carta getCartaMesa() { return mazo.getCartaMesa(); }
    public Color getColorMesa() { return colorMesaActual; }
    public List<Jugador> getJugadores() { return jugadores; }

    public Carta verSiguienteCartaMazo() {
        return mazo.verSiguiente();
    }

    public void descartarEnMesa(Carta carta) {
        mazo.descartar(carta);
    }
}
