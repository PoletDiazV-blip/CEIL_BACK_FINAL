package ceil.dao;

import java.util.List;
import java.util.Map;

public interface DashboardDao {
    /** Calculado: ingreso_total + Σingresos − Σgastos − Σabonos. No es una columna. */
    double obtenerSaldoActual(int idUsuario);

    double obtenerGastosDelMes(int idUsuario);

    double obtenerAhorroTotal(int idUsuario);

    /** ingreso_total / periodo_dias. 0 si aún no configuró su presupuesto. */
    double obtenerTechoDiario(int idUsuario);

    List<Map<String, Object>> obtenerMovimientos(int idUsuario);

    Map<String, Object> obtenerMetaMasCercana(int idUsuario);
}
