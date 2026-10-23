/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JuegoPetNine.interfaz;
import JuegoPetNine.modelo.Carta;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Cursor;
/**
 *
 * @author ULISES
 */

public class BotonCarta extends JButton {
    private Carta carta;
    private boolean seleccionado;

    public BotonCarta(Carta carta) {
        this.carta = carta;
        this.seleccionado = false;
        setText(carta != null ? carta.toString() : "");
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(110, 150));
        actualizarEstilo();

        // Si la carta tiene imagen, se muestra la imagen en lugar del texto
        javax.swing.ImageIcon icono = (carta != null) ? carta.getImagen() : null;
        if (icono != null) {
            setText("");
            setIcon(icono);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setMargin(new java.awt.Insets(0, 0, 0, 0));
            setPreferredSize(new Dimension(GestorImagenes.ANCHO_CARTA, GestorImagenes.ALTO_CARTA));
        }
         actualizarEstilo();
    }

    public void actualizarEstilo() {
        if (carta == null || carta.getColor() == null) return;
        boolean conImagen = getIcon() != null;

        // Evaluación de los colores según el enum propio del mazo (ROJO, AMARILLO, VERDE, AZUL, ESPECIAL)
        switch (carta.getColor()) {
            case ESPECIAL:
                setBackground(new java.awt.Color(44, 62, 80));
                setForeground(java.awt.Color.WHITE);
                break;
            case ROJO:
                setBackground(new java.awt.Color(231, 76, 60));
                setForeground(java.awt.Color.WHITE);
                break;
            case AMARILLO:
                setBackground(new java.awt.Color(241, 196, 15));
                setForeground(java.awt.Color.BLACK);
                break;
            case VERDE:
                setBackground(new java.awt.Color(46, 204, 113));
                setForeground(java.awt.Color.WHITE);
                break;
            case AZUL:
                setBackground(new java.awt.Color(52, 152, 219));
                setForeground(java.awt.Color.WHITE);
                break;
        }

        if (seleccionado) {
            setBorderPainted(true);
            setBorder(BorderFactory.createLineBorder(java.awt.Color.CYAN, 4));
        } else if (conImagen) {
            setBorderPainted(false);
        } else {
            setBorder(BorderFactory.createLineBorder(java.awt.Color.DARK_GRAY, 1));
        }
    }

    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado = seleccionado;
        actualizarEstilo();
    }

    public boolean isSeleccionado() {
        return seleccionado;
    }

    public Carta getCarta() {
        return carta;
    }
}
