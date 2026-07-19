package ceil.dao;

import ceil.database.DatabaseConfig;
import ceil.model.Pregunta; //  Importación del modelo
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PreguntaDaoImpl implements PreguntaDao {

    @Override
    public List<Pregunta> obtenerPreguntasPorCategoria(String categoria) {
        List<Pregunta> lista = new ArrayList<>();
        String sql = "SELECT id_pregunta, pregunta, opcion_a, opcion_b, opcion_c, opcion_correcta, puntos, retroalimentacion FROM preguntas WHERE categoria = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, categoria);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearPregunta(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public List<Pregunta> obtenerPreguntasAleatorias(int limite) {
        List<Pregunta> lista = new ArrayList<>();
        String sql = "SELECT id_pregunta, pregunta, opcion_a, opcion_b, opcion_c, opcion_correcta, puntos, retroalimentacion FROM preguntas ORDER BY RAND() LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limite);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearPregunta(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    //  Helper para mapear el ResultSet directamente a un Objeto del Modelo Real
    private Pregunta mapearPregunta(ResultSet rs) throws SQLException {
        Pregunta p = new Pregunta();
        p.setIdPregunta(rs.getInt("id_pregunta"));
        p.setPregunta(rs.getString("pregunta"));
        p.setOpcionA(rs.getString("opcion_a"));
        p.setOpcionB(rs.getString("opcion_b"));
        p.setOpcionC(rs.getString("opcion_c"));
        p.setOpcionCorrecta(rs.getString("opcion_correcta"));
        p.setPuntos(rs.getInt("puntos"));
        p.setRetroalimentacion(rs.getString("retroalimentacion"));
        return p;
    }
}