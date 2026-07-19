package ceil.service;

import ceil.dao.UsuarioDao;
import ceil.dao.UsuarioDaoImpl;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.util.Map;

public class UsuarioService {
    private final UsuarioDao usuarioDao = new UsuarioDaoImpl();

    /** Devuelve el id del usuario creado, o -1. El onboarding lo necesita enseguida. */
    public int registrarNuevoEstudiante(Map<String, Object> datos) {
        String username = (String) datos.get("username");
        String contacto = (String) datos.get("contacto");
        String password = (String) datos.get("password");

        if (username == null || username.trim().isEmpty()) return -1;
        if (contacto == null || contacto.trim().isEmpty()) return -1;
        if (password == null || password.trim().isEmpty()) return -1;

        String passwordEncriptado = BCrypt.hashpw(password, BCrypt.gensalt());
        return usuarioDao.registrarUsuario(username.trim(), contacto.trim(), passwordEncriptado);
    }

    public Map<String, Object> autenticarEstudiante(String contacto, String passwordIngresado) {
        Map<String, Object> usuario = usuarioDao.buscarUsuarioPorContacto(contacto);
        if (usuario == null) return null;

        String hashBD = (String) usuario.get("password");
        if (BCrypt.checkpw(passwordIngresado, hashBD)) {
            usuario.remove("password");
            return usuario;
        }
        return null;
    }

    public Map<String, Object> obtenerPerfil(int idUsuario) {
        return usuarioDao.obtenerPerfil(idUsuario);
    }

    /**
     * Convierte a días el periodo que capturó el usuario. El techo de gastos siempre
     * se calcula sobre días, capture él días, semanas o meses (el brief pide los tres).
     */
    private int aDias(int cantidad, String tipo) {
        return switch (tipo) {
            case "DIA" -> cantidad;
            case "SEMANA" -> cantidad * 7;
            case "MES" -> cantidad * 30;
            default -> -1;
        };
    }

    /**
     * Persiste el presupuesto del onboarding. Hasta ahora `SetupBudgetScreen` pedía
     * el monto y las semanas, y el NavGraph tiraba ambos valores: no existía ningún
     * endpoint que escribiera estas columnas.
     *
     * @return null si todo fue bien, o el mensaje de error.
     */
    public String guardarPresupuesto(int idUsuario, Map<String, Object> datos) {
        Object montoCrudo = datos.get("ingreso_total");
        Object cantidadCrudo = datos.get("periodo_cantidad");
        String tipo = (String) datos.get("periodo_tipo");

        if (!(montoCrudo instanceof Number) || !(cantidadCrudo instanceof Number)) {
            return "`ingreso_total` y `periodo_cantidad` son obligatorios y deben ser numéricos.";
        }
        if (tipo == null || tipo.isBlank()) return "Falta `periodo_tipo` (DIA, SEMANA o MES).";

        BigDecimal ingresoTotal = BigDecimal.valueOf(((Number) montoCrudo).doubleValue());
        if (ingresoTotal.signum() <= 0) return "El monto debe ser mayor que 0.";

        int cantidad = ((Number) cantidadCrudo).intValue();
        if (cantidad <= 0) return "El periodo debe ser mayor que 0.";

        String tipoNormalizado = tipo.trim().toUpperCase();
        int dias = aDias(cantidad, tipoNormalizado);
        if (dias < 0) return "`periodo_tipo` debe ser DIA, SEMANA o MES.";

        return usuarioDao.guardarPresupuesto(idUsuario, ingresoTotal, dias, tipoNormalizado)
                ? null : "No se pudo guardar el presupuesto.";
    }

    /**
     * El consentimiento de privacidad. El usuario lo aceptaba en la app y no se
     * guardaba en ninguna parte: es un problema legal, no solo técnico.
     */
    public boolean guardarConsentimiento(int idUsuario, boolean acepto) {
        return usuarioDao.guardarConsentimiento(idUsuario, acepto);
    }

    /** Sumar o restar del monto total a mano, como pide el brief. */
    public boolean ajustarSaldo(int idUsuario, BigDecimal delta) {
        return usuarioDao.ajustarIngresoTotal(idUsuario, delta);
    }

    public boolean actualizarDatosPersonales(int idUsuario, Integer edad, String sexo) {
        return usuarioDao.actualizarDatosPersonales(idUsuario, edad, sexo);
    }
}
