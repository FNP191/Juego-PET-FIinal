/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JuegoPetNine.modelo;

import JuegoPetNine.modelo.Color;
import JuegoPetNine.modelo.Carta;
import java.util.Collections;
import java.util.Stack;

/**
 *
 * @author ULISES
 */
public class Mazo {
    /** Cuantas copias de Cambio de Mano hay en el mazo (las demas especiales tienen 4). */
    private static final int COPIAS_CAMBIO_MANO = 1;

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
            cartasRobo.push(new Carta(Color.ESPECIAL, TipoCarta.ESPIAR_BARAJA, -1));
        }
        // Cambio de Mano es muy valiosa: sale mucho menos que las demas (1 copia en vez de 4)
        for (int i = 0; i < COPIAS_CAMBIO_MANO; i++) {
            cartasRobo.push(new Carta(Color.ESPECIAL, TipoCarta.CAMBIO_MANO, -1));
        }
    }

    public Carta robar() {
        asegurarCartas();
        return cartasRobo.pop();
    }

    public Carta verSiguiente(){ 
        asegurarCartas();
        return cartasRobo.peek(); 
    }

    /**
     * El mazo de robo nunca queda vacio:
     *  1) si se acaba, se rebaraja el descarte (menos la carta de la mesa) y se vuelve a usar;
     *  2) si ni asi hay cartas (todas estan en las manos), se agrega un mazo nuevo completo.
     */
    private void asegurarCartas() {
        if (cartasRobo.isEmpty()) {
            reorganizarMazo();
        }
        if (cartasRobo.isEmpty()) {
            generarCartas();
            Collections.shuffle(cartasRobo);
        }
    }

    public void descartar(Carta c){
        cartasDescarte.push(c); 
    }

    public Carta getCartaMesa(){ 
        return cartasDescarte.peek(); 
    }

    private void reorganizarMazo() {
        if (cartasDescarte.size() <= 1) return; // no hay cartas que reciclar
        Carta ultima = cartasDescarte.pop();
        cartasRobo.addAll(cartasDescarte);
        cartasDescarte.clear();
        cartasDescarte.push(ultima);
        Collections.shuffle(cartasRobo);
    }
}
