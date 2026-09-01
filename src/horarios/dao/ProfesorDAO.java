/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.dao;

import horarios.modelo.Profesor;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  DAO: ProfesorDAO
 *  CAPA: Data Access Object
 * ============================================================
 *  Acceso a datos de la tabla "profesor" y sus tablas relacionadas:
 *    - profesor_disponibilidad: bloques horarios disponibles del docente
 *    - profesor_materia:        materias que el docente puede impartir
 *
 *  Operaciones principales:
 *    CRUD basico:         agregar, actualizar, eliminar, buscarPorRfc
 *    Disponibilidad:      guardarDisponibilidad (DELETE + batch INSERT)
 *    Materias asignadas:  asignarMaterias, obtenerMateriasAsignadas
 *
 *  Todas las operaciones que modifican multiples tablas usan transacciones
 *  explicitas (setAutoCommit(false) + commit/rollback) para garantizar
 *  atomicidad (o todo sale bien, o nada se guarda).
 * ============================================================
 */
public class ProfesorDAO {

    // ---------------------------------------------------------------
    //  Constantes SQL
    // ---------------------------------------------------------------

    /**
     * Trae todos los profesores ordenados por nombre.
     * LEFT JOIN a categoria_docente (singular, PK "clave") para traer la
     * descripcion y las horas de la categoria ya resueltas.
     */
    private static final String SQL_SELECT_ALL =
        "SELECT p.*, cd.descripcion AS categoria_descripcion, " +
        "       cd.horas_frente_grupo, cd.horas_actividades_complementarias AS categoria_tope_actividades, " +
        "       ac.actividad AS actividad_nombre, ac.horas AS actividad_horas " +
        "FROM profesor p " +
        "LEFT JOIN categoria_docente cd ON p.categoria_docente_clave = cd.clave " +
        "LEFT JOIN actividades_complementarias ac ON p.actividad_complementaria_clave = ac.clave " +
        "ORDER BY p.nombre";

    /** Busca un profesor por su RFC (identificador unico). */
    private static final String SQL_SELECT_RFC =
        "SELECT p.*, cd.descripcion AS categoria_descripcion, " +
        "       cd.horas_frente_grupo, cd.horas_actividades_complementarias AS categoria_tope_actividades, " +
        "       ac.actividad AS actividad_nombre, ac.horas AS actividad_horas " +
        "FROM profesor p " +
        "LEFT JOIN categoria_docente cd ON p.categoria_docente_clave = cd.clave " +
        "LEFT JOIN actividades_complementarias ac ON p.actividad_complementaria_clave = ac.clave " +
        "WHERE p.rfc = ?";

    /** Inserta un nuevo profesor con todos sus campos. */
    private static final String SQL_INSERT =
        "INSERT INTO profesor " +
        "(rfc, nombre, apellidos, email, telefono, pregrado, categoria_docente_clave, actividad_complementaria_clave) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    /** Actualiza todos los campos de un profesor buscado por RFC. */
    private static final String SQL_UPDATE =
        "UPDATE profesor SET nombre=?, apellidos=?, email=?, telefono=?, pregrado=?, " +
        "categoria_docente_clave=?, actividad_complementaria_clave=? WHERE rfc=?";

    /** Elimina un profesor por RFC. */
    private static final String SQL_DELETE =
        "DELETE FROM profesor WHERE rfc = ?";

    /**
     * Reajusta la secuencia autoincremental de id_profesor despues de un DELETE.
     * Esto evita que el ID del siguiente profesor insertado salte a numeros muy altos.
     * setval('seq', MAX(id)) reinicia la secuencia al valor maximo actual.
     */
    private static final String SQL_RESEQ =
        "SELECT setval('profesores_id_profesor_seq', COALESCE((SELECT MAX(id_profesor) FROM profesor), 0))";

    // -- Disponibilidad horaria --

    /** Trae TODAS las disponibilidades de todos los profesores (para carga en bloque). */
    private static final String SQL_DISP_ALL =
        "SELECT profesor_id, dia, hora_inicio, hora_fin " +
        "FROM profesor_disponibilidad ORDER BY profesor_id, dia, hora_inicio";

