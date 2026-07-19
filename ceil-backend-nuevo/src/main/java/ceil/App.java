package ceil;

import ceil.controller.AdicionalesController;
import ceil.controller.AdminController;
import ceil.controller.ApartmentController;
import ceil.controller.ArbolDecisionesController;
import ceil.controller.CategoriaController;
import ceil.controller.DashboardController;
import ceil.controller.EstadisticasController;
import ceil.controller.MetaAhorroController;
import ceil.controller.MovimientoController;
import ceil.controller.PreguntasController;
import ceil.controller.UsuarioController;
import ceil.security.JwtUtil;
import ceil.security.Sesion;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.Javalin;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import java.util.Map;

public class App {

    // En macOS el puerto 7000 lo ocupa el receptor de AirPlay (ControlCenter), así
    // que el servidor no puede arrancar ahí. Se toma de CEIL_PORT si está definido.
    private static int puerto() {
        // Railway usa PORT, local usa CEIL_PORT, fallback 7000
        String valor = System.getenv("PORT");
        if (valor == null || valor.isBlank()) {
            valor = System.getenv("CEIL_PORT");
        }
        return (valor == null || valor.isBlank()) ? 7000 : Integer.parseInt(valor);
    }

    public static void main(String[] args) {
        System.out.println("Iniciando CEIL...");

        int puerto = puerto();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(JsonConfig.mapper());
            config.plugins.enableCors(cors -> {
                cors.add(it -> {
                    it.anyHost();
                });
            });
        }).start(puerto);

        System.out.println("Servidor en http://localhost:" + puerto);

        // ---- Filtro de autenticación (Fase 7) -------------------------------
        // Todo lo que cuelga de /api exige un JWT válido, salvo el registro y el login.
        // El token trae quién es el usuario; a partir de aquí los controladores leen la
        // identidad de `Sesion.uid(ctx)`, nunca del id que mande el cliente (eso cerraba
        // el IDOR: antes cualquiera pedía los datos de cualquier id).
        app.before(ctx -> {
            String metodo = ctx.req().getMethod();
            String ruta = ctx.path();

            boolean publico =
                    "OPTIONS".equalsIgnoreCase(metodo)          // preflight CORS
                    || ruta.equals("/")
                    || ruta.equals("/api/auth/login")
                    || ruta.equals("/api/auth/register");
            if (publico) return;

            String cabecera = ctx.header("Authorization");
            if (cabecera == null || !cabecera.startsWith("Bearer ")) {
                throw new UnauthorizedResponse("Falta el token de sesión.");
            }
            DecodedJWT jwt = JwtUtil.verificar(cabecera.substring(7).trim());
            if (jwt == null) {
                throw new UnauthorizedResponse("Sesión inválida o caducada. Inicia sesión de nuevo.");
            }

            // Identidad disponible para el resto de la petición.
            ctx.attribute(Sesion.ATTR_UID, jwt.getClaim("uid").asInt());
            ctx.attribute(Sesion.ATTR_ROL, jwt.getClaim("rol").asString());

            // El panel admin solo para administradores.
            if (ruta.startsWith("/api/admin") && !Sesion.esAdmin(ctx)) {
                throw new ForbiddenResponse("Se requiere rol de administrador.");
            }
        });

        // Los errores de auth se devuelven como JSON `{"error": ...}`, el formato que
        // el front ya sabe leer (en vez del cuerpo por defecto de Javalin).
        app.exception(UnauthorizedResponse.class, (e, ctx) ->
                ctx.status(401).json(Map.of("error", e.getMessage())));
        app.exception(ForbiddenResponse.class, (e, ctx) ->
                ctx.status(403).json(Map.of("error", e.getMessage())));

        app.get("/", ctx -> ctx.result("Servidor CEIL conectado correctamente"));

        // ---- Auth -----------------------------------------------------------
        UsuarioController usuarioController = new UsuarioController();
        app.post("/api/auth/register", usuarioController::registrar);
        app.post("/api/auth/login", usuarioController::login);

        // ---- Perfil y onboarding --------------------------------------------
        // La UI ya recogía estos datos; sencillamente no había ningún endpoint que
        // los escribiera, así que el NavGraph los tiraba.
        app.get("/api/usuarios/{id}", usuarioController::obtenerPerfil);
        app.post("/api/usuarios/{id}/presupuesto", usuarioController::guardarPresupuesto);
        app.post("/api/usuarios/{id}/privacidad", usuarioController::guardarConsentimiento);
        app.post("/api/usuarios/{id}/saldo", usuarioController::ajustarSaldo);
        app.put("/api/usuarios/{id}/datos", usuarioController::actualizarDatosPersonales);

        // ---- Dashboard ------------------------------------------------------
        DashboardController dashboardController = new DashboardController();
        app.get("/api/dashboard/resumen/{id}", dashboardController::obtenerResumen);

        // ---- Movimientos ----------------------------------------------------
        // POST /api/movimientos apuntaba a DashboardController::guardarMovimiento,
        // que forzaba tipo='gasto' en el SQL. Ahora va al controlador completo, que
        // acepta el tipo, la prioridad y la descripción.
        MovimientoController movimientoController = new MovimientoController();
        app.post("/api/movimientos", movimientoController::registrarMovimiento);
        app.get("/api/movimientos/{id}", movimientoController::obtenerHistorial);
        app.delete("/api/movimientos/{id}", movimientoController::eliminarMovimiento);

        // ---- Categorías -----------------------------------------------------
        CategoriaController categoriaController = new CategoriaController();
        app.get("/api/categorias", categoriaController::listarCatalogo);
        app.get("/api/usuarios/{id}/categorias", categoriaController::listarDeUsuario);
        app.post("/api/usuarios/{id}/categorias", categoriaController::guardarDeUsuario);

        // ---- Apartados / metas de ahorro ------------------------------------
        MetaAhorroController metaController = new MetaAhorroController();
        app.post("/api/metas", metaController::crearMeta);
        app.get("/api/metas/{id}", metaController::listarMetas);
        app.post("/api/metas/abonar", metaController::abonarMeta);
        app.put("/api/metas/{id}", metaController::editarMeta);
        app.delete("/api/metas/{id}", metaController::eliminarMeta);

        app.get("/api/apartments/list", ApartmentController::getApartments);

        // ---- Trivia ---------------------------------------------------------
        // Unificada en la Fase 5: una sola implementación (AdicionalesController) que
        // sirve las preguntas desde la tabla `preguntas` (fuente única) y valida la
        // respuesta en el servidor. Se eliminó TriviaController, cuyo /responder
        // confiaba en un booleano `es_correcta` mandado por el cliente (falsificable).
        //
        // Fase 6: la trivia pasa de "1 pregunta/día" a UNA SESIÓN al día con un LOTE de
        // preguntas (decisión del dueño nº1). El candado diario se quema al abrir la
        // sesión; cada respuesta suma puntos a `puntos_racha` y alimenta la bitácora.
        AdicionalesController adicionales = new AdicionalesController();
        app.get("/api/trivia/sesion", adicionales::obtenerSesionTrivia);
        app.post("/api/trivia/evaluar", adicionales::evaluarRespuesta);

        PreguntasController preguntasController = new PreguntasController();
        app.get("/api/preguntas", preguntasController::listarPreguntas);
        app.get("/api/preguntas/express", preguntasController::obtenerExpress);

        // ---- Medallas -------------------------------------------------------
        app.get("/api/medallas/{id_usuario}", adicionales::obtenerMedallasUsuario);
        // Catálogo completo (las 6) con el estado desbloqueada/bloqueada por usuario.
        app.get("/api/medallas/catalogo/{id_usuario}", adicionales::obtenerMedallasCatalogo);

        // ---- Deudas y préstamos ---------------------------------------------
        app.get("/api/deudas/{id_usuario}", adicionales::obtenerBalanceDeudas);
        app.post("/api/deudas", adicionales::agregarDeuda);
        app.put("/api/deudas/{id_deuda}", adicionales::editarDeuda);
        app.delete("/api/deudas/{id_deuda}", adicionales::eliminarDeuda);

        // ---- "¿Es realmente necesario?" -------------------------------------
        // El árbol que hay implementado NO es el del brief (pregunta por solvencia,
        // no por necesito/quiero). Se reescribe en la Fase 5.
        ArbolDecisionesController arbolController = new ArbolDecisionesController();
        app.post("/api/arbol/evaluar", arbolController::evaluarPasoArbol);

        // ---- Estadísticas del usuario (Gráficas) ----------------------------
        // Motor de la Fase 6: gastos por día, balance semanal, progreso de apartados,
        // desglose por categoría y gastos hormiga ($1–$20). Antes la pantalla de
        // Gráficas era 100% maqueta con barras hardcodeadas.
        EstadisticasController estadisticasController = new EstadisticasController();
        app.get("/api/estadisticas/{id}", estadisticasController::obtenerEstadisticas);

        // ---- Panel administrativo -------------------------------------------
        // Fase 6: datos reales. AdminDaoImpl ya no inventa métricas ni usuarios.
        AdminController adminController = new AdminController();
        app.get("/api/admin/dashboard", adminController::obtenerDashboardGlobal);
        app.get("/api/admin/usuarios", adminController::listarUsuariosRendimiento);
        app.get("/api/admin/quiz", adminController::obtenerAnaliticaQuiz);
        app.get("/api/admin/apartados", adminController::obtenerAnaliticaApartados);

        System.out.println("CEIL listo: 32 rutas registradas.");
    }
}
