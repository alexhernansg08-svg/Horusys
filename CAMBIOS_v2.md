# Cambios aplicados — version optimizada

## HorarioDAO.java (mayores cambios)

### 1. normDia() usa java.text.Normalizer
Antes: reemplazaba manualmente solo `\u00e9` y `\u00e8`.
Ahora: normaliza a NFD y elimina todos los diacriticos Unicode con `\p{M}`,
cubriendo tildes, dieresis y cualquier otra variante sin casos manuales.

### 2. Eliminada concatenacion de columna en SQL (registrarBloque)
Antes: cada bloque disparaba un UPDATE con el nombre de columna construido
en tiempo de ejecucion (`"SET " + col + " = ..."`), patron que viola las
buenas practicas de PreparedStatement aunque la fuente fuera un switch interno.

Ahora: los datos se acumulan en ctx.hGeneral (Map en memoria) y al terminar
todos los grupos se escribe horario_general en un unico batch con un UPDATE
completamente parametrizado (`SET lunes=?, martes=?, ...`). Sin columnas
interpoladas; 100% PreparedStatement.

### 3. Reduccion de roundtrips a BD
Antes: un UPDATE individual a horario_general por cada bloque asignado
(potencialmente cientos de UPDATEs aislados).
Ahora: un solo batch de UPDATEs al final de generarHorario(), uno por hora
con datos.

### 4. Etiquetado de version: v8 (era v7)

## ProfesorDAO.java
- Eliminado cheque muerto `if (conn == null)` en agregarProfesor.
  getConnection() lanza SQLException si falla; nunca retorna null, por lo que
  el cheque era inalcanzable y confuso.

## UserDAO.java / LoginController.java
- Eliminado parametro no utilizado `claveSistema` de loginUser().
  LoginController ya pasaba null; el parametro era dead code en la firma.

## Archivos con saltos de linea CRLF -> LF
Normalizados a LF Unix (git-friendly, evita diffs fantasma):
  - modelo/Especialidad.java
  - modelo/Grupo.java
  - modelo/Materia.java
  - modelo/Profesor.java
  - dao/EspecialidadDAO.java
  - dao/MateriaDAO.java

## horarios_db_script_sql_fixed.sql
- Corregido bug critico: usuario_id = 5 hardcodeado en dos funciones PostgreSQL.
  horario_grupo_tabla() y horario_por_grupo() ahora aceptan un parametro
  opcional p_usuario_id (DEFAULT NULL). Cuando es NULL, no filtra por usuario,
  comportamiento compatible con el codigo Java actual.
