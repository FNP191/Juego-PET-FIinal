package JuegoPetNine.interfaz;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import JuegoPetNine.red.Protocolo;
import JuegoPetNine.red.ServidorNine;
import JuegoPetNine.modelo.TipoHabilidad;

/** Pantalla de inicio: jugar en esta computadora, crear una partida en red o unirse a una. */
public class MenuPrincipal extends JFrame {

    /** Habilidades que el juego realmente implementa (DESCARTE_DOBLE todavia no hace nada). */
    private static final TipoHabilidad[] HABILIDADES = {
        TipoHabilidad.PROTECCION, TipoHabilidad.ESPEJO, TipoHabilidad.DUPEO, TipoHabilidad.INTERCAMBIO
    };

    public MenuPrincipal() {
        setTitle("Nine!");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(360, 340);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(0, 1, 12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 25, 40));

        JLabel titulo = new JLabel("NINE!", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 42));
        panel.add(titulo);

        JButton btnLocal = new JButton("Jugar en esta computadora");
        btnLocal.addActionListener(e -> jugarLocal());
        panel.add(btnLocal);

        JButton btnCrear = new JButton("Crear partida en red (servidor)");
        btnCrear.addActionListener(e -> crearPartida());
        panel.add(btnCrear);

        JButton btnUnirse = new JButton("Unirse a una partida en red");
        btnUnirse.addActionListener(e -> unirse());
        panel.add(btnUnirse);

        add(panel);
    }

    private void jugarLocal() {
        new VentanaJuego().setVisible(true);
        dispose();
    }

    // ------------------------------------------------------------------ servidor

    private void crearPartida() {
        JTextField txtNombre = new JTextField("Jugador 1");
        JComboBox<TipoHabilidad> cmbHabilidad = crearComboHabilidades();
        JSpinner spnJugadores = new JSpinner(new SpinnerNumberModel(2, 2, 4, 1));
        JTextField txtPuerto = new JTextField(String.valueOf(Protocolo.PUERTO_DEFECTO));

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Tu nombre:"));
        panel.add(txtNombre);
        panel.add(new JLabel("Tu habilidad:"));
        panel.add(cmbHabilidad);
        panel.add(new JLabel("Cantidad de jugadores:"));
        panel.add(spnJugadores);
        panel.add(new JLabel("Puerto:"));
        panel.add(txtPuerto);

        int respuesta = JOptionPane.showConfirmDialog(this, panel, "Crear partida",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (respuesta != JOptionPane.OK_OPTION) {
            return;
        }
        int puerto = leerPuerto(txtPuerto.getText());
        if (puerto < 0) {
            return;
        }

        ServidorNine servidor = new ServidorNine(puerto, (Integer) spnJugadores.getValue());
        try {
            servidor.iniciar();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo abrir el puerto " + puerto + ".\n¿Ya hay otro servidor abierto?\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> ips = ServidorNine.direccionesLocales();
        String info = "Eres el servidor. Los otros jugadores deben conectarse a "
                + (ips.isEmpty() ? "la IP de esta computadora" : String.join(" o ", ips))
                + " (puerto " + puerto + ").";

        // El anfitrion tambien juega: se conecta a su propio servidor
        conectarYMostrar("127.0.0.1", puerto, txtNombre.getText(),
                (TipoHabilidad) cmbHabilidad.getSelectedItem(), info);
    }

    // ------------------------------------------------------------------ cliente

    private void unirse() {
        JTextField txtNombre = new JTextField("Jugador 2");
        JTextField txtIp = new JTextField("192.168.");
        JTextField txtPuerto = new JTextField(String.valueOf(Protocolo.PUERTO_DEFECTO));
        JComboBox<TipoHabilidad> cmbHabilidad = crearComboHabilidades();

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Tu nombre:"));
        panel.add(txtNombre);
        panel.add(new JLabel("IP del servidor:"));
        panel.add(txtIp);
        panel.add(new JLabel("Puerto:"));
        panel.add(txtPuerto);
        panel.add(new JLabel("Tu habilidad:"));
        panel.add(cmbHabilidad);

        int respuesta = JOptionPane.showConfirmDialog(this, panel, "Unirse a una partida",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (respuesta != JOptionPane.OK_OPTION) {
            return;
        }
        int puerto = leerPuerto(txtPuerto.getText());
        if (puerto < 0) {
            return;
        }
        String ip = txtIp.getText().trim();
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe la IP del servidor.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        conectarYMostrar(ip, puerto, txtNombre.getText(), (TipoHabilidad) cmbHabilidad.getSelectedItem(), null);
    }

    /** Conecta en un hilo aparte (para no congelar la pantalla) y abre la ventana de juego. */
    private void conectarYMostrar(String host, int puerto, String nombre, TipoHabilidad habilidad, String info) {
        VentanaCliente ventana = new VentanaCliente(info);
        Thread hilo = new Thread(() -> {
            try {
                ventana.conectar(host, puerto, nombre, habilidad);
                SwingUtilities.invokeLater(() -> {
                    ventana.setVisible(true);
                    dispose();
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    ventana.dispose();
                    JOptionPane.showMessageDialog(this,
                            "No se pudo conectar a " + host + ":" + puerto + "\n" + ex.getMessage()
                            + "\n\nRevisa la IP y que el firewall permita Java.",
                            "Error de conexión", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "Nine-Conexion");
        hilo.setDaemon(true);
        hilo.start();
    }

    // ------------------------------------------------------------------ utilidades

    private JComboBox<TipoHabilidad> crearComboHabilidades() {
        JComboBox<TipoHabilidad> combo = new JComboBox<>(HABILIDADES);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean seleccionado, boolean foco) {
                Component c = super.getListCellRendererComponent(list, value, index, seleccionado, foco);
                if (value instanceof TipoHabilidad) {
                    setText(((TipoHabilidad) value).name());
                    setToolTipText(((TipoHabilidad) value).getDescripcion());
                }
                return c;
            }
        });
        combo.setToolTipText("Pasa el mouse sobre cada opción para ver qué hace");
        return combo;
    }

    /** Devuelve el puerto, o -1 (tras avisar) si no es valido. */
    private int leerPuerto(String texto) {
        try {
            int p = Integer.parseInt(texto.trim());
            if (p >= 1024 && p <= 65535) {
                return p;
            }
        } catch (NumberFormatException e) {
            // cae al mensaje de abajo
        }
        JOptionPane.showMessageDialog(this, "El puerto debe ser un número entre 1024 y 65535.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
        return -1;
    }
}
