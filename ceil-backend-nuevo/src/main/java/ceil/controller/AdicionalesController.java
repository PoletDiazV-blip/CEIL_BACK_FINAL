package ceil.controller;

import io.javalin.http.Context;
import ceil.database.DatabaseConfig;
import ceil.dao.PreguntaDao;
import ceil.dao.PreguntaDaoImpl;
import ceil.model.Pregunta;
import ceil.service.MedallaService;
import ceil.security.Sesion;
import java.sql.*;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

public class AdicionalesController {

    // Fuente única de preguntas: la tabla `preguntas`. Antes convivían tres fuentes
    // contradictorias (15 hardcodeadas aquí en un bloque static, 15 en el front y las
    // mismas 15 en la tabla). Se dejó solo la tabla y se borraron las otras dos.
    private final PreguntaDao preguntaDao = new PreguntaDaoImpl();

    // Cuántas preguntas trae una sesión por defecto y su tope. El dueño (decisión nº1)
    // quiere UNA sesión al día pero con VARIAS preguntas, para tener volumen de
    // respuestas de testeo: el candado sigue siendo diario, el lote sustituye al
    // "1 pregunta / día" anterior.
    private static final int LOTE_POR_DEFECTO = 5;
    private static final int LOTE_MAXIMO = 10;

