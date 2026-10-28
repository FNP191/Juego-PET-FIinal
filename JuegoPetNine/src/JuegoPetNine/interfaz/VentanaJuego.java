/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JuegoPetNine.interfaz;
import JuegoPetNine.BD.GestorBD;
import JuegoPetNine.interfaz.GestorImagenes;
import JuegoPetNine.interfaz.BotonCarta;
import JuegoPetNine.logica.JuegoNineControlador;
import JuegoPetNine.modelo.TipoCarta;
import JuegoPetNine.modelo.Jugador;
import JuegoPetNine.modelo.Habilidad;
import JuegoPetNine.modelo.Color;
import JuegoPetNine.modelo.Carta;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import javax.swing.ImageIcon;
import java.awt.Insets;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Cursor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import JuegoPetNine.modelo.TipoHabilidad;
/**
 *
 * @author ULISES
 */


public class VentanaJuego extends JFrame {
    private JuegoNineControlador juego;
    
    // Paneles e Indicadores Superior
    private JLabel lblTurno;
    private JPanel panelTurno;          // cartel grande con de quien es el turno
    private JLabel lblTiempo;           // cuenta regresiva del turno
    private JLabel lblAviso;            // aviso cuando se pasa un turno por falta de tiempo
    private javax.swing.Timer relojTurno;
    private int segundosRestantes = JuegoNineControlador.SEGUNDOS_POR_TURNO;
    // Un color distinto por jugador: asi se nota enseguida cuando cambia el turno
    private static final java.awt.Color[] COLORES_TURNO = {
        new java.awt.Color(39, 174, 96), new java.awt.Color(230, 126, 34),
        new java.awt.Color(142, 68, 173), new java.awt.Color(41, 128, 185)
    };
    private JPanel panelIndicadorColor;
    private JLabel lblColorMesa;
    private JButton btnHabilidad;
    private JButton btnRendirse;
    private JPanel panelContenedorMano;

    // Zona Central (Mesa y Mazo)
    private JButton btnCartaMesa;
    private JButton btnMazo;

    // Zona Inferior (Mano)
    private JPanel panelMano;
    private List<BotonCarta> botonesManoActual;
    private List<Carta> cartasSeleccionadas;

    public VentanaJuego() {
        juego = new JuegoNineControlador(Arrays.asList("Jugador 1", "Jugador 2"));
        
        // Cada jugador recibe 2 habilidades al azar y elige una (antes estaban fijas)
        Random azar = new Random();
        for (Jugador j : juego.getJugadores()) {
            TipoHabilidad[] opciones = TipoHabilidad.sortearDos(azar);
            TipoHabilidad elegida = DialogoHabilidad.elegir(null, j.getNombre(), opciones[0], opciones[1]);
            j.setHabilidad(new Habilidad(elegida));
        }

        juego.iniciar();

        cartasSeleccionadas = new ArrayList<>();
        botonesManoActual = new ArrayList<>();

        configurarVentana();
        crearPanelSuperior();
        crearPanelCentral();
        crearPanelInferior();

        refrescarPantalla();

        // Cada segundo baja la cuenta regresiva del turno
        relojTurno = new javax.swing.Timer(1000, e -> pasoDelReloj());
        relojTurno.start();
    }

    /** Un segundo menos. Si se acaba el tiempo, el turno pasa al siguiente jugador. */
    private void pasoDelReloj() {
        // Si hay un cuadro abierto (elegir color, espiar...) el jugador esta actuando: el reloj se pausa
        for (java.awt.Window w : getOwnedWindows()) {
            if (w.isShowing()) {
                return;
            }
        }
        segundosRestantes--;
        if (segundosRestantes <= 0) {
            Jugador quien = juego.getJugadorActual();
            juego.terminarTurno();
            refrescarPantalla();            // reinicia el reloj para el siguiente jugador
            lblAviso.setText(quien.getNombre() + " no jugó en " + JuegoNineControlador.SEGUNDOS_POR_TURNO
                    + " segundos: se pasó su turno.");
            lblAviso.setVisible(true);
        } else {
            mostrarTiempo();
        }
    }

    private void mostrarTiempo() {
        lblTiempo.setText("Tiempo: " + segundosRestantes + " s");
        lblTiempo.setForeground(segundosRestantes <= 15 ? new java.awt.Color(255, 45, 45) : java.awt.Color.WHITE);
    }

