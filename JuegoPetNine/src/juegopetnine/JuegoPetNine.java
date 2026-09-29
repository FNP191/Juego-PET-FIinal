/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package juegopetnine;

import java.util.Arrays;
import java.util.Scanner;

/**
 *
 * @author ULISES
 */
public class JuegoPetNine {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== BIEVENIDO A NINE! ===");
        JuegoNineControlador juego = new JuegoNineControlador(Arrays.asList("Jugador", "Rival"));
        
        // Asignar habilidades para probar
        juego.getJugadores().get(0).setHabilidad(new Habilidad(TipoHabilidad.DESCARTE_DOBLE));
        juego.getJugadores().get(1).setHabilidad(new Habilidad(TipoHabilidad.ESPEJO));

        juego.iniciar();
        boolean terminar = false;

        while (!terminar) {
            Jugador actual = juego.getJugadorActual();
            System.out.println("\n-------------------------------------------");
            System.out.println("Turno de: " + actual.getNombre() + " | Habilidad: " + actual.getHabilidad().getTipo() + " (Usos: " + actual.getHabilidad().getUsosRestantes() + ")");
            System.out.println("Carta en la mesa: " + juego.getCartaMesa() + " | Color actual: " + juego.getColorMesa());
            System.out.println("-------------------------------------------");

            System.out.println("Tu mano:");
            for (int i = 0; i < actual.getMano().size(); i++) {
                System.out.println(i + ": " + actual.getMano().get(i));
            }
            
            int indexRobar = actual.getMano().size();
            System.out.println(indexRobar + ": [ROBAR CARTA]");
            System.out.print("Elige una opción: ");
            
            int opcion = scanner.nextInt();

            if (opcion == indexRobar) {
                System.out.println("Robaste una carta.");
                juego.robarCarta(actual);
            } else if (opcion >= 0 && opcion < indexRobar) {
                Carta cartaElegida = actual.getMano().get(opcion);
                Color colorElegido = Color.ROJO; // Por defecto

                if (cartaElegida.getColor() == Color.ESPECIAL) {
                    System.out.println("Elige color: 1.ROJO 2.AMARILLO 3.VERDE 4.AZUL");
                    int col = scanner.nextInt();
                    if(col==2) colorElegido = Color.AMARILLO;
                    if(col==3) colorElegido = Color.VERDE;
                    if(col==4) colorElegido = Color.AZUL;
                }

                boolean exito = juego.jugarCarta(actual, cartaElegida, colorElegido);
                if (!exito) {
                    System.out.println("¡NO PUEDES JUGAR ESA CARTA! Pierdes el turno.");
                }

                if (actual.getMano().isEmpty()) {
                    System.out.println("\n¡" + actual.getNombre().toUpperCase() + " HA GANADO NINE!");
                    terminar = true;
                }
            } else {
                System.out.println("Opción inválida.");
            }
        }
        scanner.close();
    }
}
    
    

