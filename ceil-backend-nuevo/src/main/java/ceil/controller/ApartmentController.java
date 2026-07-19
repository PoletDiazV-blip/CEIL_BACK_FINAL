package ceil.controller;

import ceil.service.MovimientoService;
import ceil.security.Sesion;
import io.javalin.http.Context;

public class ApartmentController {
    private static final MovimientoService movimientoService = new MovimientoService();

    /**
     * Devuelve los movimientos del usuario. Antes hacía `SELECT * FROM movimientos`
     * sin WHERE, así que devolvía los movimientos de todo el mundo. Fase 7: el usuario
     * sale del token, no de un `?id_usuario` que se pudiera cambiar a mano.
     */
    public static void getApartments(Context ctx) {
        ctx.status(200).json(movimientoService.verHistorialUsuario(Sesion.uid(ctx)));
    }
}
