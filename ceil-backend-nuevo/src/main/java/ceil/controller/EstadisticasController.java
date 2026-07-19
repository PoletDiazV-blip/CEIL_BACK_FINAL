package ceil.controller;

import io.javalin.http.Context;
import ceil.database.DatabaseConfig;
import ceil.service.MedallaService;
import ceil.security.Sesion;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Fase 6 — el motor de estadísticas real que alimenta la pantalla de Gráficas, que
 * hasta ahora era 100% maqueta (barras y porcentajes hardcodeados en el Kotlin).
 *
 * Todo lo que devuelve sale de consultas sobre `movimientos` y `metas_ahorro`:
 *  · gastos por día (últimos 7 días),
 *  · balance de la semana (ingresos − gastos),
 *  · progreso de cada apartado de ahorro,
 *  · desglose de gasto por categoría (con la que más gasta a la cabeza),
 *  · gastos hormiga ($1–$20 según el brief) de esta semana vs la pasada.
 *
 * No se inventa ninguna serie que la base no pueda sostener: no hay bitácora de abonos
 * con fecha, así que el ahorro se muestra como progreso por meta y no como serie semanal.
 */
public class EstadisticasController {

    private static final Locale ES = new Locale("es", "ES");

    public void obtenerEstadisticas(Context ctx) {
        int idUsuario = Sesion.uid(ctx); // Fase 7: identidad desde el token, no del {id} de la ruta.

        Map<String, Object> salida = new LinkedHashMap<>();
        try (Connection conn = DatabaseConfig.getConnection()) {

            // --- Ingreso configurado y saldo de metas ---------------------------------
            double ingresoTotal = escalar(conn,
                "SELECT COALESCE(ingreso_total, 0) v FROM usuarios WHERE id_usuario = ?", idUsuario);
            double ahorroTotal = escalar(conn,
                "SELECT COALESCE(SUM(monto_actual), 0) v FROM metas_ahorro WHERE id_usuario = ?", idUsuario);
            double gastoTotal = escalar(conn,
                "SELECT COALESCE(SUM(monto), 0) v FROM movimientos " +
                "WHERE id_usuario = ? AND tipo_movimiento = 'GASTO'", idUsuario);

            salida.put("ingreso_total", ingresoTotal);
            salida.put("ahorro_total", ahorroTotal);
            salida.put("gasto_total", gastoTotal);

            // --- Semana en curso (últimos 7 días) -------------------------------------
            double gastosSemana = escalar(conn,
                "SELECT COALESCE(SUM(monto), 0) v FROM movimientos WHERE id_usuario = ? " +
                "AND tipo_movimiento = 'GASTO' AND fecha >= (CURDATE() - INTERVAL 6 DAY)", idUsuario);
            double ingresosSemana = escalar(conn,
                "SELECT COALESCE(SUM(monto), 0) v FROM movimientos WHERE id_usuario = ? " +
                "AND tipo_movimiento = 'INGRESO' AND fecha >= (CURDATE() - INTERVAL 6 DAY)", idUsuario);

            salida.put("ingresos_semana", ingresosSemana);
            salida.put("gastos_semana", gastosSemana);
            salida.put("balance_semana", ingresosSemana - gastosSemana);

            // --- Gastos por día (7 casillas fijas, hoy a la derecha) ------------------
            salida.put("gastos_por_dia", gastosPorDia(conn, idUsuario));

            // --- Progreso de cada apartado de ahorro ----------------------------------
            salida.put("metas", metas(conn, idUsuario));

            // --- Gasto por categoría (la que más gasta primero) -----------------------
            List<Map<String, Object>> categorias = categorias(conn, idUsuario, gastoTotal);
            salida.put("categorias", categorias);
            salida.put("categoria_top", categorias.isEmpty() ? null : categorias.get(0).get("nombre"));

            // --- Gastos hormiga ($1–$20): esta semana vs la pasada --------------------
            double[] hormiga = hormiga(conn, idUsuario);
            double hormigaActual = hormiga[0];
            double hormigaPasada = hormiga[1];
            salida.put("hormiga_semana_actual", hormigaActual);
            salida.put("hormiga_semana_pasada", hormigaPasada);
            // Variación % respecto a la semana pasada. Negativo = mejoró (gastó menos).
            int variacion = 0;
            if (hormigaPasada > 0) {
                variacion = (int) Math.round((hormigaActual - hormigaPasada) / hormigaPasada * 100.0);
            }
            salida.put("hormiga_variacion_pct", variacion);

        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
            return;
        }
        ctx.status(200).json(salida);
    }

