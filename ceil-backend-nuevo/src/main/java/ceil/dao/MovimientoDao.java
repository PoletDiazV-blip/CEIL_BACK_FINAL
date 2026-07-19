package ceil.dao;

import ceil.model.Movimiento;
import java.util.List;

public interface MovimientoDao {
    /** Devuelve el id generado, o -1 si falló. */
    int registrarMovimiento(Movimiento movimiento);

    List<Movimiento> obtenerHistorial(int idUsuario);

    /** El id_usuario se exige para que nadie borre un movimiento ajeno. */
    boolean eliminarMovimiento(int idMovimiento, int idUsuario);
}
