package JuegoPetNine.interfaz;

import JuegoPetNine.interfaz.GestorImagenes;
import JuegoPetNine.interfaz.BotonCarta;
import JuegoPetNine.modelo.TipoCarta;
import JuegoPetNine.modelo.Color;
import JuegoPetNine.modelo.Carta;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import JuegoPetNine.red.ClienteNine;
import JuegoPetNine.red.Protocolo;
import JuegoPetNine.modelo.TipoHabilidad;

/**
 * Ventana de una partida en red. Es la misma idea que VentanaJuego, pero NO tiene el
 * juego dentro: solo dibuja lo que el servidor le manda (mensaje ESTADO) y le envia
 * las acciones del jugador (jugar, robar, habilidad).
 */
public class VentanaCliente extends JFrame implements ClienteNine.Escucha {

    private static final java.awt.Color COLOR_TURNO_PROPIO = new java.awt.Color(39, 174, 96);
    private static final java.awt.Color COLOR_TURNO_AJENO = new java.awt.Color(52, 73, 94);

    private final ClienteNine cliente;

    // Estado recibido del servidor
    private String miNombre = "";
    private boolean enCurso = false;
    private boolean finalizada = false;
    private boolean miTurno = false;
    private boolean yaMeRendi = false;       // me rendi: sigo viendo la partida como espectador
    private String turnoNombre = "";
    private Color colorMesa = null;
    private Carta cartaMesa = null;
    private List<String> nombres = new ArrayList<>();
    private List<Integer> cantidades = new ArrayList<>();
    private List<Carta> mano = new ArrayList<>();
    private TipoHabilidad habilidad = null;
    private int usos = 0;

    // Componentes
    private JPanel panelSuperior;
    private JLabel lblTurno;
    private JLabel lblTiempo;           // cuenta regresiva del turno
    private javax.swing.Timer relojTurno;
    private int segundosRestantes = 0;
    private JPanel panelTurno;          // cartel grande con de quien es el turno
    private JLabel lblColorMesa;
    private JPanel panelIndicadorColor;
    private JButton btnHabilidad;
    private JButton btnRendirse;
    private JPanel contenedorMano;
    private JLabel lblRivales;
    private JButton btnCartaMesa;
    private JButton btnMazo;
    private JTextArea txtLog;
    private JPanel panelMano;

    /** infoRed: texto con la IP/puerto para mostrar (solo el anfitrion); puede ser null. */
    public VentanaCliente(String infoRed) {
        cliente = new ClienteNine(this);

        configurarVentana();
        crearPanelSuperior();
        crearPanelCentral();
        crearPanelInferior();

        escribirLog("Conectando...");
        if (infoRed != null) {
            escribirLog(infoRed);
        }
        refrescar();
    }

    /** Bloquea hasta conectar (llamar fuera del hilo de Swing). */
    public void conectar(String host, int puerto, String nombre) throws IOException {
        cliente.conectar(host, puerto, nombre);
    }

    // ------------------------------------------------------------------ construccion de la ventana

    private void configurarVentana() {
        setTitle("Nine! - Partida en red");
        setSize(950, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new java.awt.Color(236, 240, 241));
    }

    private void crearPanelSuperior() {
        panelSuperior = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        panelSuperior.setBackground(COLOR_TURNO_AJENO);

        lblTurno = new JLabel("Esperando jugadores...");
        lblTurno.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTurno.setForeground(java.awt.Color.WHITE);
        panelTurno = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        panelTurno.setBackground(COLOR_TURNO_AJENO);
        panelTurno.add(lblTurno);

        lblColorMesa = new JLabel("Color mesa: -");
        lblColorMesa.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblColorMesa.setForeground(java.awt.Color.WHITE);

        panelIndicadorColor = new JPanel();
        panelIndicadorColor.setPreferredSize(new Dimension(25, 25));
        panelIndicadorColor.setBorder(BorderFactory.createLineBorder(java.awt.Color.WHITE, 1));

        btnRendirse = new JButton("Rendirse");
        btnRendirse.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRendirse.setBackground(new java.awt.Color(192, 57, 43));
        btnRendirse.setForeground(java.awt.Color.WHITE);
        btnRendirse.setFocusPainted(false);
        btnRendirse.addActionListener(e -> rendirse());

        btnHabilidad = new JButton("Habilidad");
        btnHabilidad.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnHabilidad.setBackground(new java.awt.Color(155, 89, 182));
        btnHabilidad.setForeground(java.awt.Color.WHITE);
        btnHabilidad.setFocusPainted(false);
        btnHabilidad.addActionListener(e -> usarHabilidad());

        lblTiempo = new JLabel("Tiempo: -");
        lblTiempo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTiempo.setForeground(java.awt.Color.WHITE);

        // Cada segundo baja la cuenta; el servidor es quien decide cuando se pasa el turno
        relojTurno = new javax.swing.Timer(1000, e -> {
            if (enCurso && !finalizada && segundosRestantes > 0) {
                segundosRestantes--;
                mostrarTiempo();
            }
        });
        relojTurno.start();

        panelSuperior.add(lblColorMesa);
        panelSuperior.add(panelIndicadorColor);
        panelSuperior.add(lblTiempo);
        panelSuperior.add(btnHabilidad);
        panelSuperior.add(btnRendirse);

        // Arriba el cartel del turno y debajo la barra con color de la mesa y habilidad
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(panelTurno, BorderLayout.NORTH);
        norte.add(panelSuperior, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);
    }

