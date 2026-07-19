-- ============================================================================
-- CEIL — Datos semilla
-- ============================================================================
-- Idempotente vía INSERT IGNORE sobre claves únicas. Ejecutar DESPUÉS de
-- 01_schema.sql.
-- ============================================================================

-- --- Categorías -------------------------------------------------------------
-- Las 15 del brief. Emojis y colores tomados de CategorySelectionScreen.kt:27-43,
-- que hasta ahora era la única fuente (y solo vivía en el front).
--
-- es_sensible = 1 en las cuatro que el brief nombra para disparar
-- "¿es realmente necesario?": Entretenimiento, Juegos, Salidas y Otros.
INSERT IGNORE INTO categorias (nombre, emoji, color_hex, es_sensible) VALUES
  ('Escuela',          '🎓', '#3B82F6', 0),
  ('Entretenimiento',  '🎬', '#F59E0B', 1),
  ('Pasajes',          '🚌', '#10B981', 0),
  ('Comida',           '🍔', '#EF4444', 0),
  ('Aseo',             '🧹', '#8B5CF6', 0),
  ('Cuidado Personal', '🧴', '#EC4899', 0),
  ('Higiene',          '🧻', '#14B8A6', 0),
  ('Salidas',          '🎉', '#F43F5E', 1),
  ('Juegos',           '🎮', '#6366F1', 1),
  ('Material Escolar', '✏️', '#06B6D4', 0),
  ('Renta',            '🏠', '#EAB308', 0),
  ('Luz',              '💡', '#FACC15', 0),
  ('Gas',              '🔥', '#F97316', 0),
  ('Viaje',            '✈️', '#8B5CF6', 0),
  ('Otros',            '📦', '#6B7280', 1);

-- --- Medallas ---------------------------------------------------------------
-- Los 5 criterios que enumera el brief (ahorrar, disminuir gastos, cumplir
-- metas, dirigir gastos, constancia) más TRIVIA, que es el único tipo que el
-- código busca hoy (TriviaDaoImpl:38, AdicionalesController:111).
INSERT IGNORE INTO medallas (titulo, descripcion, emoji, tipo_medalla) VALUES
  ('Mente Financiera UPChiapas', 'Respondiste correctamente una trivia.',                    '🧠', 'TRIVIA'),
  ('Primer Ahorro',              'Hiciste tu primer abono a un apartado de ahorro.',         '💰', 'AHORRO'),
  ('Cazador de Gastos Hormiga',  'Redujiste tus gastos pequeños respecto a la semana pasada.','🐜', 'REDUCCION_GASTOS'),
  ('Meta Cumplida',              'Completaste una meta de ahorro al 100%.',                  '🎯', 'META_CUMPLIDA'),
  ('Gasto con Propósito',        'Dirigiste tu dinero a las categorías que te propusiste.',  '📌', 'GASTO_DIRIGIDO'),
  ('Constante',                  'Acertaste varias trivias seguidas sin fallar.',            '🔥', 'CONSTANCIA');

-- --- Banco de preguntas -----------------------------------------------------
-- Las 15 del brief. Son las mismas que ya estaban hardcodeadas en el bloque
-- static de AdicionalesController.java:17-33 (mismos enunciados, mismas
-- respuestas correctas), así que este volcado no inventa nada.
INSERT IGNORE INTO preguntas
  (id_pregunta, pregunta, opcion_a, opcion_b, opcion_correcta, puntos, retroalimentacion, categoria) VALUES
(1, 'Tienes poco presupuesto para la semana: ¿Es una buena decisión financiera comprar comida chatarra barata para llenarte rápido?',
    'Sí, porque es barata en el momento y soluciona el hambre hoy.',
    'No, porque a la larga gastarás más en salud y no te nutre realmente.',
    'B', 10,
    'A la larga, la comida chatarra afecta tu salud, lo que se traduce en gastos médicos futuros y menor productividad. Comer sano puede planificarse de forma económica.',
    'HABITOS'),
