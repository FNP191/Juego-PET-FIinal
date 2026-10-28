package JuegoPetNine.interfaz;

import JuegoPetNine.modelo.TipoHabilidad;
import java.awt.Component;
import javax.swing.JOptionPane;

/** Cuadro donde el jugador elige entre 2 habilidades sorteadas. Lo usan el modo local y el de red. */
public final class DialogoHabilidad {

    private DialogoHabilidad() { }

    /** Devuelve la habilidad elegida. Si se cierra el cuadro sin elegir, se queda con la primera opcion. */
    public static TipoHabilidad elegir(Component padre, String jugador, TipoHabilidad a, TipoHabilidad b) {
        TipoHabilidad[] opciones = {a, b};
        Object[] botones = {texto(a), texto(b)};
        int r = JOptionPane.showOptionDialog(padre,
                jugador + ", elige tu habilidad (podras usarla hasta 2 veces):",
                "Elige tu habilidad", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, botones, botones[0]);
        return opciones[r == 1 ? 1 : 0];
    }

    private static String texto(TipoHabilidad t) {
        return "<html><center><b>" + t.name() + "</b><br>" + t.getDescripcion() + "</center></html>";
    }
}
