-- ============================================================
--  MIGRACION (v2, corregida): Categorias del docente
--  + relacion N:M profesor <-> actividades complementarias
-- ============================================================
--  IMPORTANTE: esta version YA NO crea la tabla
--  "actividades_complementarias" porque en tu base de datos
--  ya existe (con columnas clave, tipo_actividad, actividad, horas
--  y 15 filas de catalogo real). Ese catalogo se reutiliza tal cual.
--
--  Que agrega esta migracion:
--    1. Tabla categorias_docente        (catalogo: Tiempo Completo, Medio Tiempo, etc.)
--    2. Tabla profesor_actividad        (relacion N:M profesor <-> actividades_complementarias,
--                                         usando "clave" como llave foranea)
--    3. Columna profesor.categoria_id   (FK hacia categorias_docente)
--
--  Seguro de re-ejecutar: usa IF NOT EXISTS / ON CONFLICT en todo.
-- ============================================================

BEGIN;

-- ---------------------------------------------------------------
-- 1. Catalogo de categorias del docente
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.categorias_docente (
    id     SERIAL PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL UNIQUE
);

INSERT INTO public.categorias_docente (nombre) VALUES
    ('Tiempo Completo'),
    ('Tres Cuartos de Tiempo'),
    ('Medio Tiempo'),
    ('Por Asignatura')
ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------
-- 2. Relacion profesor <-> actividad complementaria (N:M)
--    Usa "clave" (VARCHAR) porque asi esta definida la PK de
--    la tabla actividades_complementarias que ya existe.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.profesor_actividad (
    id              SERIAL PRIMARY KEY,
    profesor_id     INTEGER NOT NULL REFERENCES public.profesor(id_profesor) ON DELETE CASCADE,
    actividad_clave VARCHAR(20) NOT NULL REFERENCES public.actividades_complementarias(clave) ON DELETE CASCADE,
    UNIQUE (profesor_id, actividad_clave)
);

-- ---------------------------------------------------------------
-- 3. Columna nueva en "profesor": categoria del docente
-- ---------------------------------------------------------------
ALTER TABLE public.profesor
    ADD COLUMN IF NOT EXISTS categoria_id INTEGER REFERENCES public.categorias_docente(id) ON DELETE SET NULL;

COMMIT;
