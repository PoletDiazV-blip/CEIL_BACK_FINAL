package ceil.controller;

import ceil.dao.CategoriaDao;
import ceil.dao.CategoriaDaoImpl;
import ceil.model.Categoria;
import ceil.security.Sesion;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class CategoriaController {
    private final CategoriaDao categoriaDao = new CategoriaDaoImpl();

    /** Catálogo completo, para que el front deje de tener los 15 nombres y colores a mano. */
    public void listarCatalogo(Context ctx) {
        ctx.status(200).json(categoriaDao.listarTodas());
    }

    /** Las categorías de interés que el usuario eligió en el onboarding. */
    public void listarDeUsuario(Context ctx) {
        int idUsuario = Sesion.uid(ctx); // Fase 7: identidad desde el token.
        ctx.status(200).json(categoriaDao.listarDeUsuario(idUsuario));
    }

    @SuppressWarnings("unchecked")
    public void guardarDeUsuario(Context ctx) {
        int idUsuario = Sesion.uid(ctx); // Fase 7: identidad desde el token.
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);

        Object crudo = datos.get("categorias");
        if (!(crudo instanceof List)) {
            ctx.status(400).json(Map.of("error", "Se espera una lista `categorias` con los ids."));
            return;
        }

        List<Integer> ids = ((List<Object>) crudo).stream()
                .filter(Number.class::isInstance)
                .map(n -> ((Number) n).intValue())
                .toList();

        if (categoriaDao.guardarDeUsuario(idUsuario, ids)) {
            List<Categoria> guardadas = categoriaDao.listarDeUsuario(idUsuario);
            ctx.status(200).json(Map.of("mensaje", "Categorías guardadas", "categorias", guardadas));
        } else {
            ctx.status(500).json(Map.of("error", "No se pudieron guardar las categorías."));
        }
    }
}
