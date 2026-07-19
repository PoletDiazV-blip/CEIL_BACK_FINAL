-- ============================================================================
-- CEIL — Esquema completo de `ceil_db`
-- ============================================================================
-- Idempotente: se puede ejecutar varias veces sobre la base de Aiven, que ya
-- tiene `usuarios` y `movimientos` con datos. No borra nada.
--
--   mysql --host=mysql-288ee15e-ids-0d0d.g.aivencloud.com --port=15787 \
--         --user=avnadmin --password=... --ssl-mode=REQUIRED ceil_db \
--         < db/01_schema.sql
--
-- OJO: MySQL (a diferencia de MariaDB) no soporta `ALTER TABLE ... ADD COLUMN
-- IF NOT EXISTS`, así que los añadidos van por un procedimiento que consulta
-- information_schema. Aiven corre MySQL 8.4.8.
--
-- La tabla `transacciones` NO se crea a propósito: `AnalisisCompraDaoImpl` la
-- usaba, pero ese módulo se elimina en la Fase 2. Todo va a `movimientos`.
-- ============================================================================

-- --- Helpers idempotentes ---------------------------------------------------

DROP PROCEDURE IF EXISTS ceil_add_column;
DROP PROCEDURE IF EXISTS ceil_add_index;

DELIMITER $$

CREATE PROCEDURE ceil_add_column(
    IN p_tabla   VARCHAR(64),
    IN p_columna VARCHAR(64),
    IN p_defin   TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_tabla
          AND COLUMN_NAME  = p_columna
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN `', p_columna, '` ', p_defin);
        PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
END$$

CREATE PROCEDURE ceil_add_index(
    IN p_tabla    VARCHAR(64),
    IN p_indice   VARCHAR(64),
    IN p_columnas TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_tabla
          AND INDEX_NAME   = p_indice
    ) THEN
        SET @ddl = CONCAT('CREATE INDEX `', p_indice, '` ON `', p_tabla, '` (', p_columnas, ')');
        PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
END$$

DELIMITER ;

-- --- usuarios: columnas que faltan ------------------------------------------

-- Sin `rol`, AdminDaoImpl:15 lanza SQLException en cada consulta y el panel
-- admin acaba respondiendo siempre con los datos inventados del catch.
-- Las comillas simples internas van dobladas: Aiven corre con ANSI_QUOTES, así que
-- "..." sería un nombre de columna, no una cadena.
CALL ceil_add_column('usuarios', 'rol', 'VARCHAR(20) NOT NULL DEFAULT ''USER''');

-- El techo de gastos se calcula siempre como ingreso_total / periodo_dias, sin
-- importar si el usuario capturó días, semanas o meses. `periodo_tipo` solo
-- guarda la unidad que eligió, para poder mostrársela tal cual.
CALL ceil_add_column('usuarios', 'periodo_dias', 'INT DEFAULT NULL');
CALL ceil_add_column('usuarios', 'periodo_tipo', 'VARCHAR(10) DEFAULT NULL');

-- Racha de aciertos SEGUIDOS en la trivia (se reinicia al fallar). Es distinta de
-- `puntos_racha`, que acumula los puntos ganados (Fase 6, decisión del dueño nº2:
-- puntos ← trivia). Esta columna dispara la medalla CONSTANCIA al llegar al umbral.
CALL ceil_add_column('usuarios', 'trivia_aciertos_seguidos', 'INT NOT NULL DEFAULT 0');

-- --- movimientos: columnas que faltan ---------------------------------------

-- La descripción opcional que pide el brief. Hoy el front la envía y el backend
-- la tira, porque no hay dónde ponerla.
CALL ceil_add_column('movimientos', 'descripcion', 'VARCHAR(500) DEFAULT NULL');

-- Referencia al catálogo. `categoria_nombre` se conserva por compatibilidad con
-- las filas viejas, que traen el emoji pegado ("🎮 Juegos").
CALL ceil_add_column('movimientos', 'id_categoria', 'INT DEFAULT NULL');

-- El dashboard y las gráficas siempre filtran por usuario y fecha.
CALL ceil_add_index('movimientos', 'idx_movimientos_usuario_fecha', '`id_usuario`, `fecha`');

-- --- categorias: catálogo único ---------------------------------------------
-- Hoy los nombres y colores viven duplicados en el front: 15 en
-- CategorySelectionScreen y 12 en AddExpenseBottomSheet (faltan Luz, Gas, Viaje).
--
-- `es_sensible` marca las categorías que disparan la dinámica "¿lo necesitas o
-- lo quieres?" (según el brief: Entretenimiento, Juegos, Salidas, Otros). Vive
-- en la base y no en el código para poder cambiar la lista sin recompilar.
CREATE TABLE IF NOT EXISTS categorias (
  id_categoria INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nombre       VARCHAR(50)  NOT NULL,
  emoji        VARCHAR(10)  NOT NULL,
  color_hex    VARCHAR(9)   NOT NULL,
  es_sensible  TINYINT(1)   NOT NULL DEFAULT 0,
  UNIQUE KEY uk_categorias_nombre (nombre)
);

-- --- usuario_categorias: las categorías de interés del onboarding -----------
-- Hoy la selección se descarta en NavGraph y no hay dónde guardarla, así que el
-- perfil no puede mostrarla y la "personalización" no personaliza nada.
CREATE TABLE IF NOT EXISTS usuario_categorias (
  id_usuario   INT NOT NULL,
  id_categoria INT NOT NULL,
  PRIMARY KEY (id_usuario, id_categoria),
  FOREIGN KEY (id_usuario)   REFERENCES usuarios (id_usuario)     ON DELETE CASCADE,
  FOREIGN KEY (id_categoria) REFERENCES categorias (id_categoria) ON DELETE CASCADE
);

-- --- metas_ahorro: apartados ------------------------------------------------
-- `estado` es obligatoria: AdminDaoImpl:16 consulta WHERE estado = 'COMPLETADA'.
CREATE TABLE IF NOT EXISTS metas_ahorro (
  id_meta        INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_usuario     INT NOT NULL,
  nombre_meta    VARCHAR(150)  NOT NULL,
  monto_objetivo DECIMAL(10,2) NOT NULL,
  monto_actual   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  fecha_limite   DATE DEFAULT NULL,
  estado         VARCHAR(20) NOT NULL DEFAULT 'EN_PROGRESO',
  fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON DELETE CASCADE
);

-- --- preguntas: banco de la trivia ------------------------------------------
-- Columnas según PreguntaDaoImpl:14. `opcion_c` se conserva porque el modelo
-- Java la mapea, aunque las 15 preguntas del brief solo tienen A y B.
CREATE TABLE IF NOT EXISTS preguntas (
  id_pregunta       INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  pregunta          TEXT NOT NULL,
  opcion_a          TEXT NOT NULL,
  opcion_b          TEXT NOT NULL,
  opcion_c          TEXT DEFAULT NULL,
  opcion_correcta   VARCHAR(1) NOT NULL,
  puntos            INT NOT NULL DEFAULT 10,
  retroalimentacion TEXT,
  categoria         VARCHAR(50) DEFAULT NULL
);

-- --- medallas / usuario_medallas --------------------------------------------
-- TriviaDaoImpl:38 busca la medalla por tipo: WHERE tipo_medalla = ?
CREATE TABLE IF NOT EXISTS medallas (
  id_medal     INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  titulo       VARCHAR(100) NOT NULL,
  descripcion  VARCHAR(255),
  emoji        VARCHAR(10) NOT NULL DEFAULT '🏅',
  tipo_medalla VARCHAR(50) NOT NULL,
  UNIQUE KEY uk_medallas_tipo (tipo_medalla)
);

-- La PK compuesta es obligatoria: TriviaDaoImpl:39 usa INSERT IGNORE para no
-- duplicar la medalla si el usuario vuelve a cumplir la condición.
CREATE TABLE IF NOT EXISTS usuario_medallas (
  id_usuario   INT NOT NULL,
  id_medal     INT NOT NULL,
  fecha_ganada TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_usuario, id_medal),
  FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON DELETE CASCADE,
  FOREIGN KEY (id_medal)   REFERENCES medallas (id_medal)   ON DELETE CASCADE
);

