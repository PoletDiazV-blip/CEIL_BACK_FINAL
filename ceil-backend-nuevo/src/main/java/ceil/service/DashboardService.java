package ceil.service;

import ceil.dao.DashboardDao;
import ceil.dao.DashboardDaoImpl;
import ceil.dao.UsuarioDao;
import ceil.dao.UsuarioDaoImpl;
import java.util.HashMap;
import java.util.Map;

public class DashboardService {
    private final DashboardDao dashboardDao = new DashboardDaoImpl();
    private final UsuarioDao usuarioDao = new UsuarioDaoImpl();

    public Map<String, Object> obtenerResumenDashboard(int idUsuario) {
        Map<String, Object> resumen = new HashMap<>();

        Map<String, Object> perfil = usuarioDao.obtenerPerfil(idUsuario);
        if (perfil == null) return null;

        // `ingreso_total` es el presupuesto que configuró en el onboarding;
        // `saldo_disponible` es lo que le queda de verdad. Hasta ahora la app
        // enseñaba el primero como si fuera el segundo, sin restar nunca nada.
        resumen.put("ingreso_total", perfil.get("ingreso_total"));
        resumen.put("periodo_dias", perfil.get("periodo_dias"));
        resumen.put("periodo_tipo", perfil.get("periodo_tipo"));

        resumen.put("saldo_disponible", dashboardDao.obtenerSaldoActual(idUsuario));
        resumen.put("gastos_mes", dashboardDao.obtenerGastosDelMes(idUsuario));
        resumen.put("ahorro_total", dashboardDao.obtenerAhorroTotal(idUsuario));
        resumen.put("techo_diario", dashboardDao.obtenerTechoDiario(idUsuario));
        resumen.put("movimientos", dashboardDao.obtenerMovimientos(idUsuario));
        resumen.put("meta_mas_cercana", dashboardDao.obtenerMetaMasCercana(idUsuario));

        return resumen;
    }
}
