/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package juegopetnine;

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

    public Color getColor() { return color; }
    public TipoCarta getTipo() { return tipo; }
    public int getValor() { return valor; }

    @Override
    public String toString() {
        if (color == Color.ESPECIAL) return "[" + tipo.name() + "]";
        return "[" + color + " - " + (tipo == TipoCarta.NUMERO ? valor : tipo) + "]";
    }
}