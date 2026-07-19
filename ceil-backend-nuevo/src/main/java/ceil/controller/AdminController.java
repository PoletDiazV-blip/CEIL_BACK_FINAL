package ceil.controller;

import io.javalin.http.Context;
import ceil.service.AdminService;

public class AdminController {
    private final AdminService adminService = new AdminService();

    public void obtenerDashboardGlobal(Context ctx) {
        ctx.status(200).json(adminService.verEstadisticasGlobales());
    }

    public void listarUsuariosRendimiento(Context ctx) {
        ctx.status(200).json(adminService.verControlAlumnos());
    }

    public void obtenerAnaliticaQuiz(Context ctx) {
        ctx.status(200).json(adminService.verAnaliticaQuiz());
    }

    public void obtenerAnaliticaApartados(Context ctx) {
        ctx.status(200).json(adminService.verAnaliticaApartados());
    }
}
