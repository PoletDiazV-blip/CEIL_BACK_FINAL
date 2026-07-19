package ceil.dao;

import ceil.database.DatabaseConfig;
import ceil.model.DashboardAdmin;
import ceil.model.AlumnoRendimiento;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Fase 6 — el panel admin por fin dice la verdad.
 *
 * Antes esta clase MENTÍA de dos formas: (1) trataba un cero legítimo como un fallo
 * (`metasCompletadas == 0 ? 3420 : real`) y lo cambiaba por un número inventado, y
 * (2) devolvía cinco usuarios de mentira ("Carlos Ruiz", "María López"...) y siete
 * métricas de impacto quemadas. Ahora todo sale de consultas reales; un cero es un
 * cero. Las cuentas de admin no se cuentan como usuarios (WHERE rol = 'USER').
 */
public class AdminDaoImpl implements AdminDao {

    private static final Locale ES = new Locale("es", "ES");

    @Override
    public DashboardAdmin obtenerMetricasGlobales() {
        DashboardAdmin dash = new DashboardAdmin();
        try (Connection conn = DatabaseConfig.getConnection()) {
            int totalUsuarios = entero(conn, "SELECT COUNT(*) v FROM usuarios WHERE rol = 'USER'");

            dash.setUsuariosRegistrados(totalUsuarios);
            // Activo = registró algún movimiento o jugó la trivia en los últimos 30 días.
            dash.setUsuariosActivos(entero(conn,
                "SELECT COUNT(*) v FROM usuarios u WHERE u.rol = 'USER' AND (" +
                "  EXISTS (SELECT 1 FROM movimientos m WHERE m.id_usuario = u.id_usuario " +
                "          AND m.fecha >= (NOW() - INTERVAL 30 DAY)) " +
                "  OR u.ultima_trivia >= (CURDATE() - INTERVAL 30 DAY))"));
            dash.setMetasCompletadas(entero(conn,
                "SELECT COUNT(*) v FROM metas_ahorro WHERE estado = 'COMPLETADA'"));
            dash.setAhorroAcumulado(decimal(conn,
                "SELECT COALESCE(SUM(monto_actual), 0) v FROM metas_ahorro"));
            dash.setQuizzesRespondidos(entero(conn, "SELECT COUNT(*) v FROM trivia_respuestas"));
            // "Racha promedio": media de aciertos seguidos en la trivia entre los usuarios.
            dash.setRachaPromedioDias((int) Math.round(decimal(conn,
                "SELECT COALESCE(AVG(trivia_aciertos_seguidos), 0) v FROM usuarios WHERE rol = 'USER'")));

            // Impacto en usuarios: porcentaje sobre el total de usuarios reales.
            dash.setPorcAhorran(porcentaje(entero(conn,
                "SELECT COUNT(DISTINCT id_usuario) v FROM metas_ahorro WHERE monto_actual > 0"), totalUsuarios));
            dash.setPorcRegistranGastos(porcentaje(entero(conn,
                "SELECT COUNT(DISTINCT id_usuario) v FROM movimientos WHERE tipo_movimiento = 'GASTO'"), totalUsuarios));
            dash.setPorcCompletaronMetas(porcentaje(entero(conn,
                "SELECT COUNT(DISTINCT id_usuario) v FROM metas_ahorro WHERE estado = 'COMPLETADA'"), totalUsuarios));
            // "Mejoraron hábitos" = ganaron la medalla de reducción de gastos hormiga.
            dash.setPorcMejoraronHabitos(porcentaje(entero(conn,
                "SELECT COUNT(DISTINCT um.id_usuario) v FROM usuario_medallas um " +
                "JOIN medallas m ON m.id_medal = um.id_medal WHERE m.tipo_medalla = 'REDUCCION_GASTOS'"), totalUsuarios));

        } catch (SQLException e) {
            System.err.println("AdminDao (métricas): " + e.getMessage());
        }
        return dash;
    }

