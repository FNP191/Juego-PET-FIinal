package JuegoPetNine.red;

import JuegoPetNine.modelo.TipoCarta;
import JuegoPetNine.modelo.Color;
import JuegoPetNine.modelo.Carta;

/**
 * Lenguaje que hablan el servidor y los clientes.
 *
 * Cada mensaje es UNA linea de texto con campos separados por "|".
 * Las cartas viajan como "COLOR,TIPO,VALOR" (ej: "ROJO,NUMERO,5").
 *
 * Cliente -> Servidor
 *   HOLA|nombre
 *   ELEGIR|HABILIDAD           (una de las 2 que ofrecio el servidor en OPCIONES)
 *   JUGAR|carta|color          (color solo importa para Comodin y +4; si no, "-")
 *   ROBAR
 *   MENOSDOS|carta|descarte1|descarte2
 *   HABILIDAD|tipo|carta       (carta solo para DUPEO e INTERCAMBIO; si no, "-")
 *
 * Servidor -> Cliente
 *   BIENVENIDA|nombre
 *   OPCIONES|habilidadA|habilidadB   (2 al azar; el jugador debe responder con ELEGIR)
 *   ESPERANDO|texto
 *   ESTADO|turnoDe|esMiTurno|colorMesa|cartaMesa|jugadores|mano|habilidad|segundosPorTurno
 *   MENSAJE|texto              (para todos)
 *   PRIVADO|texto              (solo para un jugador)
 *   ERROR|texto
 *   FIN|ganador|texto
 */
public final class Protocolo {

    private Protocolo() { }

    public static final int PUERTO_DEFECTO = 5555;

    // Cliente -> Servidor
    public static final String HOLA = "HOLA";
    public static final String ELEGIR = "ELEGIR";
    public static final String JUGAR = "JUGAR";
    public static final String ROBAR = "ROBAR";
    public static final String MENOS_DOS = "MENOSDOS";
    public static final String HABILIDAD = "HABILIDAD";

    // Servidor -> Cliente
    public static final String BIENVENIDA = "BIENVENIDA";
    public static final String OPCIONES = "OPCIONES";
    public static final String ESPERANDO = "ESPERANDO";
    public static final String ESTADO = "ESTADO";
    public static final String MENSAJE = "MENSAJE";
    public static final String PRIVADO = "PRIVADO";
    public static final String ERROR = "ERROR";
    public static final String FIN = "FIN";

    /** Une los campos con "|". */
    public static String armar(String... partes) {
        return String.join("|", partes);
    }

    /** Separa una linea en campos (conserva los campos vacios). */
    public static String[] partir(String linea) {
        return linea.split("\\|", -1);
    }

    /** Devuelve el campo i, o "" si el mensaje no lo trae. */
    public static String arg(String[] partes, int i) {
        return i < partes.length ? partes[i] : "";
    }

    public static String cartaATexto(Carta c) {
        return c.getColor() + "," + c.getTipo() + "," + c.getValor();
    }

    /** Lanza una RuntimeException si el texto no es una carta valida. */
    public static Carta textoACarta(String texto) {
        String[] p = texto.split(",");
        return new Carta(Color.valueOf(p[0]), TipoCarta.valueOf(p[1]), Integer.parseInt(p[2]));
    }

    /** Quita los caracteres que romperian el formato (para nombres de jugador). */
    public static String limpiar(String texto) {
        String t = texto == null ? "" : texto.replaceAll("[|,;:\\r\\n\\t]", "_").trim();
        if (t.length() > 16) {
            t = t.substring(0, 16);
        }
        return t.isEmpty() ? "Jugador" : t;
    }
}