(2, 'Tienes el dinero justo este mes: ¿Debes pagar primero una deuda que te genera intereses o la suscripción de tu plataforma de streaming favorita?',
    'La deuda con intereses, para evitar que tu dinero se siga perdiendo ahí.',
    'La suscripción, porque necesitas entretenimiento diario.',
    'A', 10,
    'Las deudas con intereses crecen como bola de nieve. Detener esa pérdida de dinero siempre es la prioridad número uno.',
    'DEUDAS'),
(3, 'El café que compras diario en la calle parece un gasto insignificante. ¿Es mejor seguir comprándolo diario o prepararlo en casa y ahorrar esa diferencia?',
    'Seguir comprándolo fuera, los gastos tan pequeños no afectan tus metas.',
    'Prepararlo en casa, porque sumando ese gasto diario a fin de mes es una cantidad importante.',
    'B', 10,
    'Es el clásico "gasto hormiga". Parece poco al día, pero al mes o al año representa una cantidad enorme de dinero que pudiste haber ahorrado o invertido.',
    'GASTOS_HORMIGA'),
(4, 'Ves una oferta de ropa con el 50% de descuento en una prenda que no necesitas. ¿Comprarla te hace ahorrar dinero?',
    'Sí, porque estás gastando la mitad de su precio original.',
    'No, porque estás gastando dinero que no tenías planeado en algo que no te hace falta.',
    'B', 10,
    'No ahorraste el 50%, gastaste el 50% de tu dinero en algo que no te hacía falta. El verdadero ahorro es no gastar.',
    'CONSUMO'),
(5, 'Te urge comprar un teléfono nuevo por gusto, pero no tienes ahorros. ¿Es buena idea usar tu fondo de emergencias para comprarlo?',
    'Sí, quedarte con las ganas de un estreno es una emergencia emocional.',
    'No, el fondo de emergencias es exclusivo para imprevistos reales (salud, accidentes o desempleo).',
    'B', 10,
    'Los antojos o deseos no son emergencias. Si usas ese dinero y mañana se descompone tu herramienta de trabajo o tienes un problema de salud, quedarás desprotegido.',
    'AHORRO'),
(6, 'Te llegó un dinero extra que no esperabas. ¿Qué es financieramente más inteligente hacer primero con él?',
    'Gastarlo en un gusto que querías hace mucho antes de que se termine.',
    'Guardar una parte para tu ahorro y usar el resto para abonar a tus deudas.',
    'B', 10,
    'Usarlo para salir de deudas o ahorrar te da tranquilidad a largo plazo. Una vez cubierto eso, puedes destinar un pequeño porcentaje para un gusto.',
    'AHORRO'),
(7, 'Tienes antojo de salir a cenar con amigos pero ya te acabaste tu presupuesto del mes. ¿Debes usar la tarjeta de crédito para ir?',
    'Sí, la tarjeta está para cubrir los momentos en los que no tienes efectivo.',
    'No, si no puedes pagarlo en efectivo este mes, no deberías pedir prestado para consumo inmediato.',
    'B', 10,
    'Si usas crédito para consumo inmediato que no puedes pagar ese mismo mes, solo estás cavando un hoyo financiero para el próximo mes.',
    'CREDITO'),
(8, 'Tienes dos deudas: una pequeña que te estresa verla y una grande. Te sobra un dinero, ¿a cuál le abonas primero?',
    'A la pequeña, para eliminarla por completo del mapa y motivarte.',
    'Da igual, el dinero se acomoda solo en cualquier deuda.',
    'A', 10,
    'Es la estrategia del "efecto bola de nieve". Eliminar la deuda pequeña rápidamente te da un golpe de motivación psicológica y reduce el estrés visual.',
    'DEUDAS'),