    private void configurarVentana() {
        setTitle("Nine! - Juego de Cartas");
        setSize(950, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new java.awt.Color(236, 240, 241));
    }

    private void crearPanelSuperior() {
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        panelSuperior.setBackground(new java.awt.Color(52, 73, 94));

        lblTurno = new JLabel("¡Es el turno de ...!");
        lblTurno.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTurno.setForeground(java.awt.Color.WHITE);
        panelTurno = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        panelTurno.add(lblTurno);

        lblColorMesa = new JLabel("Color mesa: ");
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

        btnHabilidad = new JButton("Usar Habilidad");
        btnHabilidad.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnHabilidad.setBackground(new java.awt.Color(155, 89, 182));
        btnHabilidad.setForeground(java.awt.Color.WHITE);
        btnHabilidad.setFocusPainted(false);
        btnHabilidad.addActionListener(e -> ejecutarHabilidadActiva());

        lblTiempo = new JLabel("Tiempo: " + segundosRestantes + " s");
        lblTiempo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTiempo.setForeground(java.awt.Color.WHITE);

        lblAviso = new JLabel(" ", SwingConstants.CENTER);
        lblAviso.setOpaque(true);
        lblAviso.setBackground(new java.awt.Color(253, 237, 236));
        lblAviso.setForeground(new java.awt.Color(176, 38, 30));
        lblAviso.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblAviso.setVisible(false);

        panelSuperior.add(lblColorMesa);
        panelSuperior.add(panelIndicadorColor);
        panelSuperior.add(lblTiempo);
        panelSuperior.add(btnHabilidad);
        panelSuperior.add(btnRendirse);

        // Arriba el cartel del turno y debajo la barra con color de la mesa y habilidad
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(panelTurno, BorderLayout.NORTH);
        norte.add(panelSuperior, BorderLayout.CENTER);
        norte.add(lblAviso, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);
    }

