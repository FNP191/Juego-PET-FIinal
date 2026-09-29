/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package juegopetnine;

import java.util.Collections;
import java.util.Stack;

/**
 *
 * @author ULISES
 */
public class Mazo {
    private Stack<Carta> cartasRobo = new Stack<>();
    private Stack<Carta> cartasDescarte = new Stack<>();

    public Mazo() {
        generarCartas();
        Collections.shuffle(cartasRobo);
    }

    private void generarCartas() {
        // Generar colores básicos
        Color[] colores = {Color.ROJO, Color.AMARILLO, Color.VERDE, Color.AZUL};
        for (Color c : colores) {
            cartasRobo.push(new Carta(c, TipoCarta.NUMERO, 0));
            for (int i = 1; i <= 9; i++) {
                cartasRobo.push(new Carta(c, TipoCarta.NUMERO, i));
                cartasRobo.push(new Carta(c, TipoCarta.NUMERO, i));
            }
            for (int i = 0; i < 2; i++) {
                cartasRobo.push(new Carta(c, TipoCarta.SALTO, -1));
                cartasRobo.push(new Carta(c, TipoCarta.REVERSA, -1));
                cartasRobo.push(new Carta(c, TipoCarta.SUMA_DOS, -1));
            }
        }
        // Cartas especiales UNO + Cartas NINE
        for (int i = 0; i < 4; i++) {
            cartasRobo.push(new Carta(Color.ESPECIAL, TipoCarta.COMODIN, -1));
            cartasRobo.push(new Carta(Color.ESPECIAL, TipoCarta.SUMA_CUATRO, -1));
            cartasRobo.push(new Carta(Color.ESPECIAL, TipoCarta.MENOS_DOS, -1));
            cartasRobo.push(new Carta(Color.ESPECIAL, TipoCarta.ESPIAR_MAZO, -1));
            cartasRobo.push(new Carta(Color.ESPECIAL, TipoCarta.CAMBIO_MANO, -1));
        }
    }

    public Carta robar() {
        if (cartasRobo.isEmpty()) reorganizarMazo();
        return cartasRobo.pop();
    }

    public Carta verSiguiente(){ 
        return cartasRobo.peek(); 
        }

    public void descartar(Carta c){
        cartasDescarte.push(c); 
    }

    public Carta getCartaMesa(){ 
        return cartasDescarte.peek(); 
    }

    private void reorganizarMazo() {
        Carta ultima = cartasDescarte.pop();
        cartasRobo.addAll(cartasDescarte);
        cartasDescarte.clear();
        cartasDescarte.push(ultima);
        Collections.shuffle(cartasRobo);
    }
}
