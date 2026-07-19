package ceil.dao;

import ceil.model.Categoria;
import java.util.List;

public interface CategoriaDao {
    List<Categoria> listarTodas();

    /** Resuelve una categoría venga como "Juegos" o como "🎮 Juegos". null si no existe. */
    Categoria buscarPorNombre(String nombre);

    List<Categoria> listarDeUsuario(int idUsuario);

    /** Reemplaza por completo las categorías de interés del usuario. */
    boolean guardarDeUsuario(int idUsuario, List<Integer> idsCategorias);
}