    private void crearPanelCentral() {
        JPanel centro = new JPanel(new BorderLayout(5, 5));
        centro.setOpaque(false);

        lblRivales = new JLabel(" ", SwingConstants.CENTER);
        lblRivales.setFont(new Font("Segoe UI", Font.BOLD, 14));
        centro.add(lblRivales, BorderLayout.NORTH);

        btnCartaMesa = new JButton();
        btnCartaMesa.setPreferredSize(new Dimension(130, 180));
        btnCartaMesa.setEnabled(false);
        btnCartaMesa.setFont(new Font("Segoe UI", Font.BOLD, 14));

        btnMazo = new JButton("<html><center>MAZO<br><small>(Robar)</small></center></html>");
        btnMazo.setPreferredSize(new Dimension(130, 180));
        btnMazo.setBackground(new java.awt.Color(44, 62, 80));
        btnMazo.setForeground(java.awt.Color.WHITE);
        btnMazo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnMazo.setFocusPainted(false);
        btnMazo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMazo.addActionListener(e -> robar());
        ImageIcon reverso = GestorImagenes.getReverso();
        if (reverso != null) {
            btnMazo.setText("");
            btnMazo.setIcon(reverso);
            btnMazo.setDisabledIcon(reverso);
            btnMazo.setContentAreaFilled(false);
            btnMazo.setBorderPainted(false);
            btnMazo.setMargin(new Insets(0, 0, 0, 0));
            btnMazo.setPreferredSize(new Dimension(GestorImagenes.ANCHO_CARTA, GestorImagenes.ALTO_CARTA));
        }

        JPanel panelPozo = new JPanel(new BorderLayout());
        panelPozo.setOpaque(false);
        JLabel lblPozo = new JLabel("Mesa / Descarte", SwingConstants.CENTER);
        lblPozo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panelPozo.add(lblPozo, BorderLayout.NORTH);
        panelPozo.add(btnCartaMesa, BorderLayout.CENTER);

        JPanel panelMazo = new JPanel(new BorderLayout());
        panelMazo.setOpaque(false);
        JLabel lblMazo = new JLabel("Mazo de Robo", SwingConstants.CENTER);
        lblMazo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panelMazo.add(lblMazo, BorderLayout.NORTH);
        panelMazo.add(btnMazo, BorderLayout.CENTER);

        JPanel mesa = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 15));
        mesa.setOpaque(false);
        mesa.add(panelPozo);
        mesa.add(panelMazo);
        centro.add(mesa, BorderLayout.CENTER);

        txtLog = new JTextArea(4, 40);
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Mensajes"));
        centro.add(scrollLog, BorderLayout.SOUTH);

        add(centro, BorderLayout.CENTER);
    }

    private void crearPanelInferior() {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBorder(BorderFactory.createTitledBorder("Tu mano de cartas"));
        contenedor.setOpaque(false);
        contenedorMano = contenedor;

        panelMano = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        panelMano.setBackground(new java.awt.Color(220, 224, 230));

        JScrollPane scroll = new JScrollPane(panelMano);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(900, 210));

        contenedor.add(scroll, BorderLayout.CENTER);
        add(contenedor, BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------------ mensajes del servidor

    @Override
    public void alRecibir(String linea) {
        // Llega desde el hilo lector: se pasa al hilo de Swing
        SwingUtilities.invokeLater(() -> procesar(linea));
    }

    @Override
    public void alDesconectar(String motivo) {
        SwingUtilities.invokeLater(() -> {
            if (finalizada) {
                return;
            }
            finalizada = true;
            enCurso = false;
            escribirLog("Desconectado: " + motivo);
            refrescar();
            JOptionPane.showMessageDialog(this, "Se perdió la conexión con el servidor.\n" + motivo,
                    "Desconectado", JOptionPane.WARNING_MESSAGE);
        });
    }

    private void procesar(String linea) {
        String[] p = Protocolo.partir(linea);
        try {
            switch (p[0]) {
                case Protocolo.BIENVENIDA:
                    miNombre = Protocolo.arg(p, 1);
                    setTitle("Nine! - " + miNombre);
                    escribirLog("Conectado como " + miNombre + ".");
                    break;
                case Protocolo.OPCIONES:
                    elegirHabilidad(Protocolo.arg(p, 1), Protocolo.arg(p, 2));
                    break;
                case Protocolo.ESPERANDO:
                    escribirLog(Protocolo.arg(p, 1));
                    break;
                case Protocolo.ESTADO:
                    aplicarEstado(p);
                    break;
                case Protocolo.MENSAJE:
                    escribirLog(Protocolo.arg(p, 1));
                    break;
                case Protocolo.PRIVADO:
                    escribirLog("(solo para ti) " + Protocolo.arg(p, 1));
                    JOptionPane.showMessageDialog(this, Protocolo.arg(p, 1),
                            "Información privada", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case Protocolo.ERROR:
                    escribirLog("Aviso: " + Protocolo.arg(p, 1));
                    JOptionPane.showMessageDialog(this, Protocolo.arg(p, 1),
                            "Aviso", JOptionPane.WARNING_MESSAGE);
                    break;
                case Protocolo.FIN:
                    terminarPartida(Protocolo.arg(p, 1), Protocolo.arg(p, 2));
                    break;
                default:
                    break;
            }
        } catch (RuntimeException e) {
            escribirLog("Mensaje inválido del servidor: " + linea);
        }
    }

    /** OPCIONES|A|B: el servidor sorteo 2 habilidades; se elige una y se responde con ELEGIR. */
    private void elegirHabilidad(String a, String b) {
        TipoHabilidad ta = TipoHabilidad.valueOf(a);
        TipoHabilidad tb = TipoHabilidad.valueOf(b);
        TipoHabilidad elegida = DialogoHabilidad.elegir(this, miNombre, ta, tb);
        escribirLog("Elegiste la habilidad " + elegida + ".");
        cliente.enviar(Protocolo.armar(Protocolo.ELEGIR, elegida.name()));
    }

    /** ESTADO|turnoDe|esMiTurno|colorMesa|cartaMesa|jugadores|mano|habilidad */
    private void aplicarEstado(String[] p) {
        // Primero se lee todo en variables locales: si algo falla, no queda el estado a medias
        String nuevoTurno = Protocolo.arg(p, 1);
        boolean nuevoMiTurno = "1".equals(Protocolo.arg(p, 2));
        Color nuevoColor = Color.valueOf(Protocolo.arg(p, 3));
        Carta nuevaMesa = Protocolo.textoACarta(Protocolo.arg(p, 4));

        List<String> nuevosNombres = new ArrayList<>();
        List<Integer> nuevasCantidades = new ArrayList<>();
        for (String j : Protocolo.arg(p, 5).split(";")) {
            if (j.isEmpty()) {
                continue;
            }
            String[] datos = j.split(":");
            nuevosNombres.add(datos[0]);
            nuevasCantidades.add(Integer.parseInt(datos[1]));
        }

        List<Carta> nuevaMano = new ArrayList<>();
        for (String c : Protocolo.arg(p, 6).split(";")) {
            if (!c.isEmpty()) {
                nuevaMano.add(Protocolo.textoACarta(c));
            }
        }

        TipoHabilidad nuevaHab = null;
        int nuevosUsos = 0;
        String hab = Protocolo.arg(p, 7);
        if (!hab.isEmpty() && !"NINGUNA".equals(hab)) {
            String[] datos = hab.split(":");
            nuevaHab = TipoHabilidad.valueOf(datos[0]);
            nuevosUsos = Integer.parseInt(datos[1]);
        }

        // Campo 8 (opcional): segundos que tiene el jugador para actuar. Cada ESTADO reinicia la cuenta.
        String seg = Protocolo.arg(p, 8);
        int nuevosSegundos = seg.isEmpty() ? 0 : Integer.parseInt(seg);

        if (!nuevoTurno.equals(turnoNombre)) {
            escribirLog("¡Es el turno de " + nuevoTurno + "!");
        }
        segundosRestantes = nuevosSegundos;
        turnoNombre = nuevoTurno;
        miTurno = nuevoMiTurno;
        colorMesa = nuevoColor;
        cartaMesa = nuevaMesa;
        nombres = nuevosNombres;
        cantidades = nuevasCantidades;
        mano = nuevaMano;
        habilidad = nuevaHab;
        usos = nuevosUsos;
        enCurso = true;
        refrescar();
    }

    private void terminarPartida(String ganador, String texto) {
        finalizada = true;
        enCurso = false;
        String mensaje;
        if (!texto.isEmpty()) {
            mensaje = texto;
        } else if (ganador.equals(miNombre)) {
            mensaje = "¡FELICITACIONES! ¡Ganaste Nine!";
        } else {
            mensaje = "¡" + ganador.toUpperCase() + " HA GANADO NINE!";
        }
        escribirLog(mensaje);
        refrescar();
        JOptionPane.showMessageDialog(this, mensaje, "Fin de la partida", JOptionPane.INFORMATION_MESSAGE);
    }

    // ------------------------------------------------------------------ dibujo

    private void refrescar() {
        boolean jugable = enCurso && !finalizada;

        if (!enCurso) {
            lblTurno.setText(finalizada ? "Partida terminada" : "Esperando jugadores...");
        } else {
            lblTurno.setText("¡Es el turno de " + turnoNombre + "!" + (miTurno ? "  (eres tú)" : ""));
        }
        java.awt.Color fondoTurno = jugable && miTurno ? COLOR_TURNO_PROPIO : COLOR_TURNO_AJENO;
        panelSuperior.setBackground(fondoTurno);
        panelTurno.setBackground(fondoTurno);

        mostrarTiempo();

        lblColorMesa.setText("Color mesa: " + (colorMesa == null ? "-" : colorMesa));
        panelIndicadorColor.setBackground(colorMesa == null ? COLOR_TURNO_AJENO : colorAwt(colorMesa));

        // Rivales y cuantas cartas tienen
        StringBuilder rivales = new StringBuilder();
        for (int i = 0; i < nombres.size(); i++) {
            if (nombres.get(i).equals(miNombre)) {
                continue;
            }
            if (rivales.length() > 0) {
                rivales.append("     ");
            }
            rivales.append(nombres.get(i)).append(": ").append(cantidades.get(i)).append(" cartas");
            if (jugable && nombres.get(i).equals(turnoNombre)) {
                rivales.append(" (juega ahora)");
            }
        }
        lblRivales.setText(rivales.length() == 0 ? " " : rivales.toString());

        // Habilidad
        if (habilidad == null) {
            btnHabilidad.setText("Sin habilidad");
            btnHabilidad.setToolTipText(null);
            btnHabilidad.setEnabled(false);
        } else {
            btnHabilidad.setText("Habilidad: " + habilidad + " (" + usos + "/2)");
            btnHabilidad.setToolTipText(habilidad.getDescripcion());
            btnHabilidad.setEnabled(jugable && miTurno && usos > 0);
        }

        btnMazo.setEnabled(jugable && miTurno);

        // Carta de la mesa
        if (cartaMesa != null) {
            btnCartaMesa.setText(cartaMesa.toString());
            if (cartaMesa.getColor() == Color.ESPECIAL) {
                btnCartaMesa.setBackground(java.awt.Color.BLACK);
                btnCartaMesa.setForeground(java.awt.Color.WHITE);
            } else {
                btnCartaMesa.setBackground(colorAwt(colorMesa));
                btnCartaMesa.setForeground(colorMesa == Color.AMARILLO ? java.awt.Color.BLACK : java.awt.Color.WHITE);
            }
            aplicarImagen(btnCartaMesa, cartaMesa);
        }

        // Mano
        panelMano.removeAll();
        for (Carta c : mano) {
            BotonCarta btn = new BotonCarta(c);
            aplicarImagen(btn, c);
            btn.addActionListener(e -> clickCarta(c));
            panelMano.add(btn);
        }
        panelMano.revalidate();
        panelMano.repaint();

        // Titulo de la mano, abajo a la izquierda: cuantas cartas tienes
        contenedorMano.setBorder(BorderFactory.createTitledBorder(
                "Tu mano de cartas — " + mano.size() + (mano.size() == 1 ? " carta" : " cartas")));
        contenedorMano.repaint();
        btnRendirse.setEnabled(jugable && !yaMeRendi);
    }

    private void mostrarTiempo() {
        boolean activo = enCurso && !finalizada;
        lblTiempo.setText(activo ? "Tiempo: " + segundosRestantes + " s" : "Tiempo: -");
        lblTiempo.setForeground(activo && segundosRestantes <= 15 ? new java.awt.Color(255, 45, 45) : java.awt.Color.WHITE);
    }

    private static java.awt.Color colorAwt(Color c) {
        switch (c) {
            case ROJO: return new java.awt.Color(231, 76, 60);
            case AMARILLO: return new java.awt.Color(241, 196, 15);
            case VERDE: return new java.awt.Color(46, 204, 113);
            case AZUL: return new java.awt.Color(52, 152, 219);
            default: return java.awt.Color.BLACK;
        }
    }

    private void aplicarImagen(JButton btn, Carta carta) {
        ImageIcon icono = carta.getImagen();
        if (icono == null) {
            // Sin imagen: se queda el texto y el aspecto normal de boton
            btn.setIcon(null);
            btn.setDisabledIcon(null);
            btn.setContentAreaFilled(true);
            btn.setBorderPainted(true);
            return;
        }
        btn.setText("");
        btn.setIcon(icono);
        btn.setDisabledIcon(icono);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setPreferredSize(new Dimension(GestorImagenes.ANCHO_CARTA, GestorImagenes.ALTO_CARTA));
    }

    private void escribirLog(String texto) {
        txtLog.append(texto + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    // ------------------------------------------------------------------ acciones del jugador

    private boolean puedoActuar() {
        if (!enCurso || finalizada) {
            return false;
        }
        if (!miTurno) {
            escribirLog("Espera tu turno: juega " + turnoNombre + ".");
            return false;
        }
        return true;
    }

    private void clickCarta(Carta carta) {
        if (!puedoActuar()) {
            return;
        }
        if (carta.getTipo() == TipoCarta.MENOS_DOS) {
            jugarMenosDos(carta);
            return;
        }

        String color = "-";
        if (carta.cambiaColor()) {
            Color[] opciones = {Color.ROJO, Color.AMARILLO, Color.VERDE, Color.AZUL};
            Color elegido = (Color) JOptionPane.showInputDialog(this,
                    "Selecciona el nuevo color activo:", "Cambio de Color",
                    JOptionPane.QUESTION_MESSAGE, null, opciones, Color.ROJO);
            if (elegido == null) {
                return; // canceló: no se juega la carta
            }
            color = elegido.name();
        }
        cliente.enviar(Protocolo.armar(Protocolo.JUGAR, Protocolo.cartaATexto(carta), color));
    }

    private void jugarMenosDos(Carta menosDos) {
        if (mano.size() < 3) {
            JOptionPane.showMessageDialog(this,
                    "Necesitas al menos 2 cartas adicionales en tu mano para usar Menos Dos.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Carta> restantes = new ArrayList<>(mano);
        restantes.remove(menosDos);

        Carta d1 = elegirCarta(restantes, "Selecciona la primera carta a descartar:", "Menos Dos (1/2)");
        if (d1 == null) {
            return;
        }
        restantes.remove(d1);
        Carta d2 = elegirCarta(restantes, "Selecciona la segunda carta a descartar:", "Menos Dos (2/2)");
        if (d2 == null) {
            return;
        }
        cliente.enviar(Protocolo.armar(Protocolo.MENOS_DOS,
                Protocolo.cartaATexto(menosDos), Protocolo.cartaATexto(d1), Protocolo.cartaATexto(d2)));
    }

    private void rendirse() {
        if (!enCurso || finalizada || yaMeRendi) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "¿Seguro que quieres rendirte?",
                "Rendirse", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            yaMeRendi = true;
            cliente.enviar(Protocolo.RENDIRSE);
            refrescar();
        }
    }

    private void robar() {
        if (puedoActuar()) {
            cliente.enviar(Protocolo.ROBAR);
        }
    }

    private void usarHabilidad() {
        if (!puedoActuar() || habilidad == null) {
            return;
        }
        String carta = "-";
        if (habilidad == TipoHabilidad.DUPEO || habilidad == TipoHabilidad.INTERCAMBIO) {
            String pregunta = habilidad == TipoHabilidad.DUPEO
                    ? "Selecciona la carta que deseas duplicar:"
                    : "Selecciona la carta que le entregarás a tu rival:";
            Carta elegida = elegirCarta(mano, pregunta, "Habilidad " + habilidad);
            if (elegida == null) {
                return;
            }
            carta = Protocolo.cartaATexto(elegida);
        }
        cliente.enviar(Protocolo.armar(Protocolo.HABILIDAD, habilidad.name(), carta));
    }

    private Carta elegirCarta(List<Carta> opciones, String pregunta, String titulo) {
        if (opciones.isEmpty()) {
            return null;
        }
        Object[] lista = opciones.toArray();
        return (Carta) JOptionPane.showInputDialog(this, pregunta, titulo,
                JOptionPane.QUESTION_MESSAGE, null, lista, lista[0]);
    }
}