    private void crearPanelCentral() {
        JPanel panelCentral = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 40));
        panelCentral.setOpaque(false);

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
        btnMazo.addActionListener(e -> accionRobar());
        ImageIcon reverso = GestorImagenes.getReverso();
        if (reverso != null) {
        btnMazo.setText("");
        btnMazo.setIcon(reverso);
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

        JPanel panelMazoContenedor = new JPanel(new BorderLayout());
        panelMazoContenedor.setOpaque(false);
        JLabel lblMazo = new JLabel("Mazo de Robo", SwingConstants.CENTER);
        lblMazo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panelMazoContenedor.add(lblMazo, BorderLayout.NORTH);
        panelMazoContenedor.add(btnMazo, BorderLayout.CENTER);

        panelCentral.add(panelPozo);
        panelCentral.add(panelMazoContenedor);

        add(panelCentral, BorderLayout.CENTER);
    }

    private void crearPanelInferior() {
        panelContenedorMano = new JPanel(new BorderLayout());
        panelContenedorMano.setBorder(BorderFactory.createTitledBorder("Tu mano de cartas"));
        panelContenedorMano.setOpaque(false);

        panelMano = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        panelMano.setBackground(new java.awt.Color(220, 224, 230));

        JScrollPane scrollMano = new JScrollPane(panelMano);
        scrollMano.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollMano.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollMano.setPreferredSize(new Dimension(900, 210));

        panelContenedorMano.add(scrollMano, BorderLayout.CENTER);
        add(panelContenedorMano, BorderLayout.SOUTH);
    }
    private void aplicarImagen(JButton btn, Carta carta) {
    ImageIcon icono = carta.getImagen();

    if (icono == null) {
        // Sin imagen: se queda el texto y el aspecto normal de botón
        btn.setIcon(null);
        btn.setDisabledIcon(null);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(true);
        return;
    }

    btn.setText("");
    btn.setIcon(icono);
    btn.setDisabledIcon(icono); // importante para btnCartaMesa (está deshabilitado)
    btn.setContentAreaFilled(false);
    btn.setBorderPainted(false);
    btn.setFocusPainted(false);
    btn.setMargin(new Insets(0, 0, 0, 0));
    btn.setPreferredSize(new Dimension(GestorImagenes.ANCHO_CARTA, GestorImagenes.ALTO_CARTA));
    
    }
    
    public void refrescarPantalla() {
        Jugador actual = juego.getJugadorActual();

        // Cada jugada o accion vuelve a dar el tiempo completo
        segundosRestantes = JuegoNineControlador.SEGUNDOS_POR_TURNO;
        mostrarTiempo();
        lblAviso.setVisible(false);

        lblTurno.setText("¡Es el turno de " + actual.getNombre() + "!");
        int indice = juego.getJugadores().indexOf(actual);
        panelTurno.setBackground(COLORES_TURNO[indice % COLORES_TURNO.length]);
        lblColorMesa.setText("Color mesa: " + juego.getColorMesa());

        switch (juego.getColorMesa()) {
            case ROJO: panelIndicadorColor.setBackground(new java.awt.Color(231, 76, 60)); break;
            case AMARILLO: panelIndicadorColor.setBackground(new java.awt.Color(241, 196, 15)); break;
            case VERDE: panelIndicadorColor.setBackground(new java.awt.Color(46, 204, 113)); break;
            case AZUL: panelIndicadorColor.setBackground(new java.awt.Color(52, 152, 219)); break;
            default: panelIndicadorColor.setBackground(java.awt.Color.BLACK); break;
        }

        Habilidad hab = actual.getHabilidad();
        if (hab != null) {
            btnHabilidad.setText("Habilidad: " + hab.getTipo() + " (" + hab.getUsosRestantes() + "/2)");
            btnHabilidad.setEnabled(hab.getUsosRestantes() > 0);
        }

        Carta cartaMesa = juego.getCartaMesa();
        btnCartaMesa.setText(cartaMesa.toString());
        if (cartaMesa.getColor() == Color.ESPECIAL) {
            btnCartaMesa.setBackground(java.awt.Color.BLACK);
            btnCartaMesa.setForeground(java.awt.Color.WHITE);
        } else {
            switch (juego.getColorMesa()) {
                case ROJO: btnCartaMesa.setBackground(new java.awt.Color(231, 76, 60)); break;
                case AMARILLO: btnCartaMesa.setBackground(new java.awt.Color(241, 196, 15)); break;
                case VERDE: btnCartaMesa.setBackground(new java.awt.Color(46, 204, 113)); break;
                case AZUL: btnCartaMesa.setBackground(new java.awt.Color(52, 152, 219)); break;
            }
            btnCartaMesa.setForeground(juego.getColorMesa() == Color.AMARILLO ? java.awt.Color.BLACK : java.awt.Color.WHITE);
        }
        aplicarImagen(btnCartaMesa, cartaMesa);
        panelMano.removeAll();
        botonesManoActual.clear();

        for (Carta c : actual.getMano()) {
            BotonCarta btn = new BotonCarta(c);
            aplicarImagen(btn, c);                    
            btn.addActionListener(e -> manejarClickCarta(btn));
            botonesManoActual.add(btn);
            panelMano.add(btn);
        }

        panelMano.revalidate();
        panelMano.repaint();

        // Titulo de la mano, abajo a la izquierda: de quien es y cuantas cartas tiene
        panelContenedorMano.setBorder(BorderFactory.createTitledBorder(
                "Mano de " + actual.getNombre() + " — " + actual.getMano().size()
                + (actual.getMano().size() == 1 ? " carta" : " cartas")));
        panelContenedorMano.repaint();
    }

    private void manejarClickCarta(BotonCarta btnClicked) {
        Carta carta = btnClicked.getCarta();
        Jugador actual = juego.getJugadorActual();

        if (carta.getTipo() == TipoCarta.MENOS_DOS) {
            ejecutarEfectoMenosDos(carta);
            return;
        }

        if (carta.getTipo() == TipoCarta.ESPIAR_MAZO) {
            Jugador rival = juego.getSiguienteJugador();
            if (rival.getMano().size() >= 2) {
                Carta c1 = rival.getMano().get(0);
                Carta c2 = rival.getMano().get(1);
                JOptionPane.showMessageDialog(this, 
                    "Cartas espiadas de " + rival.getNombre() + ":\n1. " + c1 + "\n2. " + c2, 
                    "Espiar Mazo Enemigo", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else if (!rival.getMano().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Carta espiada de " + rival.getNombre() + ":\n1. " + rival.getMano().get(0), 
                    "Espiar Mazo Enemigo", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }

        if (carta.getTipo() == TipoCarta.ESPIAR_BARAJA) {
            Carta siguiente = juego.verSiguienteCartaMazo();
            JOptionPane.showMessageDialog(this, 
                "La siguiente carta en la baraja de robo es:\n" + siguiente, 
                "Espiar Baraja", 
                JOptionPane.INFORMATION_MESSAGE);
        }

        Color colorElegido = Color.ROJO;
        if (carta.cambiaColor()) {
            Color[] opciones = {Color.ROJO, Color.AMARILLO, Color.VERDE, Color.AZUL};
            Color seleccion = (Color) JOptionPane.showInputDialog(
                this, "Selecciona el nuevo color activo:", "Cambio de Color",
                JOptionPane.QUESTION_MESSAGE, null, opciones, Color.ROJO
            );
            if (seleccion == null) {
                return;                 // canceló: no se juega la carta
            }
            colorElegido = seleccion;
        }

        boolean jugadaExitosa = juego.jugarCarta(actual, carta, colorElegido);

        if (!jugadaExitosa) {
            JOptionPane.showMessageDialog(this, "¡No puedes tirar esa carta sobre la mesa!", "Jugada Inválida", JOptionPane.ERROR_MESSAGE);
            return;
        }

        verificarVictoria(actual);
        refrescarPantalla();
    }

    private void ejecutarEfectoMenosDos(Carta cartaMenosDos) {
        Jugador actual = juego.getJugadorActual();
        if (actual.getMano().size() < 3) {
            JOptionPane.showMessageDialog(this, "Necesitas al menos 2 cartas adicionales en tu mano para usar Menos Dos.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Elige 2 cartas de tu mano para descartarlas.", "Efecto Menos Dos", JOptionPane.INFORMATION_MESSAGE);

        // Se elige todo ANTES de tocar la mano: si el jugador cancela, la jugada no ocurre
        java.util.List<Carta> restantes = new java.util.ArrayList<>(actual.getMano());
        restantes.remove(cartaMenosDos);

        Object[] opcionesMano = restantes.toArray();
        Carta c1 = (Carta) JOptionPane.showInputDialog(this, "Selecciona la primera carta a descartar:", "Menos Dos (1/2)", JOptionPane.QUESTION_MESSAGE, null, opcionesMano, opcionesMano[0]);
        if (c1 == null) {
            return;
        }
        restantes.remove(c1);

        opcionesMano = restantes.toArray();
        Carta c2 = (Carta) JOptionPane.showInputDialog(this, "Selecciona la segunda carta a descartar:", "Menos Dos (2/2)", JOptionPane.QUESTION_MESSAGE, null, opcionesMano, opcionesMano[0]);
        if (c2 == null) {
            return;
        }

        actual.removerCarta(cartaMenosDos);
        juego.descartarEnMesa(cartaMenosDos);
        actual.removerCarta(c1);
        actual.removerCarta(c2);

        verificarVictoria(actual);
        juego.terminarTurno();
        refrescarPantalla();
    }

    private void ejecutarHabilidadActiva() {
        Jugador actual = juego.getJugadorActual();
        Habilidad hab = actual.getHabilidad();

        if (hab == null || hab.getUsosRestantes() <= 0) {
            JOptionPane.showMessageDialog(this, "No te quedan usos de habilidad.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        switch (hab.getTipo()) {
            case DUPEO:
                Object[] manoArr = actual.getMano().toArray();
                Carta elegida = (Carta) JOptionPane.showInputDialog(this, "Selecciona la carta que deseas duplicar:", "Habilidad Dupeo", JOptionPane.QUESTION_MESSAGE, null, manoArr, manoArr[0]);
                if (elegida != null && hab.usar()) {
                    actual.agregarCarta(new Carta(elegida.getColor(), elegida.getTipo(), elegida.getValor()));
                    JOptionPane.showMessageDialog(this, "¡Has duplicado " + elegida + "!");
                }
                break;

            case INTERCAMBIO:
                Jugador rival = juego.getSiguienteJugador();
                if (rival.getMano().isEmpty() || actual.getMano().isEmpty()) {
                    break;
                }
                Object[] misCartas = actual.getMano().toArray();
                Carta entregada = (Carta) JOptionPane.showInputDialog(this, "Selecciona la carta que le entregarás a " + rival.getNombre() + ":", "Habilidad Intercambio", JOptionPane.QUESTION_MESSAGE, null, misCartas, misCartas[0]);
                if (entregada != null && hab.usar()) {
                    Carta robada = rival.getMano().remove((int) (Math.random() * rival.getMano().size()));
                    actual.removerCarta(entregada);
                    actual.agregarCarta(robada);
                    rival.agregarCarta(entregada);
                    JOptionPane.showMessageDialog(this, "Robaste " + robada + " y entregaste " + entregada + ".");
                }
                break;

            case PROTECCION:
                JOptionPane.showMessageDialog(this, "Protección se activa sola ante el próximo +2 o +4 que te lancen.");
                break;

            case ESPEJO:
                JOptionPane.showMessageDialog(this, "Espejo se activa automáticamente de forma pasiva cuando te ataquen con un +2 o +4.");
                break;
        }

        refrescarPantalla();
    }

    /** Robar: solo si no tienes cartas jugables, de a una y hasta 3 por turno. */
    private void accionRobar() {
        Jugador actual = juego.getJugadorActual();
        JuegoNineControlador.Robo r = juego.robarCarta(actual);
        refrescarPantalla();
        String aviso;
        switch (r) {
            case TIENE_JUGABLE:
                aviso = "Tienes una carta que puedes jugar: no puedes robar.";
                break;
            case JUGABLE:
                aviso = actual.getNombre() + " robó una carta que puede jugar.";
                break;
            case SIGUE:
                aviso = "Robaste una carta que no te sirve. Puedes robar otra (" + juego.getRobosEnTurno()
                        + "/" + JuegoNineControlador.MAX_ROBOS + ").";
                break;
            default:
                aviso = actual.getNombre() + " robó " + JuegoNineControlador.MAX_ROBOS
                        + " cartas sin poder jugar: pierde el turno.";
        }
        lblAviso.setText(aviso);
        lblAviso.setVisible(true);
    }

    /** Rendirse: el jugador actual abandona y gana el otro (la partida local es de 2). */
    private void rendirse() {
        Jugador quien = juego.getJugadorActual();
        int ok = JOptionPane.showConfirmDialog(this, quien.getNombre() + ", ¿seguro que quieres rendirte?",
                "Rendirse", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        final String perdedor = quien.getNombre();
        juego.rendirse(quien);
        if (juego.hayGanadorPorAbandono()) {
            Jugador ganador = juego.getJugadores().get(0);
            final String nombreGanador = ganador.getNombre();
            Thread bd = new Thread(() -> {
                GestorBD.registrarVictoria(nombreGanador);
                GestorBD.registrarDerrota(perdedor);
            }, "Nine-BaseDatos");
            bd.setDaemon(true);
            bd.start();
            JOptionPane.showMessageDialog(this, perdedor + " se rindió.\n¡" + nombreGanador.toUpperCase() + " HA GANADO NINE!",
                    "Fin de Partida", JOptionPane.INFORMATION_MESSAGE);
            try {
                bd.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.exit(0);
        }
        refrescarPantalla();
    }

    /**
     * Gana quien se quede sin cartas. Se revisa a TODOS los jugadores, no solo al que jugó: con
     * Cambio de Mano, si el jugador tiraba su ultima carta, el rival recibe la mano vacia y gana el rival.
     */
    private void verificarVictoria(Jugador jugo) {
        Jugador j = null;
        if (jugo.getMano().isEmpty()) {
            j = jugo;
        } else {
            for (Jugador otro : juego.getJugadores()) {
                if (otro.getMano().isEmpty()) {
                    j = otro;
                    break;
                }
            }
        }
        if (j != null) {

            final String ganador = j.getNombre();
            final java.util.List<String> perdedores = new java.util.ArrayList<>();
            for (Jugador otro : juego.getJugadores()) {
                if (otro != j) {
                    perdedores.add(otro.getNombre());
                }
            }
            // La base de datos en otro hilo: si esta apagada no congela la ventana
            Thread bd = new Thread(() -> {
                GestorBD.registrarVictoria(ganador);
                for (String p : perdedores) {
                    GestorBD.registrarDerrota(p);
                }
            }, "Nine-BaseDatos");
            bd.setDaemon(true);
            bd.start();

            JOptionPane.showMessageDialog(this,
                "¡FELICITACIONES! " + j.getNombre().toUpperCase() + " HA GANADO NINE!",
                "Fin de Partida", JOptionPane.INFORMATION_MESSAGE);

            try {
                bd.join(3000);          // dar tiempo a que se guarde antes de cerrar
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.exit(0);
        }
    }

}

        
    

