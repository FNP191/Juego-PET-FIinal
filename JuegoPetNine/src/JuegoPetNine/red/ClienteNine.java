package JuegoPetNine.red;

import JuegoPetNine.red.Protocolo;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import JuegoPetNine.modelo.TipoHabilidad;

/**
 * Conexion del jugador con el servidor.
 * Tiene su propio hilo lector: asi la ventana nunca se congela esperando datos.
 * OJO: los avisos llegan en ese hilo, no en el hilo de Swing; quien escuche debe
 * usar SwingUtilities.invokeLater antes de tocar componentes.
 */
public class ClienteNine {

    public interface Escucha {
        void alRecibir(String linea);
        void alDesconectar(String motivo);
    }

    private final Escucha escucha;
    private Socket socket;
    private BufferedWriter salida;
    private volatile boolean activo = false;

    public ClienteNine(Escucha escucha) {
        this.escucha = escucha;
    }

    /** Se conecta (puede tardar hasta 5 s) y se presenta al servidor. */
    public void conectar(String host, int puerto, String nombre, TipoHabilidad habilidad) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, puerto), 5000);
        s.setTcpNoDelay(true);
        socket = s;
        salida = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader entrada = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        activo = true;

        enviar(Protocolo.armar(Protocolo.HOLA, nombre, habilidad.name()));

        Thread lector = new Thread(() -> leer(entrada), "Nine-Cliente-Lector");
        lector.setDaemon(true);
        lector.start();
    }

    private void leer(BufferedReader entrada) {
        String motivo = "El servidor cerró la conexión.";
        try {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                escucha.alRecibir(linea);
            }
        } catch (IOException e) {
            motivo = "Se perdió la conexión: " + e.getMessage();
        } finally {
            if (activo) {
                activo = false;
                escucha.alDesconectar(motivo);
            }
        }
    }

    public synchronized void enviar(String linea) {
        if (salida == null) {
            return;
        }
        try {
            salida.write(linea);
            salida.write('\n');
            salida.flush();
        } catch (IOException e) {
            // el hilo lector detectara la caida y avisara
        }
    }

    public void cerrar() {
        activo = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // ya estaba cerrado
        }
    }
}
