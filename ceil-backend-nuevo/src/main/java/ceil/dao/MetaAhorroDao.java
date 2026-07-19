package ceil.dao;

import ceil.model.MetaAhorro; //  Importamos nuestro modelo real
import java.util.List;

public interface MetaAhorroDao {
    boolean crearMeta(int idUsuario, String nombreMeta, double montoObjetivo, java.sql.Date fechaLimite);
    List<MetaAhorro> obtenerMetas(int idUsuario); //  Ahora devuelve objetos MetaAhorro reales

    // El `AND id_usuario` de cada consulta es lo que da la propiedad: sólo el dueño
    // de la meta puede editarla, borrarla o abonarle.
    boolean editarMeta(int idMeta, int idUsuario, String nombreMeta, double montoObjetivo, java.sql.Date fechaLimite);
    boolean eliminarMeta(int idMeta, int idUsuario);
    boolean abonarAMeta(int idMeta, int idUsuario, double montoAbono);
}
