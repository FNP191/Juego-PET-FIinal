/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JuegoPetNine.logica;

import JuegoPetNine.modelo.Mazo;
import JuegoPetNine.modelo.Jugador;
import JuegoPetNine.modelo.Habilidad;
import JuegoPetNine.modelo.Color;
import JuegoPetNine.modelo.Carta;
import java.util.ArrayList;
import java.util.List;
import JuegoPetNine.modelo.TipoCarta;
import JuegoPetNine.modelo.TipoHabilidad;

/**
 *
 * @author ULISES
 */
public class JuegoNineControlador {
    /** Tiempo que tiene un jugador para actuar; si no juega en ese tiempo, pasa el turno. */
    public static final int SEGUNDOS_POR_TURNO = 40;

    private List<Jugador> jugadores;
    private Mazo mazo;
    private int turnoActual = 0;
    private int sentido = 1; 
    private Color colorMesaActual;
    private int saltosPendientes = 0;   // jugadores que perderan el turno cuando termine el turno actual

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

    /**
     * Juega una carta. Como en el UNO original, UNA carta por turno: despues de jugarla el turno termina.
     *  - Bloqueo y Reversa (2 jugadores): el rival pierde el turno y vuelve a jugar quien la tiro.
     *  - +2 y +4: el rival roba pero SI juega.
     */
    public boolean jugarCarta(Jugador jugador, Carta carta, Color nuevoColor) {
        if (!carta.esJugableSobre(mazo.getCartaMesa(), colorMesaActual)) {
            return false;
        }
        jugador.removerCarta(carta);
        mazo.descartar(carta);

        if (carta.cambiaColor()) {
            colorMesaActual = nuevoColor;            // Comodin y +4: el jugador elige
        } else if (carta.getColor() != Color.ESPECIAL) {
            colorMesaActual = carta.getColor();      // carta normal: el color de la carta
        }                                            // Espiar, Cambio de Mano...: el color de la mesa no cambia

        saltosPendientes += ejecutarEfecto(carta, jugador);
        terminarTurno();
        return true;
    }

    /** Efecto de la carta. Devuelve cuantos jugadores perderan el turno por ella (+2 y +4: el rival roba pero SI juega). */
    private int ejecutarEfecto(Carta carta, Jugador actual) {
        switch (carta.getTipo()) {
            case SALTO: return 1;
            case REVERSA: sentido *= -1; return jugadores.size() == 2 ? 1 : 0;
            case SUMA_DOS: atacar(getSiguienteJugador(), 2, actual); return 0;
            case SUMA_CUATRO: atacar(getSiguienteJugador(), 4, actual); return 0;
            case ESPIAR_MAZO: System.out.println("--> Espiaste el mazo: La próxima carta es " + mazo.verSiguiente()); return 0;
            case CAMBIO_MANO: 
                Jugador rival = getSiguienteJugador();
                List<Carta> temp = new ArrayList<>(actual.getMano());
                actual.getMano().clear(); actual.getMano().addAll(rival.getMano());
                rival.getMano().clear(); rival.getMano().addAll(temp);
                System.out.println("¡Cambiaste tu mano con " + rival.getNombre() + "!");
                return 0;
            default: return 0;
        }
    }

    /** Pasa el turno al siguiente jugador, saltando a los que hayan perdido el turno (Bloqueo y Reversa; solo con 3 o mas jugadores). */
    public void terminarTurno() {
        // Bloqueo/Reversa: con 2 jugadores el rival pierde el turno y vuelve a jugar quien la tiro.
        // Con 3 o mas, los saltos nunca pueden devolverle el turno a quien acaba de jugar.
        saltosPendientes = Math.min(saltosPendientes, jugadores.size() == 2 ? 1 : jugadores.size() - 2);
        avanzarTurno();
        while (saltosPendientes > 0) {
            avanzarTurno();
            saltosPendientes--;
        }
    }

    private void atacar(Jugador objetivo, int cantidad, Jugador atacante) {
        Habilidad h = objetivo.getHabilidad();
        if (h != null && h.getTipo() == TipoHabilidad.ESPEJO && h.getUsosRestantes() > 0) {
            System.out.println(objetivo.getNombre() + " usó ESPEJO! El ataque vuelve a " + atacante.getNombre());
            for (int i = 0; i < cantidad; i++) atacante.agregarCarta(mazo.robar());
            h.usar();
        } else if (h != null && h.getTipo() == TipoHabilidad.PROTECCION && h.getUsosRestantes() > 0) {
            System.out.println(objetivo.getNombre() + " se PROTEGIÓ del ataque.");
            h.usar();
        } else {
            System.out.println(objetivo.getNombre() + " recibe " + cantidad + " cartas.");
            for (int i = 0; i < cantidad; i++) objetivo.agregarCarta(mazo.robar());
        }
    }

    /** Roba una carta. El jugador NO pierde el turno: sigue jugando (puede robar otra o tirar una carta; despues de tirar, el turno termina). */
    public void robarCarta(Jugador j) { j.agregarCarta(mazo.robar()); }
    public void avanzarTurno() { turnoActual = (turnoActual + sentido + jugadores.size()) % jugadores.size(); }
    
    public Jugador getJugadorActual() { return jugadores.get(turnoActual); }
    public Jugador getSiguienteJugador() { return jugadores.get((turnoActual + sentido + jugadores.size()) % jugadores.size()); }
    public Carta getCartaMesa() { return mazo.getCartaMesa(); }
    public Color getColorMesa() { return colorMesaActual; }
    public List<Jugador> getJugadores() { return jugadores; }

    public Carta verSiguienteCartaMazo() {
        return mazo.verSiguiente();
    }

    public void descartarEnMesa(Carta carta) {
        mazo.descartar(carta);
    }
}