(9, 'Si la tarjeta de crédito te pide un "pago mínimo" de $300 y el "pago para no generar intereses" es de $1,200, ¿cuál deberías pagar si tienes el dinero disponible?',
    'El pago mínimo, así te queda más dinero libre para gastar en el mes.',
    'El pago para no generar intereses, para evitar que el banco te cobre extras el próximo mes.',
    'B', 10,
    'El pago mínimo solo mantiene la tarjeta activa pero deja que los intereses sigan creciendo. El pago para no generar intereses mantiene tu deuda en cero cargos extra.',
    'CREDITO'),
(10, 'Quieres comprar un electrodoméstico caro a meses sin intereses. ¿Debes revisar si la mensualidad cabe en tu presupuesto mensual antes de firmar?',
    'No, al ser sin intereses significa que es gratis a la larga.',
    'Sí, porque aunque no tenga intereses, compromete tus ingresos de los próximos meses.',
    'B', 10,
    'Aunque no te cobren extras, esa mensualidad es dinero que ya no tendrás disponible los siguientes meses. Si acumulas muchos "meses sin intereses", puedes terminar asfixiando tus ingresos.',
    'CREDITO'),
(11, 'Tu negocio empezó a dejar buenas ganancias este mes. ¿Debes usar esa ganancia para comprarte ropa cara de inmediato?',
    'Sí, para demostrar que el negocio está funcionando bien.',
    'No, una parte debe reinvertirse en el negocio para que siga creciendo y generando más.',
    'B', 10,
    'En las primeras etapas de un proyecto, reinvertir las ganancias es lo que permite que el negocio crezca, tenga colchón financiero y genere aún más dinero después.',
    'INVERSION'),
(12, 'Tienes una meta de ahorro para comprar una laptop el próximo año. ¿Qué es mejor para cumplirla?',
    'Separar una cantidad fija cada que recibas dinero, antes de empezar a gastar.',
    'Esperar a fin de mes y ahorrar lo que te llegue a sobrar.',
    'A', 10,
    '"Págate a ti mismo primero". Si esperas a ver qué te sobra a fin de mes, lo más probable es que te lo hayas gastado todo.',
    'AHORRO'),
(13, 'Un amigo te ofrece un negocio que promete duplicar tu dinero en una semana sin hacer nada. ¿Es una inversión segura?',
    'Sí, las mejores oportunidades financieras son rápidas y sin esfuerzo.',
    'No, si suena demasiado bueno para ser verdad y promete cero riesgo, probablemente sea una estafa.',
    'B', 10,
    'No existe el dinero fácil ni las inversiones con rendimiento gigante y cero riesgo. Si suena demasiado mágico, casi seguro es un fraude o estafa piramidal.',
    'INVERSION'),
(14, 'Quieres iniciar un proyecto pero necesitas comprar material. ¿Es mejor comprar el material más caro desde el día uno aunque te quedes en ceros?',
    'Sí, hay que empezar con lo más premium siempre.',
    'No, es mejor empezar con lo básico y funcional para validar si el proyecto funciona sin arriesgar todo tu capital.',
    'B', 10,
    'Es un error común. Lo ideal es validar la idea con un "producto mínimo viable" (lo básico y funcional) antes de arriesgar mucho dinero en herramientas caras.',
    'INVERSION'),
(15, 'Trabajas por tu cuenta y tus ingresos cambian mucho cada mes. ¿Tu presupuesto debe calcularse con base en tu mes más alto o en tu mes promedio/bajo?',
    'En el mes más alto, para mentalizarte a ganar siempre eso.',
    'En el mes promedio o bajo, para asegurar que puedas cubrir tus necesidades básicas incluso en los meses difíciles.',
    'B', 10,
    'Si planificas pensando en el mes más alto y el siguiente mes tus ingresos caen, no tendrás cómo cubrir tus gastos básicos. Planificar con el mes bajo te mantiene seguro.',
    'PRESUPUESTO');
