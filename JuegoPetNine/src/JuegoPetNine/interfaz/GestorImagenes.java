/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JuegoPetNine.interfaz;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/**
 *
 * @author ULISES
 */
public class GestorImagenes {
    private static final Map<String, ImageIcon> cache = new HashMap<>();
    private static final Set<String> avisados = new HashSet<>();

    public static final int ANCHO_CARTA = 100;
    public static final int ALTO_CARTA = 150;

    // Ruta dentro del classpath: paquete juegopetnine.imagenes
    private static final String RUTA = "/juegopetnine/imagenes/";

    public static ImageIcon getImagen(String nombreArchivo) {
        if (cache.containsKey(nombreArchivo)) {
            return cache.get(nombreArchivo);
        }

        URL url = GestorImagenes.class.getResource(RUTA + nombreArchivo);
        if (url == null) {
            // Avisa una sola vez por archivo para no llenar la consola
            if (avisados.add(nombreArchivo)) {
                System.err.println("No se encontró la imagen: " + RUTA + nombreArchivo);
            }
            return null;
        }

        ImageIcon original = new ImageIcon(url);
        Image escalada = original.getImage().getScaledInstance(
                ANCHO_CARTA, ALTO_CARTA, Image.SCALE_SMOOTH);

        ImageIcon icono = new ImageIcon(escalada);
        cache.put(nombreArchivo, icono);
        return icono;
    }

    public static ImageIcon getReverso() {
        return getImagen("reverso.png");
    }

    // Traduce número a palabra: 5 -> "Cinco"
    public static String numeroAPalabra(int n) {
        String[] palabras = {"Cero", "Uno", "Dos", "Tres", "Cuatro",
                             "Cinco", "Seis", "Siete", "Ocho", "Nueve"};
        return palabras[n];
    }

    // Construye el nombre: ("Cinco", "Rojo") -> "Cinco-Rojo.png"
    public static String nombreArchivo(String valor, String color) {
        if (color == null || color.isEmpty()) {
            return valor + ".png";          // Mas4.png, CambioColor.png
        }
        return valor + "-" + color + ".png";
    }

    // Imprime en consola las imágenes que faltan (útil mientras armas el set)
    public static void verificarTodas() {
        String[] colores = {"Rojo", "Amarillo", "Verde", "Azul"};
        String[] valores = {"Cero", "Uno", "Dos", "Tres", "Cuatro", "Cinco", "Seis",
                            "Siete", "Ocho", "Nueve", "Mas2", "Reversa", "Bloqueo"};
        String[] especiales = {"CambioColor", "Mas4", "Menos2", "EspiarMazo",
                               "CambioMano", "EspiarBaraja", "reverso"};
        for (String v : valores) {
            for (String c : colores) {
                comprobar(v + "-" + c + ".png");
            }
        }
        for (String e : especiales) {
            comprobar(e + ".png");
        }
        System.out.println("Verificación de imágenes terminada.");
    }

    private static void comprobar(String nombre) {
        if (GestorImagenes.class.getResource(RUTA + nombre) == null) {
            System.out.println("FALTA: " + nombre);
        }
    }
}