-- --- trivia_respuestas: bitácora de cada respuesta de trivia -----------------
-- Antes no se guardaba ninguna respuesta: solo `ultima_trivia` (la fecha del último
-- intento). Sin bitácora era imposible sacar estadísticas ("cuántas trivias se han
-- respondido", "pregunta más acertada/fallada", participación mensual), que es justo
-- lo que el dueño quiere medir (decisión nº1: sesiones con varias preguntas para tener
-- volumen de datos de testeo). Cada fila es una respuesta evaluada por el servidor.
CREATE TABLE IF NOT EXISTS trivia_respuestas (
  id_respuesta INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_usuario   INT NOT NULL,
  id_pregunta  INT NOT NULL,
  correcta     TINYINT(1) NOT NULL,
  fecha        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_usuario)  REFERENCES usuarios (id_usuario)   ON DELETE CASCADE,
  FOREIGN KEY (id_pregunta) REFERENCES preguntas (id_pregunta) ON DELETE CASCADE
);
CALL ceil_add_index('trivia_respuestas', 'idx_trivia_resp_pregunta', '`id_pregunta`');

-- --- deudas_prestamos: "me deben" y "debo" ----------------------------------
CREATE TABLE IF NOT EXISTS deudas_prestamos (
  id_deuda       INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_usuario     INT NOT NULL,
  nombre_persona VARCHAR(150)  NOT NULL,
  monto          DECIMAL(10,2) NOT NULL,
  tipo           VARCHAR(20)   NOT NULL,                     -- 'ME_DEBEN' | 'DEBO'
  estado         VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE', -- 'PENDIENTE' | 'PAGADO'
  fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON DELETE CASCADE
);

-- --- Limpieza ---------------------------------------------------------------
DROP PROCEDURE IF EXISTS ceil_add_column;
DROP PROCEDURE IF EXISTS ceil_add_index;
