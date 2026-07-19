package ceil.controller;

import io.javalin.http.Context;
import ceil.service.PreguntaService;
import ceil.model.Pregunta;
import java.util.List;

public class PreguntasController {
    private final PreguntaService preguntaService = new PreguntaService();

    public void listarPreguntas(Context ctx) {
        String categoria = ctx.queryParam("categoria");
        List<Pregunta> preguntas = preguntaService.listarPorCategoria(categoria);
        ctx.status(200).json(preguntas);
    }

    public void obtenerExpress(Context ctx) {
        List<Pregunta> preguntas = preguntaService.obtenerTriviaExpress();
        ctx.status(200).json(preguntas);
    }
}