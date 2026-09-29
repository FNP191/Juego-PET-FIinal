/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package juegopetnine;

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
        
}
