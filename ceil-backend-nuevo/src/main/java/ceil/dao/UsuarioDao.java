package ceil.dao;

import java.math.BigDecimal;
import java.util.Map;

public interface UsuarioDao {
    /** Devuelve el id del usuario creado, o -1 si falló. El onboarding lo necesita. */
    int registrarUsuario(String username, String contacto, String password);

    Map<String, Object> buscarUsuarioPorContacto(String contacto);

    Map<String, Object> obtenerPerfil(int idUsuario);

    /** Persiste el presupuesto del onboarding. No había nada que escribiera estas columnas. */
    boolean guardarPresupuesto(int idUsuario, BigDecimal ingresoTotal, int periodoDias, String periodoTipo);

    boolean guardarConsentimiento(int idUsuario, boolean acepto);

    /** Suma al monto total (o resta, si el delta es negativo), como pide el brief. */
    boolean ajustarIngresoTotal(int idUsuario, BigDecimal delta);

    boolean actualizarDatosPersonales(int idUsuario, Integer edad, String sexo);
}
