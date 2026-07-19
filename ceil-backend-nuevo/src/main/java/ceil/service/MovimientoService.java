package ceil.service;

import ceil.dao.CategoriaDao;
import ceil.dao.CategoriaDaoImpl;
import ceil.dao.MovimientoDao;
import ceil.dao.MovimientoDaoImpl;
import ceil.model.Categoria;
import ceil.model.Movimiento;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovimientoService {
    private final MovimientoDao movimientoDao = new MovimientoDaoImpl();
    private final CategoriaDao categoriaDao = new CategoriaDaoImpl();

    public static final String GASTO = "GASTO";
    public static final String INGRESO = "INGRESO";

    /**
     * El tipo se guardaba tal cual llegaba del front, mientras el dashboard
     * filtraba por 'gasto' en minúscula. Normalizarlo aquí evita que un movimiento
     * quede fuera de las sumas por una diferencia de mayúsculas.
     */
    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) return GASTO;
        return INGRESO.equalsIgnoreCase(tipo.trim()) ? INGRESO : GASTO;
    }

    /**
     * Registra el movimiento y devuelve, además del id, si la categoría es
     * "sensible": la señal de que el front debe lanzar la dinámica "¿lo necesitas
     * o lo quieres?". La marca vive en la tabla `categorias`, no en el código.
     */
    public Map<String, Object> registrar(Map<String, Object> datos) {
        Map<String, Object> resultado = new HashMap<>();

        Object idUsuarioCrudo = datos.get("id_usuario");
        Object montoCrudo = datos.get("monto");

        if (!(idUsuarioCrudo instanceof Number) || !(montoCrudo instanceof Number)) {
            resultado.put("ok", false);
            resultado.put("error", "`id_usuario` y `monto` son obligatorios y deben ser numéricos.");
            return resultado;
        }

        BigDecimal monto = BigDecimal.valueOf(((Number) montoCrudo).doubleValue());
        if (monto.signum() <= 0) {
            resultado.put("ok", false);
            resultado.put("error", "El monto debe ser mayor que 0.");
            return resultado;
        }

        String nombreCategoria = (String) datos.get("categoria_nombre");
        Categoria categoria = categoriaDao.buscarPorNombre(nombreCategoria);

        Movimiento movimiento = new Movimiento(
                0,
                ((Number) idUsuarioCrudo).intValue(),
                monto,
                normalizarTipo((String) datos.get("tipo_movimiento")),
                (String) datos.get("concepto"),
                // Se guarda el nombre limpio del catálogo: el front venía mandando
                // "🎮 Juegos", con el emoji pegado, lo que estorba al agrupar.
                categoria != null ? categoria.getNombre() : nombreCategoria,
                categoria != null ? categoria.getId_categoria() : null,
                (String) datos.get("prioridad"),
                (String) datos.get("descripcion"),
                null
        );

        int idGenerado = movimientoDao.registrarMovimiento(movimiento);
        if (idGenerado < 0) {
            resultado.put("ok", false);
            resultado.put("error", "No se pudo registrar el movimiento.");
            return resultado;
        }

        // Fase 6 — disparo de medallas ligadas al gasto. Solo aplican a gastos:
        //  · "Gasto con Propósito": gastó en una categoría que eligió como de interés.
        //  · "Cazador de Gastos Hormiga": sus gastos pequeños ($1–$20) bajaron respecto
        //    a la semana pasada.
        if (GASTO.equals(movimiento.getTipo_movimiento())) {
            int idUsuario = movimiento.getId_usuario();
            MedallaService.evaluarGastoDirigido(idUsuario, movimiento.getId_categoria());
            MedallaService.evaluarReduccionGastos(idUsuario);
        }

        resultado.put("ok", true);
        resultado.put("id_movimiento", idGenerado);
        resultado.put("categoria_sensible",
                categoria != null && categoria.isEs_sensible() && GASTO.equals(movimiento.getTipo_movimiento()));
        return resultado;
    }

    public List<Movimiento> verHistorialUsuario(int idUsuario) {
        return movimientoDao.obtenerHistorial(idUsuario);
    }

    public boolean eliminar(int idMovimiento, int idUsuario) {
        return movimientoDao.eliminarMovimiento(idMovimiento, idUsuario);
    }
}
