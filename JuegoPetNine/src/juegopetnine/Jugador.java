/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package juegopetnine;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ULISES
 */
public class Jugador {
    private String nombre;
    private List<Carta> mano;
    private Habilidad habilidad;
    private boolean protegido;

    public Jugador(String nombre) {
        this.nombre = nombre;
        this.mano = new ArrayList<>();
        this.protegido = false;
    }

    public void agregarCarta(Carta c){ 
        mano.add(c); 
    }
    
    public void removerCarta(Carta c){ 
        mano.remove(c); 
    }
    
    public List<Carta> getMano(){ 
        return mano; 
    }
    
    public String getNombre(){ 
        return nombre; 
    }
    
    public void setHabilidad(Habilidad habilidad){ 
        this.habilidad = habilidad; 
    }
   
    public Habilidad getHabilidad(){ 
        return habilidad; 
    }
    
    public boolean isProtegido(){ 
        return protegido; 
    }
    
    public void setProtegido(boolean protegido){ 
        this.protegido = protegido; 
    }

}
