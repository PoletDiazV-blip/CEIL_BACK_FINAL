package ceil.dao;

import ceil.model.DashboardAdmin;
import ceil.model.AlumnoRendimiento;
import java.util.List;
import java.util.Map;

public interface AdminDao {
    DashboardAdmin obtenerMetricasGlobales();
    List<AlumnoRendimiento> obtenerListadoUsuariosRendimiento();
    // Analíticas para las pantallas de detalle del panel admin (Fase 6). Devuelven
    // Map en vez de un modelo porque son agregados heterogéneos que solo lee el JSON.
    Map<String, Object> obtenerAnaliticaQuiz();
    Map<String, Object> obtenerAnaliticaApartados();
}
