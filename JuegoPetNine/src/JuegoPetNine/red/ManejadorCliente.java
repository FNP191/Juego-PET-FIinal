package JuegoPetNine.red;

import JuegoPetNine.red.ServidorNine;
import JuegoPetNine.modelo.Jugador;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import JuegoPetNine.modelo.TipoHabilidad;

/**
 * Atiende a UN jugador conectado al servidor. Usa dos hilos:
 *  - el hilo "lector" (este Runnable) espera lineas del cliente y se las pasa al servidor;
 *  - el hilo "escritor" saca mensajes de una cola y los manda por el socket.
 * Asi, un cliente lento nunca frena al servidor ni a los demas jugadores.
 */
public class ManejadorCliente implements Runnable {

    private static final String CIERRE = "\u0000CIERRE";

    private final ServidorNine servidor;
    private final Socket socket;
    private final BlockingQueue<String> salida = new LinkedBlockingQueue<>();
    private volatile boolean cerrado = false;

    // Los usa el servidor (siempre dentro de su lock)
    private String nombre;
    private TipoHabilidad habilidad = TipoHabilidad.PROTECCION;
    private Jugador jugador;

    public ManejadorCliente(ServidorNine servidor, Socket socket) {
        this.servidor = servidor;
        this.socket = socket;
    }

    @Override
    public void run() {
        Thread escritor = new Thread(this::bucleEscritura, "Nine-Escritor");
        escritor.setDaemon(true);
        escritor.start();

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = in.readLine()) != null) {
                servidor.procesar(this, linea);
            }
        } catch (IOException e) {
            // el cliente cerro o se cayo la conexion
        } finally {
            cerrarYa();
            servidor.desconectar(this);
        }
    }

    private void bucleEscritura() {
        try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            while (true) {
                String mensaje = salida.take();
                if (CIERRE.equals(mensaje)) {
                    break;
                }
                out.write(mensaje);
                out.write('\n');
                out.flush();
            }
        } catch (IOException e) {
            // no se pudo escribir: el lector se enterara y limpiara
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            cerrarYa();
        }
    }

    /** Encola un mensaje; no bloquea. */
    public void enviar(String mensaje) {
        if (!cerrado) {
            salida.offer(mensaje);
        }
    }

    /** Cierra la conexion DESPUES de enviar todo lo que esta en cola. */
    public void cerrarSuave() {
        salida.offer(CIERRE);
    }

    /** Cierra la conexion de inmediato. */
    public void cerrarYa() {
        cerrado = true;
        salida.offer(CIERRE);
        try {
            socket.close();
        } catch (IOException e) {
            // ya estaba cerrado
        }
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoHabilidad getHabilidad() { return habilidad; }
    public void setHabilidad(TipoHabilidad habilidad) { this.habilidad = habilidad; }
    public Jugador getJugador() { return jugador; }
    public void setJugador(Jugador jugador) { this.jugador = jugador; }
}
