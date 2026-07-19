package ceil.controller;

import ceil.service.MovimientoService;
import ceil.model.Movimiento;
import ceil.security.Sesion;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class MovimientoController {
    private final MovimientoService movimientoService = new MovimientoService();

    @SuppressWarnings("unchecked")
    public void registrarMovimiento(Context ctx) {
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);
        // Fase 7: el dueño del movimiento es quien manda el token, no el id_usuario
        // que venga en el cuerpo (que un cliente podría falsear).
        datos.put("id_usuario", Sesion.uid(ctx));
        Map<String, Object> resultado = movimientoService.registrar(datos);

        if (Boolean.TRUE.equals(resultado.get("ok"))) {
            ctx.status(201).json(Map.of(
                    "mensaje", "Movimiento guardado correctamente",
                    "id_movimiento", resultado.get("id_movimiento"),
                    // El front usa esta bandera para lanzar "¿lo necesitas o lo quieres?"
                    "categoria_sensible", resultado.get("categoria_sensible")
            ));
        } else {
            ctx.status(400).json(Map.of("error", resultado.get("error")));
        }
    }

    public void obtenerHistorial(Context ctx) {
        int idUsuario = Sesion.uid(ctx); // Fase 7: identidad desde el token.
        List<Movimiento> historial = movimientoService.verHistorialUsuario(idUsuario);
        ctx.status(200).json(historial);
    }

    /**
     * El brief exige poder eliminar movimientos y no existía ningún DELETE en todo
     * el backend. Se pide el id_usuario para no borrar el movimiento de otra
     * persona; cuando haya sesión real (JWT) saldrá del token, no del query param.
     */
    public void eliminarMovimiento(Context ctx) {
        int idMovimiento = Integer.parseInt(ctx.pathParam("id"));
        // Fase 7: el dueño sale del token. El DELETE ya filtra por (id_movimiento,
        // id_usuario), así que solo se puede borrar lo propio.
        int idUsuario = Sesion.uid(ctx);
        if (movimientoService.eliminar(idMovimiento, idUsuario)) {
            ctx.status(200).json(Map.of("mensaje", "Movimiento eliminado"));
        } else {
            ctx.status(404).json(Map.of("error", "No existe ese movimiento para este usuario."));
        }
    }
}
