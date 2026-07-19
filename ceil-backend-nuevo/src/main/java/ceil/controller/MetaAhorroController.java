package ceil.controller;

import io.javalin.http.Context;
import ceil.service.MetaAhorroService;
import ceil.service.MedallaService;
import ceil.security.Sesion;
import ceil.model.MetaAhorro;
import java.util.Map;
import java.util.List;

public class MetaAhorroController {
    private final MetaAhorroService metaService = new MetaAhorroService();

    public void crearMeta(Context ctx) {
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);
        int idUsuario = Sesion.uid(ctx); // Fase 7: dueño desde el token.
        String nombreMeta = (String) datos.get("nombre_meta");
        double montoObjetivo = ((Number) datos.get("monto_objetivo")).doubleValue();
        String fechaLimite = (String) datos.get("fecha_limite");

        boolean exito = metaService.registrarNuevaMeta(idUsuario, nombreMeta, montoObjetivo, fechaLimite);
        if (exito) {
            ctx.status(201).json(Map.of("mensaje", "¡Meta de ahorro añadida con éxito a CEIL!"));
        } else {
            ctx.status(400).json(Map.of("error", "Datos inválidos para crear la meta."));
        }
    }

    public void listarMetas(Context ctx) {
        int idUsuario = Sesion.uid(ctx); // Fase 7: identidad desde el token.
        List<MetaAhorro> metas = metaService.listarMetasUsuario(idUsuario);
        ctx.status(200).json(metas);
    }

    public void abonarMeta(Context ctx) {
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);
        int idMeta = ((Number) datos.get("id_meta")).intValue();
        int idUsuario = Sesion.uid(ctx); // Fase 7: dueño desde el token.
        double monto = ((Number) datos.get("monto")).doubleValue();

        boolean exito = metaService.agregarProgresoAhorro(idMeta, idUsuario, monto);
        if (exito) {
            // Fase 6 — disparo de medallas de ahorro: "Primer Ahorro" (cualquier abono)
            // y "Meta Cumplida" (si este abono llevó la meta al 100%, que además la marca
            // como COMPLETADA para el panel admin).
            MedallaService.evaluarAhorro(idUsuario);
            MedallaService.evaluarMetaCumplida(idMeta, idUsuario);
            ctx.status(200).json(Map.of("mensaje", "Abono realizado con éxito"));
        } else {
            ctx.status(400).json(Map.of("error", "No se pudo procesar el abono"));
        }
    }

    public void editarMeta(Context ctx) {
        int idMeta = Integer.parseInt(ctx.pathParam("id"));
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);
        int idUsuario = Sesion.uid(ctx); // Fase 7: la meta se edita solo si es del token (WHERE id_usuario).
        String nombreMeta = (String) datos.get("nombre_meta");
        double montoObjetivo = ((Number) datos.get("monto_objetivo")).doubleValue();
        String fechaLimite = (String) datos.get("fecha_limite");

        boolean exito = metaService.editarMeta(idUsuario, idMeta, nombreMeta, montoObjetivo, fechaLimite);
        if (exito) {
            ctx.status(200).json(Map.of("mensaje", "Meta actualizada con éxito"));
        } else {
            ctx.status(400).json(Map.of("error", "No se pudo actualizar la meta"));
        }
    }

    public void eliminarMeta(Context ctx) {
        int idMeta = Integer.parseInt(ctx.pathParam("id"));
        int idUsuario = Sesion.uid(ctx); // Fase 7: solo se borra si la meta es del token.

        boolean exito = metaService.eliminarMeta(idUsuario, idMeta);
        if (exito) {
            ctx.status(200).json(Map.of("mensaje", "Meta eliminada con éxito"));
        } else {
            ctx.status(400).json(Map.of("error", "No se pudo eliminar la meta"));
        }
    }
}