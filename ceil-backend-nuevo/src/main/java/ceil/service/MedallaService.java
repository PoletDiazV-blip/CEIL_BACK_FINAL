package ceil.service;

import ceil.database.DatabaseConfig;
import java.sql.*;

/**
 * Fase 6 — el que por fin DISPARA las medallas. Hasta ahora solo la de TRIVIA se
 * otorgaba; las otras cinco estaban sembradas en la tabla `medallas` sin que nadie
 * comprobara su condición. Cada método `evaluar*` mira el estado real de la BD y,
 * si se cumple el criterio del brief, concede la medalla (una sola vez por usuario).
 *
 * Decisión del dueño (nº2): las medallas salen de metas de ahorro y de la reducción
 * de gastos; los puntos salen de la trivia y van aparte (`usuarios.puntos_racha`).
 *
 * Todo es JDBC directo y `static`, igual que el resto de módulos del proyecto. El
 * otorgamiento usa INSERT IGNORE contra la PK (id_usuario, id_medal), así que volver
 * a cumplir la condición no duplica la medalla ni revienta.
 */
public class MedallaService {

    // Rango de "gastos hormiga" según el brief: gastos pequeños de $1 a $20.
    // Fijo por ahora (pregunta abierta nº5 del dueño); si algún día se quiere
    // configurable, saldría de una tabla de ajustes en vez de estas constantes.
    public static final double HORMIGA_MIN = 1.0;
    public static final double HORMIGA_MAX = 20.0;

    // Cuántos aciertos seguidos en la trivia hacen falta para "Constante".
    public static final int CONSTANCIA_UMBRAL = 3;

    /**
     * Concede la medalla del tipo indicado si el usuario aún no la tiene.
     * @return true solo si se acaba de otorgar (fila nueva); false si ya la tenía
     *         o el tipo no existe.
     */
    public static boolean otorgar(int idUsuario, String tipoMedalla) {
        String buscar   = "SELECT id_medal FROM medallas WHERE tipo_medalla = ?";
        String otorgar  = "INSERT IGNORE INTO usuario_medallas (id_usuario, id_medal) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection()) {
            int idMedal = 0;
            try (PreparedStatement stmt = conn.prepareStatement(buscar)) {
                stmt.setString(1, tipoMedalla);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) idMedal = rs.getInt("id_medal");
                }
            }
            if (idMedal == 0) return false;
            try (PreparedStatement stmt = conn.prepareStatement(otorgar)) {
                stmt.setInt(1, idUsuario);
                stmt.setInt(2, idMedal);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Medallas (otorgar " + tipoMedalla + "): " + e.getMessage());
            return false;
        }
    }

    /** "Primer Ahorro": se hizo un abono a un apartado. Se llama tras cada abono con éxito. */
    public static void evaluarAhorro(int idUsuario) {
        otorgar(idUsuario, "AHORRO");
    }

    /**
     * "Meta Cumplida": la meta llegó al 100%. Además marca la meta como COMPLETADA,
     * de donde el panel admin saca su contador de metas completadas.
     */
    public static void evaluarMetaCumplida(int idMeta, int idUsuario) {
        String sql = "SELECT (monto_actual >= monto_objetivo) AS completa FROM metas_ahorro " +
                     "WHERE id_meta = ? AND id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMeta);
            stmt.setInt(2, idUsuario);
            boolean completa;
            try (ResultSet rs = stmt.executeQuery()) {
                completa = rs.next() && rs.getBoolean("completa");
            }
            if (!completa) return;

            try (PreparedStatement upd = conn.prepareStatement(
                    "UPDATE metas_ahorro SET estado = 'COMPLETADA' WHERE id_meta = ? AND id_usuario = ?")) {
                upd.setInt(1, idMeta);
                upd.setInt(2, idUsuario);
                upd.executeUpdate();
            }
            otorgar(idUsuario, "META_CUMPLIDA");
        } catch (SQLException e) {
            System.err.println("Medallas (meta cumplida): " + e.getMessage());
        }
    }

    /**
     * "Gasto con Propósito": el usuario gastó en una de las categorías que eligió como
     * de interés en el onboarding (tabla `usuario_categorias`). Se llama al registrar un gasto.
     */
    public static void evaluarGastoDirigido(int idUsuario, Integer idCategoria) {
        if (idCategoria == null) return;
        String sql = "SELECT 1 FROM usuario_categorias WHERE id_usuario = ? AND id_categoria = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setInt(2, idCategoria);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) otorgar(idUsuario, "GASTO_DIRIGIDO");
            }
        } catch (SQLException e) {
            System.err.println("Medallas (gasto dirigido): " + e.getMessage());
        }
    }

    /**
     * "Cazador de Gastos Hormiga": los gastos pequeños ($1–$20) de esta semana suman
     * menos que los de la semana pasada. Se compara la ventana de los últimos 7 días
     * contra los 7 anteriores. Se llama al registrar un gasto.
     */
    public static void evaluarReduccionGastos(int idUsuario) {
        String sql =
            "SELECT " +
            "  COALESCE(SUM(CASE WHEN fecha >= (CURDATE() - INTERVAL 6 DAY) THEN monto END), 0) AS actual, " +
            "  COALESCE(SUM(CASE WHEN fecha >= (CURDATE() - INTERVAL 13 DAY) " +
            "                     AND fecha <  (CURDATE() - INTERVAL 6 DAY) THEN monto END), 0) AS pasada " +
            "FROM movimientos " +
            "WHERE id_usuario = ? AND tipo_movimiento = 'GASTO' AND monto BETWEEN ? AND ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setDouble(2, HORMIGA_MIN);
            stmt.setDouble(3, HORMIGA_MAX);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double actual = rs.getDouble("actual");
                    double pasada = rs.getDouble("pasada");
                    // Solo cuenta si la semana pasada hubo gastos hormiga y esta semana bajaron.
                    if (pasada > 0 && actual < pasada) otorgar(idUsuario, "REDUCCION_GASTOS");
                }
            }
        } catch (SQLException e) {
            System.err.println("Medallas (reducción gastos): " + e.getMessage());
        }
    }

    /** "Constante": varias trivias acertadas seguidas sin fallar. */
    public static void evaluarConstancia(int idUsuario, int aciertosSeguidos) {
        if (aciertosSeguidos >= CONSTANCIA_UMBRAL) otorgar(idUsuario, "CONSTANCIA");
    }
}
