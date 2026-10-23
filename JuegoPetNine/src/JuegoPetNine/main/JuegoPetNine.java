/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package JuegoPetNine.main;

import javax.swing.SwingUtilities;
import JuegoPetNine.interfaz.MenuPrincipal;

/**
 *
 * @author ULISES
 */
public class JuegoPetNine {

    public static void main(String[] args) {
        // Abre el menú: jugar local, crear partida en red (servidor) o unirse
     SwingUtilities.invokeLater(() -> new MenuPrincipal().setVisible(true));
    }
}
