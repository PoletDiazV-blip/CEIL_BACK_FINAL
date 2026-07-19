package ceil.service;

import ceil.dao.AdminDao;
import ceil.dao.AdminDaoImpl;
import ceil.model.DashboardAdmin;
import ceil.model.AlumnoRendimiento;
import java.util.List;
import java.util.Map;

public class AdminService {
    private final AdminDao adminDao = new AdminDaoImpl();

    public DashboardAdmin verEstadisticasGlobales() {
        return adminDao.obtenerMetricasGlobales();
    }

    public List<AlumnoRendimiento> verControlAlumnos() {
        return adminDao.obtenerListadoUsuariosRendimiento();
    }

    public Map<String, Object> verAnaliticaQuiz() {
        return adminDao.obtenerAnaliticaQuiz();
    }

    public Map<String, Object> verAnaliticaApartados() {
        return adminDao.obtenerAnaliticaApartados();
    }
}