    /** Trae la disponibilidad de UN solo profesor (para buscarPorRfc). */
    private static final String SQL_DISP_SELECT =
        "SELECT dia, hora_inicio, hora_fin FROM profesor_disponibilidad " +
        "WHERE profesor_id = ? ORDER BY dia, hora_inicio";

    /** Borra TODA la disponibilidad de un profesor (antes de re-insertarla). */
    private static final String SQL_DISP_DELETE =
        "DELETE FROM profesor_disponibilidad WHERE profesor_id = ?";

    /** Inserta un bloque de disponibilidad para un profesor. */
    private static final String SQL_DISP_INSERT =
        "INSERT INTO profesor_disponibilidad (profesor_id, dia, hora_inicio, hora_fin) VALUES (?, ?, ?, ?)";

    // -- Materias asignadas --
    // NOTA: la tabla real profesor_materia solo tiene (id, profesor_id, materia_id,
    // especialidad_id, fecha_asignacion) — NO existe columna grupo_id. Un profesor
    // asignado a una materia puede impartirla a CUALQUIER grupo elegible; ya no se
    // puede restringir a grupos especificos desde aqui (eso lo decide el generador
    // de horarios repartiendo la carga entre los profesores disponibles).

    /** Trae los IDs de materias asignadas a un profesor. */
    private static final String SQL_MAT_SELECT =
        "SELECT materia_id FROM profesor_materia WHERE profesor_id = ?";

    /** Borra todas las materias asignadas a un profesor. */
    private static final String SQL_MAT_DELETE =
        "DELETE FROM profesor_materia WHERE profesor_id = ?";

    // -- Actividad complementaria (tutorias, gestion escolar, etc.) --
    // NOTA: ya NO es una relacion muchos-a-muchos. profesor.actividad_complementaria_clave
    // es una sola columna directa (una actividad por profesor), ya incluida en
    // SQL_SELECT_ALL/SQL_SELECT_RFC/SQL_INSERT/SQL_UPDATE. No hace falta tabla ni
    // metodos aparte para guardarla/leerla: viaja con el resto de los datos del profesor.

    /** Inserta una relacion profesor-materia (sin grupo: aplica a cualquier grupo elegible). */
    private static final String SQL_MAT_INSERT =
        "INSERT INTO profesor_materia (profesor_id, materia_id) VALUES (?, ?)";
        // NOTA: No se usa ON CONFLICT porque asignarMaterias() hace
        // DELETE de todas las filas del profesor antes de insertar las nuevas.
        // Al momento del INSERT no puede haber conflictos, ya que la tabla
        // quedo vacia para ese profesor_id.

    // ---------------------------------------------------------------
    //  CRUD basico
    // ---------------------------------------------------------------