    // ---- Helpers ------------------------------------------------------------------

    private double escalar(Connection conn, String sql, int idUsuario) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("v") : 0.0;
            }
        }
    }

    /** Los últimos 7 días con su total de gastos; 0 en los días sin movimientos. */
    private List<Map<String, Object>> gastosPorDia(Connection conn, int idUsuario) throws SQLException {
        Map<String, Double> porFecha = new HashMap<>();
        String sql = "SELECT DATE(fecha) d, SUM(monto) t FROM movimientos " +
                     "WHERE id_usuario = ? AND tipo_movimiento = 'GASTO' " +
                     "AND fecha >= (CURDATE() - INTERVAL 6 DAY) GROUP BY DATE(fecha)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) porFecha.put(rs.getDate("d").toLocalDate().toString(), rs.getDouble("t"));
            }
        }
        List<Map<String, Object>> dias = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            String etiqueta = dia.getDayOfWeek()
                    .getDisplayName(TextStyle.NARROW, ES).toUpperCase(); // L, M, X...
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("etiqueta", etiqueta);
            item.put("fecha", dia.toString());
            item.put("total", porFecha.getOrDefault(dia.toString(), 0.0));
            dias.add(item);
        }
        return dias;
    }

    private List<Map<String, Object>> metas(Connection conn, int idUsuario) throws SQLException {
        List<Map<String, Object>> metas = new ArrayList<>();
        String sql = "SELECT nombre_meta, monto_actual, monto_objetivo FROM metas_ahorro " +
                     "WHERE id_usuario = ? ORDER BY (monto_actual / NULLIF(monto_objetivo,0)) DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    double actual = rs.getDouble("monto_actual");
                    double objetivo = rs.getDouble("monto_objetivo");
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("nombre_meta", rs.getString("nombre_meta"));
                    m.put("monto_actual", actual);
                    m.put("monto_objetivo", objetivo);
                    m.put("progreso", objetivo > 0 ? Math.min(actual / objetivo, 1.0) : 0.0);
                    metas.add(m);
                }
            }
        }
        return metas;
    }

    private List<Map<String, Object>> categorias(Connection conn, int idUsuario, double gastoTotal) throws SQLException {
        List<Map<String, Object>> categorias = new ArrayList<>();
        String sql = "SELECT COALESCE(c.nombre, m.categoria_nombre) nombre, " +
                     "       COALESCE(c.color_hex, '#6B7280') color, SUM(m.monto) total " +
                     "FROM movimientos m LEFT JOIN categorias c ON c.id_categoria = m.id_categoria " +
                     "WHERE m.id_usuario = ? AND m.tipo_movimiento = 'GASTO' " +
                     "GROUP BY COALESCE(c.nombre, m.categoria_nombre), COALESCE(c.color_hex, '#6B7280') " +
                     "ORDER BY total DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    double total = rs.getDouble("total");
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("nombre", rs.getString("nombre"));
                    c.put("color_hex", rs.getString("color"));
                    c.put("total", total);
                    c.put("porcentaje", gastoTotal > 0 ? total / gastoTotal : 0.0);
                    categorias.add(c);
                }
            }
        }
        return categorias;
    }

    /** {actual, pasada}: suma de gastos hormiga ($1–$20) de los últimos 7 días y de los 7 previos. */
    private double[] hormiga(Connection conn, int idUsuario) throws SQLException {
        String sql =
            "SELECT " +
            "  COALESCE(SUM(CASE WHEN fecha >= (CURDATE() - INTERVAL 6 DAY) THEN monto END), 0) actual, " +
            "  COALESCE(SUM(CASE WHEN fecha >= (CURDATE() - INTERVAL 13 DAY) " +
            "                     AND fecha <  (CURDATE() - INTERVAL 6 DAY) THEN monto END), 0) pasada " +
            "FROM movimientos WHERE id_usuario = ? AND tipo_movimiento = 'GASTO' AND monto BETWEEN ? AND ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setDouble(2, MedallaService.HORMIGA_MIN);
            stmt.setDouble(3, MedallaService.HORMIGA_MAX);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return new double[]{ rs.getDouble("actual"), rs.getDouble("pasada") };
            }
        }
        return new double[]{ 0.0, 0.0 };
    }
}
