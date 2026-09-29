/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package juegopetnine;

/**
 *
 * @author ULISES
 */
public class Habilidad {
    private TipoHabilidad tipo;
    private int usosRestantes;

    public Habilidad(TipoHabilidad tipo) {
        this.tipo = tipo;
        this.usosRestantes = 2; 
    }

    public boolean usar() {
        if (usosRestantes > 0) {
            usosRestantes--;
            return true;
        }
        return false;
    }

    public TipoHabilidad getTipo() { 
        return tipo; 
    }
    
    public int getUsosRestantes() { 
        return usosRestantes; 
    }

}

