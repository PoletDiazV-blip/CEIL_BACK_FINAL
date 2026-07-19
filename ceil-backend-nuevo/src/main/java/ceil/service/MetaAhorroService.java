package ceil.service;

import ceil.dao.MetaAhorroDao;
import ceil.dao.MetaAhorroDaoImpl;
import ceil.model.MetaAhorro; // ✨ Importamos el modelo real
import java.sql.Date;
import java.util.List;

public class MetaAhorroService {
    private final MetaAhorroDao metaDao = new MetaAhorroDaoImpl();

    public boolean registrarNuevaMeta(int idUsuario, String nombre, double objetivo, String fechaStr) {
        if (nombre == null || nombre.trim().isEmpty() || objetivo <= 0) return false;

        // La fecha límite es opcional (la columna admite NULL). Antes, un `fecha_limite`
        // ausente lanzaba NPE dentro de Date.valueOf y salía como un 500; y cualquier
        // formato distinto de yyyy-MM-dd tumbaba la petición.
        Date fechaLimite = null;
        if (fechaStr != null && !fechaStr.isBlank()) {
            try {
                fechaLimite = Date.valueOf(fechaStr.trim());
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return metaDao.crearMeta(idUsuario, nombre, objetivo, fechaLimite);
    }

    // ✨ Cambiado de List<Map<...>> a List<MetaAhorro>
    public List<MetaAhorro> listarMetasUsuario(int idUsuario) {
        return metaDao.obtenerMetas(idUsuario);
    }

    public boolean editarMeta(int idUsuario, int idMeta, String nombre, double objetivo, String fechaStr) {
        if (nombre == null || nombre.trim().isEmpty() || objetivo <= 0) return false;

        // Misma regla que al crear: la fecha límite es opcional y cualquier formato
        // distinto de yyyy-MM-dd invalida la petición en vez de reventar con un 500.
        Date fechaLimite = null;
        if (fechaStr != null && !fechaStr.isBlank()) {
            try {
                fechaLimite = Date.valueOf(fechaStr.trim());
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return metaDao.editarMeta(idMeta, idUsuario, nombre, objetivo, fechaLimite);
    }

    public boolean eliminarMeta(int idUsuario, int idMeta) {
        return metaDao.eliminarMeta(idMeta, idUsuario);
    }

    public boolean agregarProgresoAhorro(int idMeta, int idUsuario, double monto) {
        if (monto <= 0) return false;
        return metaDao.abonarAMeta(idMeta, idUsuario, monto);
    }
}