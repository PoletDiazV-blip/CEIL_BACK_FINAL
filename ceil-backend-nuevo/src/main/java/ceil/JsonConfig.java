package ceil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.json.JavalinJackson;

/**
 * La API venía devolviendo tres convenciones distintas a la vez: `id_movimiento`
 * en movimientos, `idMeta` en metas y `opcionA` en preguntas. El front tendría que
 * parsear cada endpoint de una forma. Aquí se fija una sola: snake_case en todo.
 *
 * Y las fechas salían como milisegundos desde 1970 (`1797746400000`); ahora son
 * cadenas ISO, que es lo que el front puede mostrar sin convertir.
 */
public final class JsonConfig {

    private JsonConfig() {}

    public static JavalinJackson mapper() {
        ObjectMapper mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new JavalinJackson(mapper);
    }
}
