package ceil.controller;

import ceil.service.UsuarioService;
import ceil.security.JwtUtil;
import ceil.security.Sesion;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.Map;

public class UsuarioController {
    private final UsuarioService usuarioService = new UsuarioService();

    @SuppressWarnings("unchecked")
    public void registrar(Context ctx) {
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);
        int idUsuario = usuarioService.registrarNuevoEstudiante(datos);

        if (idUsuario > 0) {
            // El id se devuelve para que el onboarding (privacidad → categorías →
            // presupuesto) pueda escribir sobre el usuario recién creado sin tener
            // que hacer login otra vez. El token deja al usuario ya autenticado para
            // esas llamadas del onboarding (rol USER por defecto).
            ctx.status(201).json(Map.of(
                    "status", "success",
                    "mensaje", "Registro exitoso. ¡Bienvenido a CEIL!",
                    "id_usuario", idUsuario,
                    "token", JwtUtil.generar(idUsuario, "USER")
            ));
        } else {
            ctx.status(400).json(Map.of(
                    "status", "error",
                    "error", "Error al registrar. Puede que el correo o el usuario ya existan."
            ));
        }
    }

    @SuppressWarnings("unchecked")
    public void login(Context ctx) {
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);
        String contacto = (String) datos.get("contacto");
        String password = (String) datos.get("password");

        Map<String, Object> usuario = usuarioService.autenticarEstudiante(contacto, password);

        if (usuario != null) {
            int uid = ((Number) usuario.get("id_usuario")).intValue();
            String rol = (String) usuario.get("rol");
            ctx.status(200).json(Map.of(
                    "status", "success",
                    "mensaje", "¡Bienvenido a CEIL!",
                    "usuario", usuario,
                    "token", JwtUtil.generar(uid, rol)
            ));
        } else {
            ctx.status(401).json(Map.of(
                    "status", "error",
                    "error", "Credenciales incorrectas."
            ));
        }
    }

    public void obtenerPerfil(Context ctx) {
        int idUsuario = Sesion.uid(ctx);
        Map<String, Object> perfil = usuarioService.obtenerPerfil(idUsuario);

        if (perfil == null) {
            ctx.status(404).json(Map.of("error", "Usuario no encontrado."));
            return;
        }
        ctx.status(200).json(perfil);
    }

    @SuppressWarnings("unchecked")
    public void guardarPresupuesto(Context ctx) {
        int idUsuario = Sesion.uid(ctx);
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);

        String error = usuarioService.guardarPresupuesto(idUsuario, datos);
        if (error != null) {
            ctx.status(400).json(Map.of("error", error));
            return;
        }

        // Devuelve el perfil ya actualizado, para que el front pueda pintar el techo
        // de gastos sin tener que pedir el dashboard otra vez.
        ctx.status(200).json(usuarioService.obtenerPerfil(idUsuario));
    }

    @SuppressWarnings("unchecked")
    public void guardarConsentimiento(Context ctx) {
        int idUsuario = Sesion.uid(ctx);
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);

        Object acepto = datos.get("acepto_privacidad");
        if (!(acepto instanceof Boolean)) {
            ctx.status(400).json(Map.of("error", "`acepto_privacidad` debe ser true o false."));
            return;
        }

        if (usuarioService.guardarConsentimiento(idUsuario, (Boolean) acepto)) {
            ctx.status(200).json(Map.of("mensaje", "Consentimiento registrado", "acepto_privacidad", acepto));
        } else {
            ctx.status(404).json(Map.of("error", "Usuario no encontrado."));
        }
    }

    /** Sumar o restar del monto total a mano. El brief lo pide explícitamente. */
    @SuppressWarnings("unchecked")
    public void ajustarSaldo(Context ctx) {
        int idUsuario = Sesion.uid(ctx);
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);

        Object montoCrudo = datos.get("monto");
        if (!(montoCrudo instanceof Number)) {
            ctx.status(400).json(Map.of("error", "`monto` es obligatorio (negativo para restar)."));
            return;
        }

        BigDecimal delta = BigDecimal.valueOf(((Number) montoCrudo).doubleValue());
        if (usuarioService.ajustarSaldo(idUsuario, delta)) {
            ctx.status(200).json(usuarioService.obtenerPerfil(idUsuario));
        } else {
            ctx.status(404).json(Map.of("error", "Usuario no encontrado."));
        }
    }

    @SuppressWarnings("unchecked")
    public void actualizarDatosPersonales(Context ctx) {
        int idUsuario = Sesion.uid(ctx);
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);

        Object edadCruda = datos.get("edad");
        Integer edad = (edadCruda instanceof Number) ? ((Number) edadCruda).intValue() : null;
        String sexo = (String) datos.get("sexo");

        if (usuarioService.actualizarDatosPersonales(idUsuario, edad, sexo)) {
            ctx.status(200).json(usuarioService.obtenerPerfil(idUsuario));
        } else {
            ctx.status(404).json(Map.of("error", "Usuario no encontrado."));
        }
    }
}
