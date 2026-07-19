package ceil.dao;

import ceil.model.Pregunta; // ✨ Importación del modelo
import java.util.List;

public interface PreguntaDao {
    List<Pregunta> obtenerPreguntasPorCategoria(String categoria); //  Ahora devuelve objetos Pregunta
    List<Pregunta> obtenerPreguntasAleatorias(int limite);         //  Ahora devuelve objetos Pregunta
}