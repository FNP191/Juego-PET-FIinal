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
public class JuegoNineControlador {
    private List<Jugador> jugadores;
    private Mazo mazo;
    private int turnoActual = 0;
    private int sentido = 1; 
    private Color colorMesaActual;
    private boolean saltarTurnoExtra = false;

    public JuegoNineControlador(List<String> nombres) {
        jugadores = new ArrayList<>();
        for (String nombre : nombres) {
            jugadores.add(new Jugador(nombre));
        }
        mazo = new Mazo();
    }

    public void iniciar() {
        // Repartir 9 cartas a cada jugador
        for (Jugador j : jugadores) {
            for (int i = 0; i < 9; i++) {
                j.agregarCarta(mazo.robar());
            }
        }
        Carta inicial = mazo.robar();
        while(inicial.getColor() == Color.ESPECIAL){
            mazo.descartar(inicial);
            inicial = mazo.robar();
        }
        mazo.descartar(inicial);
        colorMesaActual = inicial.getColor();
    }

    public boolean jugarCarta(Jugador jugador, Carta carta, Color nuevoColor) {
        if (carta.esJugableSobre(mazo.getCartaMesa(), colorMesaActual)) {
            jugador.removerCarta(carta);
            mazo.descartar(carta);
            
            colorMesaActual = (carta.getColor() == Color.ESPECIAL) ? nuevoColor : carta.getColor();
            ejecutarEfecto(carta, jugador);
            
            if(!saltarTurnoExtra){
                avanzarTurno();
            } else {
                saltarTurnoExtra = false; // El jugador mantiene su turno por el -2
            }
            return true;
        }
        return false;
    }

    private void ejecutarEfecto(Carta carta, Jugador actual) {
        switch (carta.getTipo()) {
            case SALTO: avanzarTurno(); break;
            case REVERSA: sentido *= -1; if(jugadores.size() == 2) avanzarTurno(); break;
            case SUMA_DOS: atacar(getSiguienteJugador(), 2, actual); break;
            case SUMA_CUATRO: atacar(getSiguienteJugador(), 4, actual); avanzarTurno(); break;
            case MENOS_DOS: saltarTurnoExtra = true; System.out.println("¡Tiras otra carta!"); break;
            case ESPIAR_MAZO: System.out.println("--> Espiaste el mazo: La próxima carta es " + mazo.verSiguiente()); break;
            case CAMBIO_MANO: 
                Jugador rival = getSiguienteJugador();
                List<Carta> temp = new ArrayList<>(actual.getMano());
                actual.getMano().clear(); actual.getMano().addAll(rival.getMano());
                rival.getMano().clear(); rival.getMano().addAll(temp);
                System.out.println("¡Cambiaste tu mano con " + rival.getNombre() + "!");
                break;
            default: break;
        }
    }

    private void atacar(Jugador objetivo, int cantidad, Jugador atacante) {
        if (objetivo.getHabilidad().getTipo() == TipoHabilidad.ESPEJO && objetivo.getHabilidad().getUsosRestantes() > 0) {
            System.out.println(objetivo.getNombre() + " usó ESPEJO! El ataque vuelve a " + atacante.getNombre());
            for (int i = 0; i < cantidad; i++) atacante.agregarCarta(mazo.robar());
            objetivo.getHabilidad().usar();
        } else if (objetivo.getHabilidad().getTipo() == TipoHabilidad.PROTECCION && objetivo.getHabilidad().getUsosRestantes() > 0) {
            System.out.println(objetivo.getNombre() + " se PROTAGIÓ del ataque.");
            objetivo.getHabilidad().usar();
        } else {
            System.out.println(objetivo.getNombre() + " recibe " + cantidad + " cartas.");
            for (int i = 0; i < cantidad; i++) objetivo.agregarCarta(mazo.robar());
        }
    }

    public void robarCarta(Jugador j) { j.agregarCarta(mazo.robar()); avanzarTurno(); }
    public void avanzarTurno() { turnoActual = (turnoActual + sentido + jugadores.size()) % jugadores.size(); }
    
    public Jugador getJugadorActual() { return jugadores.get(turnoActual); }
    public Jugador getSiguienteJugador() { return jugadores.get((turnoActual + sentido + jugadores.size()) % jugadores.size()); }
    public Carta getCartaMesa() { return mazo.getCartaMesa(); }
    public Color getColorMesa() { return colorMesaActual; }
    public List<Jugador> getJugadores() { return jugadores; }
}