    // 1. ABRIR LA SESIÓN DE TRIVIA DEL DÍA (un lote de preguntas al azar, una vez al día)
    public void obtenerSesionTrivia(Context ctx) {
        int idUsuario = Sesion.uid(ctx); // Fase 7: identidad desde el token.
        int cuantas = LOTE_POR_DEFECTO;
        if (ctx.queryParam("n") != null) {
            try { cuantas = Integer.parseInt(ctx.queryParam("n")); } catch (NumberFormatException ignored) {}
        }
        cuantas = Math.max(1, Math.min(cuantas, LOTE_MAXIMO));

        // Candado diario. La comparación "¿ya jugó hoy?" se hace en SQL con CURDATE(),
        // no en Java: así ambos lados de la fecha salen del reloj de la base (Aiven),
        // evitando el desfase de zona horaria con el reloj de la máquina.
        String sqlVerificar = "SELECT (ultima_trivia = CURDATE()) AS ya_jugo FROM usuarios WHERE id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlVerificar)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getBoolean("ya_jugo")) {
                    // 200 con estatus BLOQUEADO para distinguirlo de un error de red.
                    ctx.status(200).json(Map.of(
                            "estatus", "BLOQUEADO",
                            "mensaje_pantalla", "¡Ya jugaste la trivia de hoy! Vuelve mañana para un nuevo reto."
                    ));
                    return;
                }
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
            return;
        }

        // Lote al azar desde la tabla. Nunca se incluye `opcion_correcta`.
        List<Pregunta> lote = preguntaDao.obtenerPreguntasAleatorias(cuantas);
        if (lote.isEmpty()) {
            ctx.status(404).json(Map.of("error", "No hay preguntas configuradas."));
            return;
        }

        // El candado del día se quema AL ABRIR la sesión (no al evaluar cada respuesta):
        // así una sesión puede tener varias preguntas sin que la primera bloquee al resto.
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE usuarios SET ultima_trivia = CURDATE() WHERE id_usuario = ?")) {
            stmt.setInt(1, idUsuario);
            stmt.executeUpdate();
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "No se pudo abrir la sesión: " + e.getMessage()));
            return;
        }

        List<Map<String, Object>> preguntas = new ArrayList<>();
        for (Pregunta p : lote) {
            Map<String, Object> item = new HashMap<>();
            item.put("id_pregunta", p.getIdPregunta());
            item.put("pregunta", p.getPregunta());
            item.put("opcion_a", p.getOpcionA());
            item.put("opcion_b", p.getOpcionB());
            item.put("opcion_c", p.getOpcionC()); // puede ser null; HashMap lo admite
            preguntas.add(item);
        }
        ctx.status(200).json(Map.of("estatus", "OK", "preguntas", preguntas));
    }

    // 2. EVALUAR UNA RESPUESTA (la corrección la decide el servidor, no el cliente)
    public void evaluarRespuesta(Context ctx) {
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);
        int idUsuario = Sesion.uid(ctx); // Fase 7: identidad desde el token.
        int idPregunta = ((Number) datos.get("id_pregunta")).intValue();
        String respuestaUsuario = (String) datos.get("colocada_opcion"); // "A", "B" o "C"

        // Se lee la respuesta correcta y los puntos desde la tabla. El cliente jamás
        // conoce la correcta, así que la trivia no se puede falsificar.
        String correcta = null;
        String retro = null;
        int puntos = 0;
        String sqlPregunta = "SELECT opcion_correcta, retroalimentacion, puntos FROM preguntas WHERE id_pregunta = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlPregunta)) {
            stmt.setInt(1, idPregunta);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    correcta = rs.getString("opcion_correcta");
                    retro = rs.getString("retroalimentacion");
                    puntos = rs.getInt("puntos");
                }
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
            return;
        }

        if (correcta == null) {
            ctx.status(404).json(Map.of("error", "Pregunta no encontrada"));
            return;
        }

        boolean acerto = respuestaUsuario != null && respuestaUsuario.equalsIgnoreCase(correcta);
        int puntosGanados = acerto ? puntos : 0;
        int puntosTotales = 0;
        int rachaAciertos = 0;

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Bitácora de la respuesta: alimenta las estadísticas del panel admin.
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO trivia_respuestas (id_usuario, id_pregunta, correcta) VALUES (?, ?, ?)")) {
                stmt.setInt(1, idUsuario);
                stmt.setInt(2, idPregunta);
                stmt.setBoolean(3, acerto);
                stmt.executeUpdate();
            }

            // Puntos (decisión del dueño nº2: puntos ← trivia) y racha de aciertos
            // seguidos, que se reinicia al fallar. Se hace en una sola sentencia por
            // usuario para no leer-modificar-escribir.
            String sqlPuntos = acerto
                ? "UPDATE usuarios SET puntos_racha = puntos_racha + ?, " +
                  "trivia_aciertos_seguidos = trivia_aciertos_seguidos + 1 WHERE id_usuario = ?"
                : "UPDATE usuarios SET trivia_aciertos_seguidos = 0 WHERE id_usuario = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlPuntos)) {
                if (acerto) { stmt.setInt(1, puntosGanados); stmt.setInt(2, idUsuario); }
                else        { stmt.setInt(1, idUsuario); }
                stmt.executeUpdate();
            }

            // Leer el estado ya actualizado para devolverlo y para evaluar CONSTANCIA.
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT puntos_racha, trivia_aciertos_seguidos FROM usuarios WHERE id_usuario = ?")) {
                stmt.setInt(1, idUsuario);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        puntosTotales = rs.getInt("puntos_racha");
                        rachaAciertos = rs.getInt("trivia_aciertos_seguidos");
                    }
                }
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "Error al registrar la respuesta: " + e.getMessage()));
            return;
        }

        boolean ganoMedalla = false;
        if (acerto) {
            // Medalla de trivia (primera correcta) y CONSTANCIA (varias seguidas).
            ganoMedalla = MedallaService.otorgar(idUsuario, "TRIVIA");
            MedallaService.evaluarConstancia(idUsuario, rachaAciertos);
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("estatus", acerto ? "CORRECTO" : "INCORRECTO");
        respuesta.put("mensaje_pantalla", acerto ? "¡Respuesta correcta, felicidades!" : "¡Lástima, suerte para la siguiente!");
        respuesta.put("explicacion", retro != null ? retro : "");
        respuesta.put("gano_medalla", ganoMedalla);
        respuesta.put("puntos_ganados", puntosGanados);
        respuesta.put("puntos_totales", puntosTotales);
        ctx.status(200).json(respuesta);
    }

    // ==========================================
    // MÓDULO INTERNO: SECCIÓN MEDALLAS Y DEUDAS
    // ==========================================

    public void obtenerMedallasUsuario(Context ctx) {
        int idUsuario = Sesion.uid(ctx); // Fase 7: identidad desde el token.
        List<Map<String, Object>> medallas = new ArrayList<>();
        String sql = "SELECT m.id_medal, m.titulo, m.descripcion, m.tipo_medalla, um.fecha_ganada " +
                "FROM usuario_medallas um INNER JOIN medallas m ON um.id_medal = m.id_medal WHERE um.id_usuario = ? ORDER BY um.fecha_ganada DESC";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                medallas.add(Map.of("id_medal", rs.getInt("id_medal"), "titulo", rs.getString("titulo"), "descripcion", rs.getString("descripcion"), "tipo_medalla", rs.getString("tipo_medalla"), "fecha_ganada", rs.getTimestamp("fecha_ganada").toString()));
            }
            ctx.status(200).json(medallas);
        } catch (SQLException e) { ctx.status(500).json(Map.of("error", e.getMessage())); }
    }

    // Catálogo completo de medallas: LEFT JOIN para incluir también las bloqueadas.
    // Cada medalla llega con desbloqueada=true/false según si el usuario ya la ganó.
    public void obtenerMedallasCatalogo(Context ctx) {
        int idUsuario = Sesion.uid(ctx); // Fase 7: identidad desde el token.
        List<Map<String, Object>> catalogo = new ArrayList<>();
        String sql = "SELECT m.id_medal, m.titulo, m.descripcion, m.emoji, m.tipo_medalla, um.fecha_ganada " +
                "FROM medallas m LEFT JOIN usuario_medallas um " +
                "ON um.id_medal = m.id_medal AND um.id_usuario = ? ORDER BY m.id_medal";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Timestamp fecha = rs.getTimestamp("fecha_ganada");
                Map<String, Object> fila = new HashMap<>();
                fila.put("id_medal", rs.getInt("id_medal"));
                fila.put("titulo", rs.getString("titulo"));
                fila.put("descripcion", rs.getString("descripcion"));
                fila.put("emoji", rs.getString("emoji"));
                fila.put("tipo_medalla", rs.getString("tipo_medalla"));
                fila.put("desbloqueada", fecha != null);
                fila.put("fecha_ganada", fecha != null ? fecha.toString() : null);
                catalogo.add(fila);
            }
            ctx.status(200).json(catalogo);
        } catch (SQLException e) { ctx.status(500).json(Map.of("error", e.getMessage())); }
    }

    // El otorgamiento de medallas vive ahora en MedallaService (Fase 6), que además
    // dispara las otras cinco medallas según metas y gastos. Aquí ya solo se consulta
    // el catálogo (arriba) y se conceden desde evaluarRespuesta vía MedallaService.

    // CRUD Deudas
    public void obtenerBalanceDeudas(Context ctx) {
        int idUsuario = Sesion.uid(ctx); // Fase 7: identidad desde el token.
        List<Map<String, Object>> listaDeudas = new ArrayList<>();
        double totalMeDeben = 0.0; double totalDebo = 0.0;
        String sql = "SELECT id_deuda, nombre_persona, monto, tipo, estado FROM deudas_prestamos WHERE id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario); ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                double monto = rs.getDouble("monto"); String tipo = rs.getString("tipo"); String estado = rs.getString("estado");
                if ("PENDIENTE".equalsIgnoreCase(estado)) {
                    if ("ME_DEBEN".equalsIgnoreCase(tipo)) totalMeDeben += monto; else totalDebo += monto;
                }
                listaDeudas.add(Map.of("id_deuda", rs.getInt("id_deuda"), "nombre_persona", rs.getString("nombre_persona"), "monto", monto, "tipo", tipo, "estado", estado));
            }
            ctx.status(200).json(Map.of("total_me_deben", totalMeDeben, "total_debo", totalDebo, "balance_neto", (totalMeDeben - totalDebo), "deudas", listaDeudas));
        } catch (SQLException e) { ctx.status(500).json(Map.of("error", e.getMessage())); }
    }

    public void agregarDeuda(Context ctx) {
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);
        int idUsuario = Sesion.uid(ctx); // Fase 7: dueño desde el token.
        String nombrePersona = (String) datos.get("nombre_persona");
        double monto = ((Number) datos.get("monto")).doubleValue();
        String tipo = (String) datos.get("tipo");
        String estado = (String) datos.getOrDefault("estado", "PENDIENTE");
        String sql = "INSERT INTO deudas_prestamos (id_usuario, nombre_persona, monto, tipo, estado) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario); stmt.setString(2, nombrePersona); stmt.setDouble(3, monto); stmt.setString(4, tipo.toUpperCase()); stmt.setString(5, estado.toUpperCase());
            stmt.executeUpdate(); ctx.status(201).json(Map.of("mensaje", "Registro guardado con éxito."));
        } catch (SQLException e) { ctx.status(500).json(Map.of("error", e.getMessage())); }
    }

    public void editarDeuda(Context ctx) {
        int idDeuda = Integer.parseInt(ctx.pathParam("id_deuda"));
        int idUsuario = Sesion.uid(ctx);
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);
        // Fase 7: el `AND id_usuario = ?` impide editar la deuda de otra persona (antes
        // este UPDATE no verificaba dueño: cualquiera con el id_deuda podía cambiarla).
        String sql = "UPDATE deudas_prestamos SET nombre_persona = ?, monto = ?, tipo = ?, estado = ? WHERE id_deuda = ? AND id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, (String) datos.get("nombre_persona")); stmt.setDouble(2, ((Number) datos.get("monto")).doubleValue());
            stmt.setString(3, ((String) datos.get("tipo")).toUpperCase()); stmt.setString(4, ((String) datos.get("estado")).toUpperCase());
            stmt.setInt(5, idDeuda); stmt.setInt(6, idUsuario);
            if (stmt.executeUpdate() > 0) ctx.status(200).json(Map.of("mensaje", "Actualizado con éxito."));
            else ctx.status(404).json(Map.of("error", "No existe esa deuda para este usuario."));
        } catch (SQLException e) { ctx.status(500).json(Map.of("error", e.getMessage())); }
    }

    public void eliminarDeuda(Context ctx) {
        int idDeuda = Integer.parseInt(ctx.pathParam("id_deuda"));
        int idUsuario = Sesion.uid(ctx);
        // Fase 7: mismo blindaje que en editar — solo se borra la deuda propia.
        String sql = "DELETE FROM deudas_prestamos WHERE id_deuda = ? AND id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idDeuda); stmt.setInt(2, idUsuario);
            if (stmt.executeUpdate() > 0) ctx.status(200).json(Map.of("mensaje", "Eliminado."));
            else ctx.status(404).json(Map.of("error", "No existe esa deuda para este usuario."));
        } catch (SQLException e) { ctx.status(500).json(Map.of("error", e.getMessage())); }
    }
}
