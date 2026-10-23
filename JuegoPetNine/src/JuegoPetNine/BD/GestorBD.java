package JuegoPetNine.BD;

import java.sql.*;

/**
 * @author ulises
 */
public class GestorBD {

    private static final String URL = "jdbc:mariadb://localhost:3306/JuegoNINE";
    private static final String USUARIO = "alumno";
    private static final String PASSWORD = "1234";

    // Método reutilizable para conectar desde cualquier parte del juego
    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }
    
        // Busca el id por username; si no existe, lo crea y devuelve el id nuevo
    private static int obtenerOcrearJugador(String username) throws SQLException {
        String sqlBuscar = "SELECT id FROM Jugadores WHERE username = ?";
        String sqlInsertar = "INSERT INTO Jugadores (username, password_hash) VALUES (?, ?)";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        // No existe -> lo creamos con password por defecto (después puedes mejorar esto)
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sqlInsertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, "pendiente"); // idealmente aquí iría un hash real
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
        }

        throw new SQLException("No se pudo crear el jugador " + username);
    }

    public static void registrarVictoria(String nombreJugador) {
        String sql = "INSERT INTO Estadisticas (id_jugador, partidas_jugadas, partidas_ganadas) "
                   + "VALUES (?, 1, 1) "
                   + "ON DUPLICATE KEY UPDATE partidas_jugadas = partidas_jugadas + 1, "
                   + "partidas_ganadas = partidas_ganadas + 1";

        try {
            int id = obtenerOcrearJugador(nombreJugador);
            try (Connection con = conectar();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
                System.out.println("Victoria de " + nombreJugador + " registrada ✅");
            }
        } catch (SQLException e) {
            System.out.println("Error al registrar victoria: " + e.getMessage());
        }
    }

    public static void registrarDerrota(String nombreJugador) {
        String sql = "INSERT INTO Estadisticas (id_jugador, partidas_jugadas, partidas_perdidas) "
                   + "VALUES (?, 1, 1) "
                   + "ON DUPLICATE KEY UPDATE partidas_jugadas = partidas_jugadas + 1, "
                   + "partidas_perdidas = partidas_perdidas + 1";

        try {
            int id = obtenerOcrearJugador(nombreJugador);
            try (Connection con = conectar();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error al registrar derrota: " + e.getMessage());
        }
    }



    // main solo para PROBAR que la conexión funciona
    public static void main(String[] args) {
        try (Connection conexion = GestorBD.conectar()) {
            System.out.println("¡CONEXIÓN EXITOSA! Java ya está conectado a JuegoNINE.");
        } catch (SQLException error) {
            System.out.println("ERROR AL CONECTAR: " + error.getMessage());
        }
    }
}
