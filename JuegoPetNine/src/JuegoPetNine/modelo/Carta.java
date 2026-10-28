/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JuegoPetNine.modelo;

import javax.swing.ImageIcon;
import JuegoPetNine.interfaz.GestorImagenes;

/**
 *
 * @author ULISES
 */
public class Carta {
    private Color color;
    private TipoCarta tipo;
    private int valor; 

    public Carta(Color color, TipoCarta tipo, int valor) {
        this.color = color;
        this.tipo = tipo;
        this.valor = valor;
    }

    public boolean esJugableSobre(Carta cartaMesa, Color colorActualMesa) {
        if (this.color == Color.ESPECIAL || this.tipo == TipoCarta.COMODIN || this.tipo == TipoCarta.SUMA_CUATRO) {
            return true; 
        }
        if (this.color == colorActualMesa) {
            return true;
        }
        if (this.tipo == TipoCarta.NUMERO && cartaMesa.getTipo() == TipoCarta.NUMERO) {
            return this.valor == cartaMesa.getValor();
        }
        return this.tipo == cartaMesa.getTipo();
    }

    /**
     * Solo el Comodin y el +4 permiten elegir el color de la mesa. Las demas cartas especiales
     * de Nine! (Menos Dos, Espiar Mazo, Cambio de Mano, Espiar Baraja) NO lo cambian.
     */
    public boolean cambiaColor() {
        return tipo == TipoCarta.COMODIN || tipo == TipoCarta.SUMA_CUATRO;
    }

    public Color getColor() { return color; }
    public TipoCarta getTipo() { return tipo; }
    public int getValor() { return valor; }
    
public String getNombreImagen() {
    // Comodines: no llevan color en el nombre
    if (tipo == TipoCarta.COMODIN) {
        return "CambioColor.png";
    }
    if (tipo == TipoCarta.SUMA_CUATRO) {
        return "Mas4.png";
    }

    // Parte del valor
    String parteValor;
    switch (tipo) {
        case NUMERO:    parteValor = GestorImagenes.numeroAPalabra(valor); break;
        case SUMA_DOS:  parteValor = "Mas2"; break;
        case REVERSA:   parteValor = "Reversa"; break;
        case SALTO:     parteValor = "Bloqueo"; break;
        // Cartas propias de Nine! que aún no tienen imagen:
        case MENOS_DOS:     parteValor = "Menos2"; break;
        case ESPIAR_MAZO:   parteValor = "EspiarMazo"; break;
        case CAMBIO_MANO:   parteValor = "CambioMano"; break;
        case ESPIAR_BARAJA: parteValor = "EspiarBaraja"; break;
        default:            parteValor = tipo.name(); break;
    }
     
        // Las cartas ESPECIAL no llevan color en el nombre  
    if (color == Color.ESPECIAL) {
        return parteValor + ".png";   // Menos2.png, EspiarMazo.png, CambioMano.png
    }
    
    
    // Capitaliza el color: ROJO -> "Rojo"
    String c = color.name();
    c = c.charAt(0) + c.substring(1).toLowerCase();

    return GestorImagenes.nombreArchivo(parteValor, c);
    }

    public ImageIcon getImagen() {
        return GestorImagenes.getImagen(getNombreImagen());
    }

    @Override
    public String toString() {
        if (color == Color.ESPECIAL) return "[" + tipo.name() + "]";
        return "[" + color + " - " + (tipo == TipoCarta.NUMERO ? valor : tipo) + "]";
    }
}