package ceil.dao;

import ceil.database.DatabaseConfig;
import ceil.model.Movimiento;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovimientoDaoImpl implements MovimientoDao {

    private static final String COLUMNAS =
            "id_movimiento, id_usuario, monto, tipo_movimiento, concepto, " +
            "categoria_nombre, id_categoria, prioridad, descripcion, fecha";

    @Override
    public int registrarMovimiento(Movimiento m) {
        String sql = "INSERT INTO movimientos " +
                "(id_usuario, monto, tipo_movimiento, concepto, categoria_nombre, id_categoria, prioridad, descripcion, fecha) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, m.getId_usuario());
            stmt.setBigDecimal(2, m.getMonto());
            stmt.setString(3, m.getTipo_movimiento());
            stmt.setString(4, m.getConcepto());
            stmt.setString(5, m.getCategoria_nombre());
            if (m.getId_categoria() == null) {
                stmt.setNull(6, Types.INTEGER);
            } else {
                stmt.setInt(6, m.getId_categoria());
            }
            stmt.setString(7, m.getPrioridad());
            stmt.setString(8, m.getDescripcion());

            if (stmt.executeUpdate() == 0) return -1;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public List<Movimiento> obtenerHistorial(int idUsuario) {
        String sql = "SELECT " + COLUMNAS + " FROM movimientos WHERE id_usuario = ? ORDER BY fecha DESC";
        List<Movimiento> historial = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) historial.add(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return historial;
    }

    @Override
    public boolean eliminarMovimiento(int idMovimiento, int idUsuario) {
        // El id_usuario va dentro del WHERE, no en una comprobación aparte: así es
        // imposible borrar el movimiento de otra persona aunque se adivine el id.
        String sql = "DELETE FROM movimientos WHERE id_movimiento = ? AND id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMovimiento);
            stmt.setInt(2, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Movimiento mapear(ResultSet rs) throws SQLException {
        int idCategoria = rs.getInt("id_categoria");
        boolean sinCategoria = rs.wasNull();
        return new Movimiento(
                rs.getInt("id_movimiento"),
                rs.getInt("id_usuario"),
                rs.getBigDecimal("monto"),
                rs.getString("tipo_movimiento"),
                rs.getString("concepto"),
                rs.getString("categoria_nombre"),
                sinCategoria ? null : idCategoria,
                rs.getString("prioridad"),
                rs.getString("descripcion"),
                rs.getTimestamp("fecha")
        );
    }
}
