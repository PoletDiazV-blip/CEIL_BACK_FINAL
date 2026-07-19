package ceil.dao;

import ceil.database.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardDaoImpl implements DashboardDao {

    /**
     * El saldo es CALCULADO, no una columna.
     *
     *   saldo = ingreso_total + Σ ingresos − Σ gastos − Σ abonos a apartados
     *
     * Antes esto devolvía `ingreso_total` a pelo y ningún movimiento lo tocaba
     * jamás, así que la app nunca restaba nada. Al calcularlo en cada consulta es
     * imposible que quede desincronizado, y borrar un movimiento lo recompone solo.
     */
    @Override
    public double obtenerSaldoActual(int idUsuario) {
        String sql =
            "SELECT u.ingreso_total " +
            "     + COALESCE((SELECT SUM(m.monto) FROM movimientos m " +
            "                  WHERE m.id_usuario = u.id_usuario AND m.tipo_movimiento = 'INGRESO'), 0) " +
            "     - COALESCE((SELECT SUM(m.monto) FROM movimientos m " +
            "                  WHERE m.id_usuario = u.id_usuario AND m.tipo_movimiento = 'GASTO'), 0) " +
            "     - COALESCE((SELECT SUM(g.monto_actual) FROM metas_ahorro g " +
            "                  WHERE g.id_usuario = u.id_usuario), 0) AS saldo " +
            "FROM usuarios u WHERE u.id_usuario = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getDouble("saldo");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public double obtenerGastosDelMes(int idUsuario) {
        String sql = "SELECT COALESCE(SUM(monto), 0) AS total_gastos FROM movimientos " +
                "WHERE id_usuario = ? AND tipo_movimiento = 'GASTO' " +
                "AND MONTH(fecha) = MONTH(NOW()) AND YEAR(fecha) = YEAR(NOW())";
        return unDouble(sql, idUsuario, "total_gastos");
    }

    @Override
    public double obtenerAhorroTotal(int idUsuario) {
        String sql = "SELECT COALESCE(SUM(monto_actual), 0) AS ahorro FROM metas_ahorro WHERE id_usuario = ?";
        return unDouble(sql, idUsuario, "ahorro");
    }

    /**
     * El techo de gastos: cuánto puede gastar al día sin quedarse corto.
     * `ingreso_total` y `periodo_dias` existían en la tabla y `SetupBudgetScreen` ya
     * pedía justo esos dos datos — lo único que faltaba era esta división.
     * Devuelve 0 si el usuario todavía no ha configurado su presupuesto.
     */
    @Override
    public double obtenerTechoDiario(int idUsuario) {
        String sql = "SELECT ingreso_total / NULLIF(periodo_dias, 0) AS techo " +
                     "FROM usuarios WHERE id_usuario = ?";
        return unDouble(sql, idUsuario, "techo");
    }

    private double unDouble(String sql, int idUsuario, String columna) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(columna);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public List<Map<String, Object>> obtenerMovimientos(int idUsuario) {
        List<Map<String, Object>> movimientos = new ArrayList<>();
        String sql = "SELECT id_movimiento, concepto, monto, tipo_movimiento, categoria_nombre, " +
                "id_categoria, descripcion, fecha " +
                "FROM movimientos WHERE id_usuario = ? ORDER BY fecha DESC LIMIT 10";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> mov = new HashMap<>();
                    mov.put("id_movimiento", rs.getInt("id_movimiento"));
                    mov.put("concepto", rs.getString("concepto"));
                    mov.put("monto", rs.getDouble("monto"));
                    mov.put("tipo_movimiento", rs.getString("tipo_movimiento"));
                    mov.put("categoria_nombre", rs.getString("categoria_nombre"));
                    mov.put("id_categoria", rs.getObject("id_categoria"));
                    mov.put("descripcion", rs.getString("descripcion"));
                    mov.put("fecha", rs.getTimestamp("fecha").toString());
                    movimientos.add(mov);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movimientos;
    }

    @Override
    public Map<String, Object> obtenerMetaMasCercana(int idUsuario) {
        String sql = "SELECT id_meta, nombre_meta, monto_objetivo, monto_actual, fecha_limite " +
                     "FROM metas_ahorro WHERE id_usuario = ? AND estado <> 'COMPLETADA' " +
                     "ORDER BY fecha_limite IS NULL, fecha_limite ASC LIMIT 1";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("id_meta", rs.getInt("id_meta"));
                    meta.put("nombre_meta", rs.getString("nombre_meta"));
                    meta.put("monto_objetivo", rs.getDouble("monto_objetivo"));
                    meta.put("monto_actual", rs.getDouble("monto_actual"));
                    Date limite = rs.getDate("fecha_limite");
                    meta.put("fecha_limite", limite != null ? limite.toString() : null);
                    return meta;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
