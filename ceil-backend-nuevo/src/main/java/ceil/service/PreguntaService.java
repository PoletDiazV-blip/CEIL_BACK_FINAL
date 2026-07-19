package ceil.service;

import ceil.dao.PreguntaDao;
import ceil.dao.PreguntaDaoImpl;
import ceil.model.Pregunta; // ✨ Importamos el modelo real
import java.util.List;

public class PreguntaService {
    private final PreguntaDao preguntaDao = new PreguntaDaoImpl();

    // ✨ Cambiado de List<Map<...>> a List<Pregunta>
    public List<Pregunta> listarPorCategoria(String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) {
            return preguntaDao.obtenerPreguntasAleatorias(5);
        }
        return preguntaDao.obtenerPreguntasPorCategoria(categoria);
    }

    // ✨ Cambiado de List<Map<...>> a List<Pregunta>
    public List<Pregunta> obtenerTriviaExpress() {
        return preguntaDao.obtenerPreguntasAleatorias(3);
    }
}