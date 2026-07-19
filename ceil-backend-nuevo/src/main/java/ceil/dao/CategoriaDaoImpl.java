package ceil.dao;

import ceil.database.DatabaseConfig;
import ceil.model.Categoria;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDaoImpl implements CategoriaDao {

    private static final String COLUMNAS =
            "id_categoria, nombre, emoji, color_hex, es_sensible";

    @Override
    public List<Categoria> listarTodas() {
        String sql = "SELECT " + COLUMNAS + " FROM categorias ORDER BY id_categoria";
        List<Categoria> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public Categoria buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;

        // El front venía mandando la categoría con el emoji pegado ("🎮 Juegos"),
        // así que se acepta tanto el nombre exacto como el nombre precedido de
        // cualquier prefijo. Ordenar por longitud descendente evita que un nombre
        // corto le gane a uno largo si alguna vez uno es sufijo del otro.
        String sql = "SELECT " + COLUMNAS + " FROM categorias " +
                     "WHERE nombre = ? OR ? LIKE CONCAT('%', nombre) " +
                     "ORDER BY nombre = ? DESC, CHAR_LENGTH(nombre) DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String limpio = nombre.trim();
            stmt.setString(1, limpio);
            stmt.setString(2, limpio);
            stmt.setString(3, limpio);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Categoria> listarDeUsuario(int idUsuario) {
        String sql = "SELECT c.id_categoria, c.nombre, c.emoji, c.color_hex, c.es_sensible " +
                     "FROM categorias c " +
                     "JOIN usuario_categorias uc ON uc.id_categoria = c.id_categoria " +
                     "WHERE uc.id_usuario = ? ORDER BY c.id_categoria";
        List<Categoria> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public boolean guardarDeUsuario(int idUsuario, List<Integer> idsCategorias) {
        // Borrar + insertar en una transacción: si el insert falla, el usuario no
        // se queda sin ninguna categoría.
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM usuario_categorias WHERE id_usuario = ?")) {
                del.setInt(1, idUsuario);
                del.executeUpdate();
            }

            if (!idsCategorias.isEmpty()) {
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO usuario_categorias (id_usuario, id_categoria) VALUES (?, ?)")) {
                    for (Integer idCategoria : idsCategorias) {
                        ins.setInt(1, idUsuario);
                        ins.setInt(2, idCategoria);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        return new Categoria(
                rs.getInt("id_categoria"),
                rs.getString("nombre"),
                rs.getString("emoji"),
                rs.getString("color_hex"),
                rs.getBoolean("es_sensible")
        );
    }
}
