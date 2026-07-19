package ceil.controller;

import io.javalin.http.Context;
import ceil.service.DashboardService;
import ceil.security.Sesion;
import java.util.Map;

public class DashboardController {
    private final DashboardService dashboardService = new DashboardService();

    public void obtenerResumen(Context ctx) {
        // Identidad desde el token (Fase 7): el {id} de la ruta se ignora, así nadie
        // puede pedir el dashboard de otro usuario cambiando el número en la URL.
        int idUsuario = Sesion.uid(ctx);
        Map<String, Object> resumen = dashboardService.obtenerResumenDashboard(idUsuario);

        // Antes devolvía 200 con ceros para un usuario inexistente, que es
        // justo lo que enmascaraba el `usuarioId = 3` hardcodeado del front.
        if (resumen == null) {
            ctx.status(404).json(Map.of("error", "Usuario no encontrado."));
            return;
        }
        ctx.status(200).json(resumen);
    }

    // guardarMovimiento vivía aquí, duplicando a MovimientoController y con 'gasto'
    // escrito a mano en el SQL, lo que hacía imposible registrar un ingreso.
    // Ahora POST /api/movimientos apunta a MovimientoController::registrarMovimiento.
}