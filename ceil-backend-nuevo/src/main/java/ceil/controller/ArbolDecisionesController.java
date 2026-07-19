package ceil.controller;

import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

/**
 * La dinámica "¿Es realmente necesario?" del brief (la "imagen 1").
 *
 * Se dispara al registrar un gasto de una categoría marcada como sensible
 * (Entretenimiento, Juegos, Salidas, Otros — ver `categorias.es_sensible`).
 *
 * OJO: el árbol que había implementado aquí NO era este. Preguntaba por solvencia
 * ("¿tienes el dinero disponible sin afectar tus gastos fijos?") y no compartía ni
 * la raíz ni las ramas con lo que el dueño especificó.
 *
 *   ¿Lo necesitas o lo quieres?
 *     ├── NECESITO → fin: la compra es comprensible
 *     └── QUIERO   → "podríamos buscar otra forma de conseguirlo"
 *                     ¿Encontraste otra forma?
 *                       ├── SÍ → fin: creemos un apartado para conseguirlo sin afectar
 *                       └── NO → ¿Esto afecta o acorta tus gastos?
 *                                  ├── SÍ → fin: mejor apártalo
 *                                  └── NO → fin: puedes comprarlo, pero analízalo
 */
public class ArbolDecisionesController {

    private static final String MENSAJE_INICIAL =
            "Se detecta una compra innecesaria. Analicemos antes de continuar.";

    private record Opcion(String valor, String etiqueta) {}

    private static final List<Map<String, String>> NECESITO_O_QUIERO = List.of(
            Map.of("valor", "NECESITO", "etiqueta", "Lo necesito"),
            Map.of("valor", "QUIERO", "etiqueta", "Lo quiero")
    );

    private static final List<Map<String, String>> SI_O_NO = List.of(
            Map.of("valor", "SI", "etiqueta", "Sí"),
            Map.of("valor", "NO", "etiqueta", "No")
    );

    public void evaluarPasoArbol(Context ctx) {
        @SuppressWarnings("unchecked")
        Map<String, Object> datos = ctx.bodyAsClass(Map.class);

        String nodo = ((String) datos.getOrDefault("nodo_actual", "INICIO")).toUpperCase();
        String respuesta = ((String) datos.getOrDefault("respuesta", "")).toUpperCase();

        Map<String, Object> paso = switch (nodo) {
            case "INICIO" -> pregunta(
                    "NECESIDAD_O_DESEO",
                    "¿Lo necesitas o lo quieres?",
                    NECESITO_O_QUIERO);

            case "NECESIDAD_O_DESEO" -> "NECESITO".equals(respuesta)
                    // Fin de la rama "lo necesito". El brief sugiere aquí aportar esa
                    // cantidad a un apartado, para llevar el control.
                    ? veredicto(
                        "VEREDICTO_NECESIDAD",
                        "Si lo necesitas, la compra es comprensible. Registra el gasto con "
                        + "tranquilidad, y si quieres, apártale esa cantidad para tenerla controlada.",
                        true, true)
                    : pregunta(
                        "BUSCAR_OTRA_FORMA",
                        "Lo quieres. Podríamos buscar otra forma de conseguirlo.\n"
                        + "¿Encontraste otra forma?",
                        SI_O_NO);

            case "BUSCAR_OTRA_FORMA" -> "SI".equals(respuesta)
                    ? veredicto(
                        "VEREDICTO_OTRA_FORMA",
                        "¡Muy bien! Podríamos crear un apartado o una meta para conseguirlo "
                        + "sin afectar tu presupuesto.",
                        false, true)
                    : pregunta(
                        "AFECTA_GASTOS",
                        "¿Esto afecta o acorta tus gastos?",
                        SI_O_NO);

            case "AFECTA_GASTOS" -> "SI".equals(respuesta)
                    ? veredicto(
                        "VEREDICTO_AFECTA",
                        "Si afecta tus gastos, lo mejor es no comprarlo ahora. "
                        + "Creemos un apartado para conseguirlo sin descuadrarte.",
                        false, true)
                    : veredicto(
                        "VEREDICTO_NO_AFECTA",
                        "Si no afecta tus gastos, podrías comprarlo. Aun así, "
                        + "no estaría de más analizarlo un poco más.",
                        true, false);

            default -> null;
        };

        if (paso == null) {
            ctx.status(400).json(Map.of("error", "Nodo del árbol no reconocido: " + nodo));
            return;
        }
        ctx.status(200).json(paso);
    }

    private Map<String, Object> pregunta(String nodo, String texto, List<Map<String, String>> opciones) {
        return Map.of(
                "nodo_siguiente", nodo,
                "mensaje_inicial", MENSAJE_INICIAL,
                "texto", texto,
                "opciones", opciones,
                "es_final", false,
                "permitir_compra", true,
                "sugerir_apartado", false
        );
    }

    /**
     * @param permitirCompra   si false, el front ofrece deshacer el gasto ya registrado.
     * @param sugerirApartado  si true, el front ofrece crear un apartado con ese monto.
     */
    private Map<String, Object> veredicto(String nodo, String texto,
                                          boolean permitirCompra, boolean sugerirApartado) {
        return Map.of(
                "nodo_siguiente", nodo,
                "mensaje_inicial", MENSAJE_INICIAL,
                "texto", texto,
                "opciones", List.of(),
                "es_final", true,
                "permitir_compra", permitirCompra,
                "sugerir_apartado", sugerirApartado
        );
    }
}
