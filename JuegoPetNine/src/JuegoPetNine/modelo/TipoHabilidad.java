/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JuegoPetNine.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 * @author ULISES
 */
public enum TipoHabilidad {
    DESCARTE_DOBLE("Tiras 2 cartas por turno."),
    PROTECCION("Te protege ante cartas que sumen cartas."),
    DUPEO("Te duplica la carta que vos elijas."),
    ESPEJO("Devuelve el ataque de +2 o +4 al atacante."),
    INTERCAMBIO("Robas una carta del rival y le das una tuya.");

    private final String descripcion;

    TipoHabilidad(String descripcion) {
        this.descripcion = descripcion;
    }
    public String getDescripcion(){ 
        return descripcion; 
    }

    /** Habilidades que el juego realmente implementa (DESCARTE_DOBLE todavia no hace nada). */
    public static List<TipoHabilidad> disponibles() {
        List<TipoHabilidad> lista = new ArrayList<>();
        for (TipoHabilidad t : values()) {
            if (t != DESCARTE_DOBLE) {
                lista.add(t);
            }
        }
        return lista;
    }

    /** Sortea 2 habilidades distintas entre las disponibles: el jugador elige una de las dos. */
    public static TipoHabilidad[] sortearDos(Random azar) {
        List<TipoHabilidad> lista = disponibles();
        Collections.shuffle(lista, azar);
        return new TipoHabilidad[]{lista.get(0), lista.get(1)};
    }
        
}