    @Override
    public List<AlumnoRendimiento> obtenerListadoUsuariosRendimiento() {
        List<AlumnoRendimiento> lista = new ArrayList<>();
        String sql =
            "SELECT u.id_usuario, u.username, u.puntos_racha, u.ultima_trivia, " +
            "  COALESCE((SELECT SUM(g.monto_actual) FROM metas_ahorro g WHERE g.id_usuario = u.id_usuario), 0) ahorro, " +
            "  (SELECT COUNT(*) FROM usuario_medallas um WHERE um.id_usuario = u.id_usuario) medallas, " +
            "  (SELECT MAX(m.fecha) FROM movimientos m WHERE m.id_usuario = u.id_usuario) ult_mov " +
            "FROM usuarios u WHERE u.rol = 'USER' ORDER BY u.puntos_racha DESC, ahorro DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            LocalDate hoy = LocalDate.now();
            while (rs.next()) {
                String nombre = rs.getString("username");
                int racha = rs.getInt("puntos_racha");
                double ahorro = rs.getDouble("ahorro");
                int medallas = rs.getInt("medallas");

                // Activo si hubo movimiento o trivia en los últimos 30 días.
                boolean activo = false;
                Timestamp ultMov = rs.getTimestamp("ult_mov");
                if (ultMov != null && !ultMov.toLocalDateTime().toLocalDate().isBefore(hoy.minusDays(30))) activo = true;
                java.sql.Date ultTrivia = rs.getDate("ultima_trivia");
                if (ultTrivia != null && !ultTrivia.toLocalDate().isBefore(hoy.minusDays(30))) activo = true;

                lista.add(new AlumnoRendimiento(
                    nombre, racha, ahorro,
                    activo ? "Activo" : "Inactivo",
                    nivelPorMedallas(medallas)));
            }
        } catch (SQLException e) {
            System.err.println("AdminDao (usuarios): " + e.getMessage());
        }
        return lista;
    }

    @Override
    public Map<String, Object> obtenerAnaliticaQuiz() {
        Map<String, Object> out = new LinkedHashMap<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            int total = entero(conn, "SELECT COUNT(*) v FROM trivia_respuestas");
            out.put("total_respuestas", total);
            out.put("promedio_aciertos", (int) Math.round(decimal(conn,
                "SELECT COALESCE(AVG(correcta), 0) * 100 v FROM trivia_respuestas")));
            // Completitud: % de usuarios que han jugado alguna vez la trivia.
            int totalUsuarios = entero(conn, "SELECT COUNT(*) v FROM usuarios WHERE rol = 'USER'");
            int hanJugado = entero(conn,
                "SELECT COUNT(*) v FROM usuarios WHERE rol = 'USER' AND ultima_trivia IS NOT NULL");
            out.put("tasa_completitud", porcentaje(hanJugado, totalUsuarios));

            out.put("pregunta_mas_acertada", preguntaExtrema(conn, true));
            out.put("pregunta_mas_fallada", preguntaExtrema(conn, false));
            out.put("participacion_mensual", participacionMensual(conn));
        } catch (SQLException e) {
            System.err.println("AdminDao (quiz): " + e.getMessage());
        }
        return out;
    }

    @Override
    public Map<String, Object> obtenerAnaliticaApartados() {
        Map<String, Object> out = new LinkedHashMap<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Apartados más "utilizados" = los de mayor progreso, en toda la base.
            List<Map<String, Object>> apartados = new ArrayList<>();
            String sql = "SELECT nombre_meta, monto_actual, monto_objetivo FROM metas_ahorro " +
                         "ORDER BY (monto_actual / NULLIF(monto_objetivo,0)) DESC, monto_actual DESC LIMIT 6";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    double actual = rs.getDouble("monto_actual");
                    double objetivo = rs.getDouble("monto_objetivo");
                    Map<String, Object> a = new LinkedHashMap<>();
                    a.put("nombre_meta", rs.getString("nombre_meta"));
                    a.put("monto_actual", actual);
                    a.put("monto_objetivo", objetivo);
                    a.put("progreso", objetivo > 0 ? Math.min(actual / objetivo, 1.0) : 0.0);
                    apartados.add(a);
                }
            }
            out.put("apartados_top", apartados);
            out.put("gastos_por_dia", gastosPorDiaGlobal(conn));
        } catch (SQLException e) {
            System.err.println("AdminDao (apartados): " + e.getMessage());
        }
        return out;
    }

    // ---- Helpers ------------------------------------------------------------------

    private int entero(Connection conn, String sql) throws SQLException {
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getInt("v") : 0;
        }
    }

    private double decimal(Connection conn, String sql) throws SQLException {
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getDouble("v") : 0.0;
        }
    }

    private int porcentaje(int parte, int total) {
        return total <= 0 ? 0 : (int) Math.round(parte * 100.0 / total);
    }

    private String nivelPorMedallas(int medallas) {
        if (medallas >= 5) return "Platino";
        if (medallas >= 3) return "Oro";
        if (medallas >= 1) return "Plata";
        return "Bronce";
    }

    /** La pregunta con mayor (o menor) tasa de acierto, entre las que tienen respuestas. */
    private Map<String, Object> preguntaExtrema(Connection conn, boolean masAcertada) throws SQLException {
        String orden = masAcertada ? "DESC" : "ASC";
        String sql =
            "SELECT p.pregunta, AVG(r.correcta) tasa " +
            "FROM trivia_respuestas r JOIN preguntas p ON p.id_pregunta = r.id_pregunta " +
            "GROUP BY r.id_pregunta, p.pregunta ORDER BY tasa " + orden + ", COUNT(*) DESC LIMIT 1";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("pregunta", rs.getString("pregunta"));
                m.put("tasa_acierto", (int) Math.round(rs.getDouble("tasa") * 100));
                return m;
            }
        }
        return null;
    }

    /** Respuestas de trivia por mes en los últimos 6 meses (para el histograma). */
    private List<Map<String, Object>> participacionMensual(Connection conn) throws SQLException {
        Map<String, Integer> porMes = new HashMap<>();
        String sql = "SELECT DATE_FORMAT(fecha, '%Y-%m') ym, COUNT(*) t FROM trivia_respuestas " +
                     "WHERE fecha >= (CURDATE() - INTERVAL 5 MONTH) GROUP BY ym";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) porMes.put(rs.getString("ym"), rs.getInt("t"));
        }
        List<Map<String, Object>> meses = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate mes = hoy.minusMonths(i);
            String clave = String.format("%04d-%02d", mes.getYear(), mes.getMonthValue());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("etiqueta", mes.getMonth().getDisplayName(TextStyle.SHORT, ES));
            item.put("total", porMes.getOrDefault(clave, 0));
            meses.add(item);
        }
        return meses;
    }

    /** Gasto total de TODOS los usuarios por día, últimos 7 días. */
    private List<Map<String, Object>> gastosPorDiaGlobal(Connection conn) throws SQLException {
        Map<String, Double> porFecha = new HashMap<>();
        String sql = "SELECT DATE(fecha) d, SUM(monto) t FROM movimientos " +
                     "WHERE tipo_movimiento = 'GASTO' AND fecha >= (CURDATE() - INTERVAL 6 DAY) GROUP BY DATE(fecha)";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) porFecha.put(rs.getDate("d").toLocalDate().toString(), rs.getDouble("t"));
        }
        List<Map<String, Object>> dias = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("etiqueta", dia.getDayOfWeek().getDisplayName(TextStyle.NARROW, ES).toUpperCase());
            item.put("total", porFecha.getOrDefault(dia.toString(), 0.0));
            dias.add(item);
        }
        return dias;
    }
}