    /**
     * Inserta un nuevo profesor en la BD.
     * Usa setInsertParams() para los 9 campos del INSERT.
     *
     * @param p profesor a insertar
     * @return true si se inserto correctamente
     */
    public boolean agregar(Profesor p) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            setInsertParams(ps, p);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace(); // Ej: RFC duplicado (unique constraint)
            return false;
        }
    }

    /**
     * Actualiza los datos de un profesor existente buscado por RFC.
     * Usa setUpdateParams() donde el RFC va al final como clausula WHERE.
     *
     * @param p profesor con RFC existente y nuevos datos
     * @return true si se modifico correctamente
     */
    public boolean actualizar(Profesor p) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setUpdateParams(ps, p);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina un profesor y reajustamos la secuencia de ID.
     *
     *   1. DELETE del profesor por RFC.
     *   2. setval() para reajustar la secuencia (SQL_RESEQ).
     *   Si el DELETE no afecto ninguna fila -> rollback (RFC no existe).
     *
     * @param rfc RFC del profesor a eliminar
     * @return true si se elimino correctamente
     */
    public boolean eliminar(String rfc) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // iniciar transaccion manual

            try {
                // Paso 1: eliminar el profesor
                try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
                    ps.setString(1, rfc);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback(); // RFC no existe: revertir y retornar false
                        return false;
                    }
                }

                // Paso 2: reajustar la secuencia al MAX(id_profesor) actual
                try (PreparedStatement ps = conn.prepareStatement(SQL_RESEQ)) {
                    ps.executeQuery(); // setval retorna un valor, por eso es executeQuery
                }

                conn.commit(); // ambos pasos exitosos: confirmar
                return true;

            } catch (Exception ex) {
                conn.rollback(); // cualquier error: revertir todo
                ex.printStackTrace();
                return false;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Busca un profesor por RFC y carga su disponibilidad.
     *
     * Para un solo profesor, se hace una query adicional de disponibilidad
     * (patron aceptable: es solo 1 query extra, no N).
     *
     * @param rfc RFC del profesor a buscar
     * @return Profesor con disponibilidad cargada, o null si no existe
     */
    public Profesor buscarPorRfc(String rfc) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_RFC)) {

            ps.setString(1, rfc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Profesor p = mapear(rs);
                    // Cargar disponibilidad con una query adicional (aceptable para 1 profesor)
                    p.setDisponibilidad(cargarDisponibilidad(conn, p.getId()));
                    return p;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Retorna todos los profesores con sus disponibilidades precargadas.
     * @return lista de todos los Profesor con disponibilidad poblada
     */
    public List<Profesor> obtenerTodos() {
        List<Profesor> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {

            // Query 1: cargar TODAS las disponibilidades en un Map de una vez
            Map<Integer, List<Profesor.DisponibilidadBloque>> dispMap =
                cargarTodasDisponibilidades(conn);

            // Query 2: cargar los profesores y asignarles su disponibilidad desde el Map
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Profesor p = mapear(rs);
                    // getOrDefault: si el profesor no tiene disponibilidad registrada, usa lista vacia
                    p.setDisponibilidad(dispMap.getOrDefault(p.getId(), new ArrayList<>()));
                    lista.add(p);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    // ---------------------------------------------------------------
    //  Disponibilidad horaria
    // ---------------------------------------------------------------

    /**
     * Reemplaza completamente la disponibilidad de un profesor.
     * Patron: DELETE todos los bloques actuales + INSERT de los nuevos en batch.
     *
     * Usa transaccion para que el reemplazo sea atomico:
     * si el INSERT falla a medias, el DELETE se revierte (rollback)
     * y la disponibilidad anterior queda intacta.
     *
     * @param profesorId ID del profesor
     * @param bloques    lista de bloques nuevo (puede estar vacia para borrar todo)
     * @return true si se guardo correctamente
     */
    public boolean guardarDisponibilidad(int profesorId, List<Profesor.DisponibilidadBloque> bloques) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Paso 1: borrar todos los bloques anteriores del profesor
                try (PreparedStatement ps = conn.prepareStatement(SQL_DISP_DELETE)) {
                    ps.setInt(1, profesorId);
                    ps.executeUpdate();
                }

                // Paso 2: insertar los nuevos bloques en batch (si hay alguno)
                if (!bloques.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(SQL_DISP_INSERT)) {
                        for (Profesor.DisponibilidadBloque b : bloques) {
                            ps.setInt(1, profesorId);
                            ps.setString(2, b.getDia());
                            ps.setString(3, b.getHoraInicio());
                            ps.setString(4, b.getHoraFin());
                            ps.addBatch(); // acumular en batch
                        }
                        ps.executeBatch(); // un solo viaje a la BD para todos los bloques
                    }
                }

                conn.commit();
                return true;
            } catch (Exception ex) {
                conn.rollback(); // si algo falla, revertir el DELETE y dejar disponibilidad intacta
                ex.printStackTrace();
                return false;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // ---------------------------------------------------------------
    //  NOTA: ya no existen obtenerActividadesAsignadas()/guardarActividades().
    //  La actividad complementaria de un profesor es un solo campo directo
    //  (Profesor.getActividadComplementariaClave()/setActividadComplementariaClave())
    //  que se guarda junto con el resto de sus datos via agregar()/actualizar().
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    //  Materias asignadas
    // ---------------------------------------------------------------

    /**
     * Retorna los IDs de las materias que puede impartir un profesor.
     * Usado al abrir el dialogo de edicion para pre-marcar los checkboxes.
     *
     * @param profesorId ID del profesor
     * @return lista de IDs de materias (puede estar vacia)
     */
    public List<Integer> obtenerMateriasAsignadas(int profesorId) {
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_MAT_SELECT)) {

            ps.setInt(1, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("materia_id"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ids;
    }

    /**
     * Reemplaza las materias asignadas a un profesor.
     * Patron: DELETE todas las asignaciones actuales + INSERT de las nuevas en batch.
     *
     * Si la lista esta vacia, solo hace el DELETE y retorna true
     * (profesor sin materias asignadas es un estado valido).
     *
     * @param profesorId ID del profesor
     * @param materiaIds IDs de las materias que puede impartir
     * @return true si se guardo correctamente
     */
    public boolean asignarMaterias(int profesorId, List<Integer> materiaIds) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Borrar asignaciones anteriores
                try (PreparedStatement ps = conn.prepareStatement(SQL_MAT_DELETE)) {
                    ps.setInt(1, profesorId);
                    ps.executeUpdate();
                }

                if (!materiaIds.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(SQL_MAT_INSERT)) {
                        for (Integer matId : materiaIds) {
                            ps.setInt(1, profesorId);
                            ps.setInt(2, matId);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                conn.commit();
                return true;
            } catch (Exception ex) {
                conn.rollback();
                ex.printStackTrace();
                return false;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Busca profesores por coincidencia parcial en nombre o apellidos.
     * Mapea solo los datos básicos (sin disponibilidad), lo cual es suficiente para la JTable.
     */
    public List<Profesor> buscarPorNombre(String textoBusqueda) {
        List<Profesor> lista = new ArrayList<>();
        String sql = "SELECT p.*, cd.descripcion AS categoria_descripcion, " +
                     "       cd.horas_frente_grupo, cd.horas_actividades_complementarias AS categoria_tope_actividades, " +
                     "       ac.actividad AS actividad_nombre, ac.horas AS actividad_horas " +
                     "FROM profesor p " +
                     "LEFT JOIN categoria_docente cd ON p.categoria_docente_clave = cd.clave " +
                     "LEFT JOIN actividades_complementarias ac ON p.actividad_complementaria_clave = ac.clave " +
                     "WHERE p.nombre ILIKE ? OR p.apellidos ILIKE ? ORDER BY p.nombre";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            // El mismo texto se busca en las dos columnas
            String comodin = "%" + textoBusqueda + "%";
            ps.setString(1, comodin);
            ps.setString(2, comodin);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs)); // Solo mapea datos básicos para la tabla
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al buscar profesor por nombre: " + ex.getMessage());
        }
        return lista;
    }

    // ---------------------------------------------------------------
    //  Helpers privados
    // ---------------------------------------------------------------

    /**
     * Convierte la fila actual del ResultSet en un objeto Profesor.
     * No carga disponibilidad; el caller decide como poblarla.
     *
     * @param rs ResultSet posicionado en una fila valida de la tabla "profesor"
     * @return Profesor con datos basicos poblados (sin disponibilidad)
     */
    private Profesor mapear(ResultSet rs) throws SQLException {
        Profesor p = new Profesor();
        p.setId(rs.getInt("id_profesor"));
        p.setRfc(rs.getString("rfc"));
        p.setNombre(rs.getString("nombre"));
        p.setApellidos(rs.getString("apellidos"));
        p.setEmail(rs.getString("email"));
        p.setTelefono(rs.getString("telefono"));
        p.setPregrado(rs.getString("pregrado"));

        // categoria_docente_clave puede ser NULL (profesor sin categoria asignada aun)
        p.setCategoriaClave(rs.getString("categoria_docente_clave"));
        p.setCategoriaDescripcion(rs.getString("categoria_descripcion"));
        int horasFrenteGrupo = rs.getInt("horas_frente_grupo");
        p.setCategoriaHorasFrenteGrupo(rs.wasNull() ? null : horasFrenteGrupo);
        int topeActividades = rs.getInt("categoria_tope_actividades");
        p.setCategoriaHorasActividadesComplementarias(rs.wasNull() ? null : topeActividades);

        // actividad_complementaria_clave puede ser NULL (profesor sin actividad asignada aun)
        p.setActividadComplementariaClave(rs.getString("actividad_complementaria_clave"));
        p.setActividadComplementariaNombre(rs.getString("actividad_nombre"));
        int actividadHoras = rs.getInt("actividad_horas");
        p.setActividadComplementariaHoras(rs.wasNull() ? null : actividadHoras);

        return p;
    }

    /**
     * Carga disponibilidades de TODOS los profesores en una sola query.
     */
    private Map<Integer, List<Profesor.DisponibilidadBloque>> cargarTodasDisponibilidades(
            Connection conn) throws SQLException {
        Map<Integer, List<Profesor.DisponibilidadBloque>> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_DISP_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int pid = rs.getInt("profesor_id");
                // computeIfAbsent: si no hay lista para este profId, crear una nueva
                map.computeIfAbsent(pid, k -> new ArrayList<>())
                   .add(new Profesor.DisponibilidadBloque(
                       rs.getString("dia"),
                       rs.getString("hora_inicio"),
                       rs.getString("hora_fin")));
            }
        }
        return map;
    }

    /**
     * Carga la disponibilidad de UN solo profesor.
     * @param conn       conexion activa
     * @param profesorId ID del profesor
     * @return lista de DisponibilidadBloque del profesor
     */
    private List<Profesor.DisponibilidadBloque> cargarDisponibilidad(
            Connection conn, int profesorId) throws SQLException {
        List<Profesor.DisponibilidadBloque> bloques = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_DISP_SELECT)) {
            ps.setInt(1, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    bloques.add(new Profesor.DisponibilidadBloque(
                        rs.getString("dia"),
                        rs.getString("hora_inicio"),
                        rs.getString("hora_fin")));
            }
        }
        return bloques;
    }

    /**
     * Asigna los 8 parametros del INSERT de profesor.
     * Orden: rfc(1), nombre(2), apellidos(3), email(4), telefono(5), pregrado(6),
     *        categoria_docente_clave(7), actividad_complementaria_clave(8).
     */
    private void setInsertParams(PreparedStatement ps, Profesor p) throws SQLException {
        ps.setString(1, p.getRfc());
        ps.setString(2, p.getNombre());
        ps.setString(3, p.getApellidos());
        ps.setString(4, p.getEmail());
        ps.setString(5, p.getTelefono());
        ps.setString(6, p.getPregrado());
        setClaveNullable(ps, 7, p.getCategoriaClave());
        setClaveNullable(ps, 8, p.getActividadComplementariaClave());
    }

    /**
     * Asigna los 8 parametros del UPDATE de profesor.
     * El UPDATE tiene diferente orden: no incluye RFC en el SET
     * (solo en el WHERE), y los campos van en diferente posicion.
     *
     * Orden: nombre(1), apellidos(2), email(3), telefono(4), pregrado(5),
     *        categoria_docente_clave(6), actividad_complementaria_clave(7), rfc(8=WHERE).
     */
    private void setUpdateParams(PreparedStatement ps, Profesor p) throws SQLException {
        ps.setString(1, p.getNombre());
        ps.setString(2, p.getApellidos());
        ps.setString(3, p.getEmail());
        ps.setString(4, p.getTelefono());
        ps.setString(5, p.getPregrado());
        setClaveNullable(ps, 6, p.getCategoriaClave());
        setClaveNullable(ps, 7, p.getActividadComplementariaClave());
        ps.setString(8, p.getRfc());             // RFC va al final como clausula WHERE
    }

    /**
     * Asigna una clave de forma null-safe al PreparedStatement.
     * Se usa para categoria_docente_clave y actividad_complementaria_clave:
     * ambas son opcionales (un profesor puede no tener categoria o actividad asignada aun).
     *
     * ps    PreparedStatement donde asignar
     * idx   indice del parametro (posicion del ? en el SQL)
     * clave valor de la clave, o null
     */
    private void setClaveNullable(PreparedStatement ps, int idx, String clave) throws SQLException {
        if (clave != null && !clave.trim().isEmpty()) ps.setString(idx, clave.trim());
        else ps.setNull(idx, Types.VARCHAR);
    }
}
