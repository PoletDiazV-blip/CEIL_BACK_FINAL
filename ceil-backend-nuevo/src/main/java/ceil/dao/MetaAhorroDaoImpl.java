package ceil.dao;

import ceil.database.DatabaseConfig;
import ceil.model.MetaAhorro; // ✨ Importamos tu nuevo objeto real
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MetaAhorroDaoImpl implements MetaAhorroDao {

    @Override
    public boolean crearMeta(int idUsuario, String nombreMeta, double montoObjetivo, java.sql.Date fechaLimite) {
        String sql = "INSERT INTO metas_ahorro (id_usuario, nombre_meta, monto_objetivo, monto_actual, fecha_limite) VALUES (?, ?, ?, 0.0, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setString(2, nombreMeta);
            stmt.setDouble(3, montoObjetivo);
            stmt.setDate(4, fechaLimite);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<MetaAhorro> obtenerMetas(int idUsuario) {
        List<MetaAhorro> metas = new ArrayList<>();
        String sql = "SELECT id_meta, nombre_meta, monto_objetivo, monto_actual, fecha_limite FROM metas_ahorro WHERE id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // 🚀 Creamos e instanciamos el objeto real mapeando cada columna
                    MetaAhorro meta = new MetaAhorro();
                    meta.setIdMeta(rs.getInt("id_meta"));
                    meta.setIdUsuario(idUsuario);
                    meta.setNombreMeta(rs.getString("nombre_meta"));
                    meta.setMontoObjetivo(rs.getDouble("monto_objetivo"));
                    meta.setMontoActual(rs.getDouble("monto_actual"));
                    meta.setFechaLimite(rs.getDate("fecha_limite"));

                    metas.add(meta);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return metas;
    }

    @Override
    public boolean editarMeta(int idMeta, int idUsuario, String nombreMeta, double montoObjetivo, java.sql.Date fechaLimite) {
        String sql = "UPDATE metas_ahorro SET nombre_meta = ?, monto_objetivo = ?, fecha_limite = ? WHERE id_meta = ? AND id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nombreMeta);
            stmt.setDouble(2, montoObjetivo);
            stmt.setDate(3, fechaLimite);
            stmt.setInt(4, idMeta);
            stmt.setInt(5, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean eliminarMeta(int idMeta, int idUsuario) {
        String sql = "DELETE FROM metas_ahorro WHERE id_meta = ? AND id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMeta);
            stmt.setInt(2, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean abonarAMeta(int idMeta, int idUsuario, double montoAbono) {
        // LEAST pone el tope: `monto_actual` nunca puede pasarse de `monto_objetivo`.
        // El `AND id_usuario` impide abonar a una meta que no es tuya.
        String sql = "UPDATE metas_ahorro SET monto_actual = LEAST(monto_actual + ?, monto_objetivo) WHERE id_meta = ? AND id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, montoAbono);
            stmt.setInt(2, idMeta);
            stmt.setInt(3, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}