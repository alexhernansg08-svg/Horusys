/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.dao;

import java.sql.*;
import java.text.Normalizer;
import java.util.*;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  DAO: HorarioDAO
 *  CAPA: Data Access Object
 * ============================================================
 *  [PASO 1] limpiarHorarios()           — borra horarios anteriores con TRUNCATE
 *  [PASO 2] inicializarHorarioGeneral() — crea la cuadricula vacia (filas por hora)
 *  [PASO 3] inicializarContexto()       — carga todos los datos en memoria
 *    [PASO 3.1] cargarCatalogoProfesores() — query profesores + sus materias
 *    [PASO 3.2] cargarTodasDisponibilidades() — expande rangos a slots individuales
 *    [PASO 3.3] cargarTodasMaterias()    — indexa materias por "espId|semestre"
 *  [PASO 4] cargarGrupos()              — obtiene la lista de grupos a procesar
 *  [PASO 5] procesarGrupo()             — por cada grupo: ordena materias y delega
 *    [PASO 5.1] procesarMateria()       — elige profesor y asigna bloques
 *      [PASO 5.1.1] elegirProfesor()    — aplica 3 filtros y preferencia
 *      [PASO 5.1.2] asignarBloques()    — pasada 1 (1/dia) y pasada 2 (contiguo)
 *  [PASO 6] flushBloques()              — INSERT batch a horario_profesores y horario_grupos
 *  [PASO 7] persistirHorarioGeneral()   — UPDATE batch a la cuadricula horario_general
 *  [PASO 8] commit()                    — confirma todo en una sola transaccion
 * ============================================================
 */
public class HorarioDAO {

    // ===============================================================
    //  CONSTANTES DEL ALGORITMO
    // ===============================================================

    /** Dias lectivos en el orden en que se recorren. */
    private static final String[] DIAS      = {"Lunes","Martes","Miercoles","Jueves","Viernes"};

    /** Slots del turno matutino (una hora por slot). */
    private static final String[] HORAS_MAT = {"07:00","08:00","09:00","10:00","11:00","12:00","13:00"};

    /** Slots del turno vespertino (una hora por slot). */
    private static final String[] HORAS_VES = {"14:00","15:00","16:00","17:00","18:00","19:00","20:00"};

    /** Maximo de horas de la misma materia que se pueden asignar en un mismo dia. */
    private static final int MAX_POR_DIA = 2;

    /**
     * Forma sin tildes en minusculas, por si alguien lo escribe diferente en la base de datos.
     */
    private static final Map<String, String> DIA_NORM = new HashMap<>();
    static {
        DIA_NORM.put("lunes",     "Lunes");
        DIA_NORM.put("martes",    "Martes");
        DIA_NORM.put("miercoles", "Miercoles");
        DIA_NORM.put("jueves",    "Jueves");
        DIA_NORM.put("viernes",   "Viernes");
    }

    // ===============================================================
    //  RECORDS — estructuras de datos ligeras usadas durante la generacion
    // ===============================================================
    /**
     * Datos de un profesor durante la generacion.
     *
     * materias:  IDs de materias que puede impartir.
     * matGrupos: materia_id -> grupos especificos permitidos
     * con record para no usar mas codigo de map
     */
    record ProfesorSlot(
        int id,
        String rfc,
        String nombre,
        int horasMax,              // viene de categoria_docente.horas_frente_grupo (ya no de profesor.horas_academicas)
        String actividadClave,     // profesor.actividad_complementaria_clave (null = sin actividad asignada)
        String actividadNombre,    // actividades_complementarias.actividad (para mostrar/persistir)
        int actividadHoras,        // actividades_complementarias.horas (horas fijas de esa actividad)
        int actividadTope,         // categoria_docente.horas_actividades_complementarias (tope segun su categoria)
        Set<Integer> materias,
        Map<Integer, Set<Integer>> matGrupos  // clave: materia_id  ->  valor: Set de grupo_id permitidos para esa materia
    ) {}

    /**
     * Datos de un grupo leidos de la BD.
     * El turno ("Matutino"/"Vespertino") determina que array de horas se usa.
     */
    record GrupoSlot(
        int id,
        String codigo,
        String turno, //con este determinamos que Array de horas usar
        int especialidadId,
        int semestre
    ) {}

    /**
     * Datos de una materia leidos del cache.
     * horasSemanales indica cuantos bloques hay que asignar en la semana.
     */
    record MateriaSlot(
        int id,
        String nombre,
        int horasSemanales
    ) {}

    /**
     * Bloque a insertar en horario_profesores.
     * Representa una hora concreta que un profesor imparte una materia a un grupo.
     */
    record BloqueProfesor(
        String rfc,
        String nombreProfesor,
        String materia,
        String grupo,
        String dia,
        String hora
    ) {}

    /**
     * Bloque a insertar en horario_grupos.
     * que materia y profesor tiene en cada slot.
     */
    record BloqueGrupo(
        String grupo,
        String materia,
        String profesor,
        String dia,
        String hora
    ) {}

    // ===============================================================
    //  CLASE INTERNA: Contexto
    //  Guarda todo el estado compartido durante la generacion.
    //  Se divide en 3 secciones:
    //    1. Datos precargados (solo lectura - no cambian durante el algoritmo)
    //    2. Estado mutable   (cambia con cada bloque asignado)
    //    3. Bloques pendientes (se mandan a BD al final en batch)
    // ===============================================================

    private static class Contexto {

        // ── Seccion 1: Datos precargados (solo lectura) ──────────────

        /** Lista de todos los profesores disponibles para asignacion. */
        final List<ProfesorSlot> catalogo = new ArrayList<>();

        /**
         * Disponibilidad por profesor: profId -> conjunto de slots "DIA|HH:00" libres.
         * id_profesor (int) -> Set de strings con formato "Lunes|07:00"
         */
        final Map<Integer, Set<String>> dispCache = new HashMap<>();

        /**
         * Materias indexadas por "espId|semestre".
         * id_especialidad|id_semestre, (String, ej: "3|2")  ->  valor: lista de MateriaSlot de ese semestre
         * Permite obtener en O(1) las materias de un grupo sin query extra.
         */
        final Map<String, List<MateriaSlot>> materiasCache = new HashMap<>();

        // ──────── Seccion 2: Cambia con cada bloque ───────

        /** Slots ya ocupados por cada profesor.
         *  id_profesor (int) -> Set de slots ocupados con formato "Lunes|07:00" */
        private final Map<Integer, Set<String>>  profOcup  = new HashMap<>();

        /** Slots ya ocupados por cada grupo.
         *  codigo del grupo (String, ej: "1A") -> Set de slots ocupados con formato "Lunes|07:00" */
        private final Map<String,  Set<String>>  grupoOcup = new HashMap<>();

        /** Total de horas asignadas a cada profesor hasta el momento.
         *  id_profesor (int) -> numero de horas ya asignadas (int) */
        private final Map<Integer, Integer>      profHoras = new HashMap<>();

        /**
         * Contador de veces que un profesor da clase a un grupo.
         * id_profesor (int) -> Map interno donde
         * codigo del grupo (String) -> Numero de veces que el profesor da clase a ese grupo (int)
         * Permite preferir profesores que aun no han dado clase a ese grupo.
         */
        private final Map<Integer, Map<String, Integer>> porGrupo = new HashMap<>();

        // ── Seccion 3: Bloques pendientes de persistir ────────────────

        /** Filas listas para batch INSERT en horario_profesores. */
        final List<BloqueProfesor> pendingProf  = new ArrayList<>();

        /** Filas listas para batch INSERT en horario_grupos. */
        final List<BloqueGrupo>   pendingGrupo = new ArrayList<>();

        /** Cuadricula para horario_general.
         *  hora (String, ej: "07:00") -> Map interno donde
         *  dia (String, ej: "Lunes") -> etiqueta de clase (String, ej: "1A: Matematicas") */
        final Map<String, Map<String, String>> hGeneral = new LinkedHashMap<>();

        // ── Inicializacion de estructuras por entidad ─────────────────

        /** Prepara los Maps internos para un profesor que aun no existe en ellos. */
        void inicializarProfesor(int profId) {
            profOcup .putIfAbsent(profId, new HashSet<>());
            profHoras.putIfAbsent(profId, 0);
            porGrupo .putIfAbsent(profId, new HashMap<>());
        }

        /** Prepara los Maps internos para un grupo que aun no existe en ellos. */
        /** preparamos la estructura de cada profesor antes de usarla */
        void inicializarGrupo(String codigoGrupo) {
            grupoOcup.putIfAbsent(codigoGrupo, new HashSet<>());
        }

        // ── Lecturas de estado ────────────────────────────────────────

        /** Slots ocupados por el profesor (getOrvacio si no existe). */
        Set<String> slotsProf(int profId) {
            return profOcup.getOrDefault(profId, Collections.emptySet());
        }

        /** Slots ocupados por el grupo (vacio si no existe). */
        Set<String> slotsGrupo(String grupo) {
            return grupoOcup.getOrDefault(grupo, Collections.emptySet());
        }

        /** Slots disponibles del profesor segun su disponibilidad registrada en BD. */
        Set<String> disponibilidad(int profId) {
            return dispCache.getOrDefault(profId, Collections.emptySet());
        }

        /** Horas totales asignadas al profesor hasta ahora. */
        int horasUsadas(int profId) {
            return profHoras.getOrDefault(profId, 0);
        }

        /** Cuantas veces el profesor ya da clase al grupo indicado. */
        int vecesEnGrupo(int profId, String g) {
            return porGrupo.getOrDefault(profId, Collections.emptyMap()).getOrDefault(g, 0);
        }

        // ── Mutaciones ────────────────────────────────────────────────

        /**
         * Marca el slot como ocupado para el profesor y el grupo,
         * e incrementa el contador de horas del profesor.
         * Se llama una vez por cada bloque confirmado en asignarBloques().
         */
        void marcarAsignado(int profId, String grupo, String slot) {
            profOcup .computeIfAbsent(profId, k -> new HashSet<>()).add(slot);
            grupoOcup.computeIfAbsent(grupo,  k -> new HashSet<>()).add(slot);
            profHoras.merge(profId, 1, Integer::sum);
            porGrupo .computeIfAbsent(profId, k -> new HashMap<>()).merge(grupo, 1, Integer::sum);
        }

        /**
         * Igual que marcarAsignado(), pero para un bloque de ACTIVIDAD COMPLEMENTARIA
         * (no hay grupo real involucrado): solo ocupa el slot del profesor y suma
         * a sus horas totales, sin tocar grupoOcup ni porGrupo.
         */
        void marcarActividad(int profId, String slot) {
            profOcup.computeIfAbsent(profId, k -> new HashSet<>()).add(slot);
            profHoras.merge(profId, 1, Integer::sum);
        }

        /**
         * Acumula un par de bloques en las listas de persistencia
         * y actualiza la cuadricula horario_general.
         * Se llama justo despues de marcarAsignado() para el mismo bloque.
         */
        void acumularBloque(BloqueProfesor bp, BloqueGrupo bg) {
            pendingProf .add(bp);
            pendingGrupo.add(bg);

            // Etiqueta para la cuadricula: "CODIGO: Materia"
            String etiqueta = bp.grupo() + ": " + bp.materia();
            hGeneral.computeIfAbsent(bp.hora(), k -> new LinkedHashMap<>()).merge(bp.dia(), etiqueta, (prev, n) -> prev + " / " + n);
        }

        /**
         * Igual que acumularBloque(), pero SOLO para actividades complementarias:
         * no existe un grupo real, asi que NO se agrega a pendingGrupo/horario_grupos.
         * Insertar ahi un grupo ficticio compartido ("(Actividad)") causaba falsos
         * "CHOQUE GRUPO" en el validador cada vez que 2+ profesores tenian actividad
         * a la misma hora (algo normal, no un choque real).
         */
        void acumularBloqueActividad(BloqueProfesor bp) {
            pendingProf.add(bp);

            String etiqueta = "(Actividad): " + bp.materia() + " [" + bp.nombreProfesor() + "]";
            hGeneral.computeIfAbsent(bp.hora(), k -> new LinkedHashMap<>()).merge(bp.dia(), etiqueta, (prev, n) -> prev + " / " + n);
        }
    }

    // ===============================================================
    //  [PUNTO DE ENTRADA] generarHorario()
    //  Orquesta todos los pasos del 1 al 8 dentro de una transaccion.
    //  Si cualquier paso lanza excepcion, el try-with-resources cierra
    //  la conexion con autoCommit=false, lo que revierte todo (rollback).
    // ===============================================================

    public boolean generarHorario(int usuarioId) {
        System.out.println("================================================");
        System.out.println("  GENERACION INTELIGENTE DE HORARIOS v8");
        System.out.printf ("  Director/Usuario ID: %d%n", usuarioId);
        System.out.println("================================================");

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) { System.err.println("Sin conexion."); return false; }

            // Iniciar transaccion manual: nada se guarda hasta el commit() final
            conn.setAutoCommit(false);

            // [PASO 1] Borrar horarios anteriores y registrar quien genero este
            limpiarHorarios(conn, usuarioId);

            // [PASO 2] Crear filas vacias en horario_general (una por cada hora del dia)
            inicializarHorarioGeneral(conn);

            // [PASO 3] Cargar profesores, disponibilidades y materias en memoria
            Contexto ctx = inicializarContexto(conn);

            // [PASO 4] Obtener lista de todos los grupos a procesar
            List<GrupoSlot> grupos = cargarGrupos(conn);

            // [PASO 5] Procesar cada grupo: asignar materias y profesores
            int total = grupos.size(), idx = 0;
            for (GrupoSlot grupo : grupos)
                procesarGrupo(grupo, ++idx, total, ctx);

            // [PASO 5.5] Rellenar huecos de cada profesor con su actividad complementaria
            // (tutorias, asesorias, etc.) para que no le queden horas libres
            asignarActividadesComplementarias(ctx);

            // [PASO 6] Mandar todos los bloques acumulados a la BD en un solo batch
            flushBloques(conn, ctx);

            // [PASO 7] Actualizar la cuadricula visual horario_general con las clases
            persistirHorarioGeneral(conn, ctx.hGeneral);

            // [PASO 8] Confirmar toda la transaccion: ahora si se guarda todo en BD
            conn.commit();
            System.out.println("\n  HORARIO GENERADO CON EXITO.");
            return true;

        } catch (SQLException e) {
            System.err.println("Error SQL: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ===============================================================
    //  [PASO 1] limpiarHorarios()
    //  Borra todo el horario anterior con TRUNCATE CASCADE y registra
    //  en horario_meta quien disparo la generacion y cuando.
    //  Si horario_meta no existe, el error se ignora (retrocompatibilidad).
    // ===============================================================

    private void limpiarHorarios(Connection conn, int usuarioId) throws SQLException {
        // TRUNCATE es mas rapido que DELETE; RESTART IDENTITY reinicia los IDs;
        // CASCADE borra tambien las tablas dependientes automaticamente
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                "TRUNCATE TABLE horario_general, horario_grupos, " +
                "horario_profesores RESTART IDENTITY CASCADE");
        }
        // Registrar metadato: quien genero el horario y cuando
        // ON CONFLICT: si ya existe una fila, se sobreescribe (solo hay un horario activo)
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO horario_meta(usuario_id, fecha_generacion) VALUES(?, NOW()) " +
                "ON CONFLICT (id) DO UPDATE SET usuario_id = EXCLUDED.usuario_id, " +
                "fecha_generacion = EXCLUDED.fecha_generacion")) {
            ps.setInt(1, usuarioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("  (Nota: tabla horario_meta no encontrada, metadato omitido)");
        }
        System.out.println("  Tablas limpiadas. Generado por usuario ID: " + usuarioId);
    }

    // ===============================================================
    //  [PASO 2] inicializarHorarioGeneral()
    //  Inserta una fila vacia por cada hora del dia en horario_general.
    //  Estas filas se actualizan en el [PASO 7] con las clases asignadas.
    //  Se hace en batch: 14 INSERTs en un solo viaje a la BD.
    // ===============================================================

    private void inicializarHorarioGeneral(Connection conn) throws SQLException {
        String[] all = {"07:00","08:00","09:00","10:00","11:00","12:00","13:00",
                        "14:00","15:00","16:00","17:00","18:00","19:00","20:00"};

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO horario_general(hora,lunes,martes,miercoles,jueves,viernes)" +
                " VALUES(?,'','','','','')")) {
            for (String h : all) {
                ps.setString(1, h);
                ps.addBatch(); // acumular
            }
            ps.executeBatch(); // ejecutar todos juntos
        }
        System.out.println("  Cuadricula horaria inicializada.");
    }

    // ===============================================================
    //  [PASO 3] inicializarContexto()
    //  Carga todos los datos necesarios en el objeto Contexto antes
    //  de entrar al loop de grupos.
    // ===============================================================

    private Contexto inicializarContexto(Connection conn) throws SQLException {
        Contexto ctx = new Contexto();

        // [PASO 3.1] Cargar profesores y sus materias asignadas
        cargarCatalogoProfesores(conn, ctx);

        // [PASO 3.2] Cargar y expandir disponibilidades a slots "DIA|HH:00"
        ctx.dispCache.putAll(cargarTodasDisponibilidades(conn));

        // [PASO 3.3] Cargar materias indexadas por "especialidadId|semestre"
        ctx.materiasCache.putAll(cargarTodasMaterias(conn));

        // Preparar estructuras de estado para cada profesor del catalogo
        for (ProfesorSlot prof : ctx.catalogo)
            ctx.inicializarProfesor(prof.id());

        return ctx;
    }

    // ===============================================================
    //  [PASO 3.1] cargarCatalogoProfesores()
    //  Usa 2 queries separadas para evitar el producto cartesiano que
    //  generaria un JOIN (un profesor con N materias apareceria N veces).
    //    Instruccion 1: datos basicos del profesor -> construye ProfesorSlot
    //    Instruccion 2: tabla profesor_materia -> llena sets de materias y grupos
    // ===============================================================

    private void cargarCatalogoProfesores(Connection conn, Contexto ctx) throws SQLException {
        // idx sirve para enlazar la Query 2 con el ProfesorSlot ya creado en Query 1
        // clave: id_profesor (int)  ->  valor: ProfesorSlot correspondiente
        Map<Integer, ProfesorSlot> idx = new HashMap<>();

        // Instruccion 1: datos basicos + categoria (horas_frente_grupo) + actividad complementaria.
        // La prioridad ya no vive en profesor: el orden real de asignacion lo decide
        // procesarGrupo() ordenando las MATERIAS por horas_semanales (ver [PASO 5]).
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT p.id_profesor, p.rfc, p.nombre, p.apellidos, " +
                "       COALESCE(cd.horas_frente_grupo, 0)               AS horas_max, " +
                "       COALESCE(cd.horas_actividades_complementarias,0) AS actividad_tope, " +
                "       p.actividad_complementaria_clave, " +
                "       ac.actividad                                     AS actividad_nombre, " +
                "       COALESCE(ac.horas, 0)                            AS actividad_horas " +
                "FROM profesor p " +
                "LEFT JOIN categoria_docente cd ON cd.clave = p.categoria_docente_clave " +
                "LEFT JOIN actividades_complementarias ac ON ac.clave = p.actividad_complementaria_clave " +
                "ORDER BY p.id_profesor ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ProfesorSlot prof = new ProfesorSlot(
                    rs.getInt("id_profesor"),
                    rs.getString("rfc"),
                    rs.getString("nombre") + " " + rs.getString("apellidos"),
                    rs.getInt("horas_max"),
                    rs.getString("actividad_complementaria_clave"),
                    rs.getString("actividad_nombre"),
                    rs.getInt("actividad_horas"),
                    rs.getInt("actividad_tope"),
                    new HashSet<>(),   // materias: se llena en Query 2
                    new HashMap<>()    // matGrupos: se llena en Query 2
                );
                ctx.catalogo.add(prof);
                idx.put(prof.id(), prof); // guardar referencia para Query 2
            }
        }

        // Instruccion 2: llenar los sets de materias por profesor.
        // profesor_materia ahora SI tiene grupo_id (columna agregada a proposito):
        // si viene NULL, el profesor puede dar esa materia a CUALQUIER grupo elegible
        // (compatibilidad con asignaciones viejas); si trae un grupo especifico, el
        // profesor SOLO puede impartir esa materia a ese grupo puntual.
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT profesor_id, materia_id, grupo_id FROM profesor_materia");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ProfesorSlot prof = idx.get(rs.getInt("profesor_id"));
                if (prof == null) continue;

                int matId = rs.getInt("materia_id");
                prof.materias().add(matId); // agregar materia al set del profe

                int grupoId = rs.getInt("grupo_id");
                if (!rs.wasNull()) {
                    // Grupo especifico: solo puede dar esa materia a ese grupo
                    prof.matGrupos().computeIfAbsent(matId, k -> new HashSet<>()).add(grupoId);
                } else {
                    // Sin grupo especifico: set vacio = puede dar la materia a cualquier grupo
                    prof.matGrupos().putIfAbsent(matId, new HashSet<>());
                }
            }
        }
    }

    // ===============================================================
    //  [PASO 3.2] cargarTodasDisponibilidades()
    //  Lee rangos de disponibilidad de la BD y los expande a slots.
    //  Ejemplo: dia=Lunes, hora_inicio=07:00, hora_fin=10:00
    //    genera: "Lunes|07:00", "Lunes|08:00", "Lunes|09:00"
    //  Resultado: Map<profId, Set<"DIA|HH:00">>
    // ===============================================================

    private Map<Integer, Set<String>> cargarTodasDisponibilidades(Connection conn) throws SQLException {
        // clave: id_profesor (int)  ->  valor: Set de slots disponibles con formato "Lunes|07:00"
        Map<Integer, Set<String>> map = new HashMap<>();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT profesor_id, dia, hora_inicio, hora_fin " +
                "FROM profesor_disponibilidad ORDER BY profesor_id, dia, hora_inicio");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int    pid = rs.getInt("profesor_id");
                String dia = normDia(rs.getString("dia")); // normalizar tildes/mayusculas
                int    ini = horaInt(rs.getString("hora_inicio"));
                int    fin = horaInt(rs.getString("hora_fin"));
                if (fin <= ini) continue; // rango invalido: ignorar

                // Expandir el rango a un slot por cada hora
                Set<String> slots = map.computeIfAbsent(pid, k -> new LinkedHashSet<>());
                for (int h = ini; h < fin; h++)
                    slots.add(dia + "|" + String.format("%02d:00", h));
            }
        }
        return map;
    }

    // ===============================================================
    //  [PASO 3.3] cargarTodasMaterias()
    //  Indexa todas las materias por la clave "especialidadId|semestre".
    //  Ejemplo: clave "3|2" -> materias del semestre 2 de la especialidad 3.
    //  Permite buscar en O(1) al procesar cada grupo (sin query extra).
    // ===============================================================

    private Map<String, List<MateriaSlot>> cargarTodasMaterias(Connection conn) throws SQLException {
        // clave: "id_especialidad|id_semestre" (String, ej: "3|2")  ->  valor: lista de MateriaSlot de ese semestre
        Map<String, List<MateriaSlot>> map = new HashMap<>();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_materia, nombre, horas_semanales, id_especialidad, id_semestre " +
                "FROM materias");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                // La clave agrupa materias del mismo semestre y especialidad
                String key = rs.getInt("id_especialidad") + "|" + rs.getInt("id_semestre");
                map.computeIfAbsent(key, k -> new ArrayList<>())
                   .add(new MateriaSlot(
                       rs.getInt("id_materia"),
                       rs.getString("nombre"),
                       rs.getInt("horas_semanales")
                   ));
            }
        }
        return map;
    }

    // ===============================================================
    //  [PASO 4] cargarGrupos()
    //  Obtiene todos los grupos de la BD como lista de GrupoSlot.
    //  Ordenados por especialidad, semestre y codigo para consistencia.
    // ===============================================================

    private List<GrupoSlot> cargarGrupos(Connection conn) throws SQLException {
        List<GrupoSlot> lista = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, codigo, turno, especialidad_id, semestre " +
                "FROM grupos ORDER BY especialidad_id, semestre, codigo");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new GrupoSlot(
                    rs.getInt("id"),
                    rs.getString("codigo"),
                    rs.getString("turno"),
                    rs.getInt("especialidad_id"),
                    rs.getInt("semestre")
                ));
        }
        return lista;
    }

    // ===============================================================
    //  [PASO 5] procesarGrupo()
    //  Por cada grupo:
    //    - Obtiene sus materias del cache (sin query a BD)
    //    - Las ordena de mayor a menor horas (greedy: pesadas primero)
    //    - Delega cada materia al [PASO 5.1]
    // ===============================================================

    private void procesarGrupo(GrupoSlot grupo, int idx, int total, Contexto ctx) {
        System.out.printf("%n  [%d/%d] Grupo: %-18s | Turno: %-10s | Sem %d%n",
                          idx, total, grupo.codigo(), grupo.turno(), grupo.semestre());

        ctx.inicializarGrupo(grupo.codigo());

        // Buscar las materias del grupo en cache usando la clave "espId|semestre"
        String clave = grupo.especialidadId() + "|" + grupo.semestre();
        List<MateriaSlot> materias = new ArrayList<>(
            ctx.materiasCache.getOrDefault(clave, Collections.emptyList()));

        // Ordenar de mayor a menor horas: asignar primero las materias con mas horas
        // reduce la fragmentacion (bin-packing greedy)
        materias.sort(Comparator.comparingInt(MateriaSlot::horasSemanales).reversed());

        // Seleccionar el array de horas segun el turno del grupo
        String[] horas = "Matutino".equalsIgnoreCase(grupo.turno()) ? HORAS_MAT : HORAS_VES;

        // [PASO 5.1] Procesar cada materia del grupo
        for (MateriaSlot mat : materias)
            procesarMateria(mat, grupo, horas, ctx);
    }

    // ===============================================================
    //  [PASO 5.1] procesarMateria()
    //  Para una materia de un grupo:
    //    - [PASO 5.1.1] Elige el mejor profesor disponible
    //    - [PASO 5.1.2] Asigna bloques en dos pasadas (distribuir + agrupar)
    // ===============================================================

    private void procesarMateria(MateriaSlot mat, GrupoSlot grupo,
                                 String[] horas, Contexto ctx) {

        // Ordenar el catalogo por menor carga global primero (desempate estable por id).
        // La "prioridad" real del sistema ya no es del profesor: es de la MATERIA,
        // porque procesarGrupo() ordena las materias del grupo por horas_semanales
        // descendente antes de llegar aqui (ver [PASO 5]). Esto reparte la carga
        // entre profesores sin favorecer siempre al mismo por un numero fijo.
        List<ProfesorSlot> ordenado = new ArrayList<>(ctx.catalogo);
        ordenado.sort(
            Comparator.comparingInt((ProfesorSlot p) -> ctx.horasUsadas(p.id()))
                      .thenComparingInt(ProfesorSlot::id)
        );

        // [PASO 5.1.1] Elegir el mejor profesor para esta materia y grupo
        ProfesorSlot prof = elegirProfesor(mat, grupo, horas, ordenado, ctx);
        if (prof == null) {
            System.out.printf("      !! SIN PROFESOR para '%s'%n", mat.nombre());
            return; // no hay profesor disponible: materia queda sin asignar
        }

        // cntDia rastrea cuantas horas de esta materia ya hay en cada dia
        // clave: nombre del dia (String, ej: "Lunes")  ->  valor: lista de horas ya asignadas ese dia (ej: ["07:00","08:00"])
        // Sirve para respetar el limite MAX_POR_DIA
        Map<String, List<String>> cntDia = new HashMap<>();

        int rest = mat.horasSemanales();

        // [PASO 5.1.2a] Pasada 1: maximo 1 hora por dia (distribuir en dias distintos)
        rest = asignarBloques(prof, mat, grupo, horas, rest, cntDia, 1, ctx);

        // [PASO 5.1.2b] Pasada 2: si quedan horas, agrupar de forma contigua (2 por dia)
        if (rest > 0)
            rest = asignarBloques(prof, mat, grupo, horas, rest, cntDia, MAX_POR_DIA, ctx);

        System.out.printf("      %-35s | %d/%d hrs | Prof: %s%n",
                          mat.nombre(), mat.horasSemanales() - rest,
                          mat.horasSemanales(), prof.nombre());
        if (rest > 0)
            System.out.printf("        >> INCOMPLETA: faltan %d hrs%n", rest);
    }

    // ===============================================================
    //  [PASO 5.5] asignarActividadesComplementarias()
    //  Despues de agendar TODAS las materias de TODOS los grupos, revisa
    //  a cada profesor: si le quedan horas libres respecto a su horasMax
    //  (categoria_docente.horas_frente_grupo) y tiene una actividad
    //  complementaria asignada, le llena ese hueco con dicha actividad
    //  (acotado por las horas propias de la actividad y por el tope de
    //  su categoria) para que su carga quede completa, sin espacios libres.
    //
    //  No involucra a ningun grupo: solo ocupa el tiempo del profesor.
    // ===============================================================

    private void asignarActividadesComplementarias(Contexto ctx) {
        String[] todasLasHoras = new String[HORAS_MAT.length + HORAS_VES.length];
        System.arraycopy(HORAS_MAT, 0, todasLasHoras, 0, HORAS_MAT.length);
        System.arraycopy(HORAS_VES, 0, todasLasHoras, HORAS_MAT.length, HORAS_VES.length);

        for (ProfesorSlot prof : ctx.catalogo) {
            if (prof.actividadClave() == null) continue; // sin actividad asignada

            int hueco = prof.horasMax() - ctx.horasUsadas(prof.id());
            if (hueco <= 0) continue; // ya cumplio su carga con materias

            // No puede exceder ni las horas propias de la actividad ni el tope de su categoria
            int aAsignar = Math.min(hueco, Math.min(prof.actividadHoras(), prof.actividadTope()));
            if (aAsignar <= 0) continue;

            Set<String> disp = ctx.disponibilidad(prof.id());
            Set<String> ocup = ctx.slotsProf(prof.id());
            int asignadas = 0;

            // Mejora: consolidar la actividad en los dias que el profesor YA tiene
            // clase (en vez de dispersarla por toda la semana), y dentro de cada dia,
            // preferir la hora pegada a un bloque ya ocupado (extender, no salpicar).
            // Esto evita huecos como "lunes 14:00, martes 20:00, miercoles 17:00...".
            List<String> diasOrdenados = new ArrayList<>(Arrays.asList(DIAS));
            diasOrdenados.sort(Comparator.comparingInt(
                (String dia) -> contarHorasOcupadasDia(ocup, dia, todasLasHoras)).reversed());

            outer:
            for (String dia : diasOrdenados) {
                List<String> horasOrdenadas = ordenarHorasPorCercania(ocup, dia, todasLasHoras);
                for (String hora : horasOrdenadas) {
                    if (asignadas >= aAsignar) break outer;
                    String slot = dia + "|" + hora;
                    if (!disp.contains(slot) || ocup.contains(slot)) continue;

                    ctx.marcarActividad(prof.id(), slot);
                    ocup.add(slot); // reflejar de inmediato para que la siguiente hora
                                     // de esta misma actividad se pegue a esta
                    ctx.acumularBloqueActividad(
                        new BloqueProfesor(prof.rfc(), prof.nombre(), prof.actividadNombre(), "(Actividad)", dia, hora)
                    );
                    asignadas++;
                }
            }

            if (asignadas > 0) {
                System.out.printf("      + %-30s | %d hrs de '%s' (relleno de carga)%n",
                                  prof.nombre(), asignadas, prof.actividadNombre());
            }
        }
    }

    /** Cuenta cuantas horas del dia ya estan ocupadas para ese profesor (de cualquier turno). */
    private int contarHorasOcupadasDia(Set<String> ocup, String dia, String[] todasLasHoras) {
        int n = 0;
        for (String hora : todasLasHoras) if (ocup.contains(dia + "|" + hora)) n++;
        return n;
    }

    /**
     * Ordena las horas de un dia de manera que las mas cercanas a un bloque ya
     * ocupado queden primero (extender el bloque existente), en vez de recorrer
     * las horas siempre de corrido 07:00->20:00 sin importar donde ya hay clases.
     * Si el profesor no tiene nada ese dia todavia, se deja el orden natural.
     */
    private List<String> ordenarHorasPorCercania(Set<String> ocup, String dia, String[] todasLasHoras) {
        List<Integer> ocupadasIdx = new ArrayList<>();
        for (int i = 0; i < todasLasHoras.length; i++)
            if (ocup.contains(dia + "|" + todasLasHoras[i])) ocupadasIdx.add(i);

        if (ocupadasIdx.isEmpty()) return new ArrayList<>(Arrays.asList(todasLasHoras));

        List<String> resultado = new ArrayList<>(Arrays.asList(todasLasHoras));
        resultado.sort(Comparator.comparingInt(hora -> {
            int i = Arrays.asList(todasLasHoras).indexOf(hora);
            int distanciaMin = Integer.MAX_VALUE;
            for (int oi : ocupadasIdx) distanciaMin = Math.min(distanciaMin, Math.abs(i - oi));
            return distanciaMin;
        }));
        return resultado;
    }

    // ===============================================================
    //  [PASO 5.1.1] elegirProfesor()
    //  Recorre el catalogo (ya ordenado por carga y prioridad) y aplica
    //  3 filtros en orden. El primero que pase todos es elegido.
    //
    //  Filtro 1: tiene la materia asignada?
    //  Filtro 1b: si tiene grupos especificos, el grupo debe estar incluido?
    //  Filtro 2: le quedan horas por asignar?
    //  Filtro 3: tiene al menos un slot libre compatible con el turno?
    //
    //  Preferencia: se prefiere al que aun no ha dado clase a este grupo.
    //  Si no hay tal candidato, se devuelve el primer valido (fallback).
    // ===============================================================

    private ProfesorSlot elegirProfesor(MateriaSlot mat, GrupoSlot grupo,
                                        String[] horas,
                                        List<ProfesorSlot> ordenado,
                                        Contexto ctx) {
        ProfesorSlot fallback = null;

        for (ProfesorSlot prof : ordenado) {

            // Filtro 1: el profesor debe tener la materia asignada
            if (!prof.materias().contains(mat.id())) continue;

            // Filtro 1b: si tiene grupos especificos, el grupo actual debe estar en ellos
            Set<Integer> gruposPermitidos = prof.matGrupos().get(mat.id());
            if (gruposPermitidos != null && !gruposPermitidos.isEmpty()
                    && !gruposPermitidos.contains(grupo.id())) continue;

            // Filtro 2: el profesor no debe haber alcanzado su maximo de horas
            if (ctx.horasUsadas(prof.id()) >= prof.horasMax()) continue;

            // Filtro 3: debe tener al menos un slot libre compatible con el turno del grupo
            Set<String> disp     = ctx.disponibilidad(prof.id());
            Set<String> ocupProf = ctx.slotsProf(prof.id());
            Set<String> ocupGrup = ctx.slotsGrupo(grupo.codigo());

            boolean haySlot = false;
            outer:
            for (String dia : DIAS)
                for (String hora : horas) {
                    String c = dia + "|" + hora;
                    if (disp.contains(c) && !ocupProf.contains(c) && !ocupGrup.contains(c)) {
                        haySlot = true;
                        break outer;
                    }
                }
            if (!haySlot) continue;

            // Preferencia: elegir al que aun no ha dado clase a este grupo
            if (ctx.vecesEnGrupo(prof.id(), grupo.codigo()) == 0) return prof;

            // Si ya dio clase aqui, guardarlo como fallback por si no hay mejor opcion
            if (fallback == null) fallback = prof;
        }

        return fallback; // null si ningun profesor paso los filtros
    }

    // ===============================================================
    //  [PASO 5.1.2] asignarBloques()
    //  Asigna hasta 'restantes' bloques de la materia al profesor,
    //  respetando el limite 'maxPorDia' por dia.
    //
    //  Pasada 1 (maxPorDia=1): elige un slot aleatorio por dia para
    //    distribuir las horas en dias distintos.
    //  Pasada 2 (maxPorDia=2): elige un slot contiguo (+1 o -1 hora)
    //    al ya asignado ese dia, para que las horas sean seguidas.
    //
    //  Por cada bloque confirmado llama a:
    //    ctx.marcarAsignado() — actualiza estado interno
    //    ctx.acumularBloque() — acumula para el INSERT final (PASO 6)
    //
    //  Retorna cuantos bloques quedaron sin asignar (0 = exito total).
    // ===============================================================

    private int asignarBloques(ProfesorSlot prof, MateriaSlot mat, GrupoSlot grupo,String[] horas, int restantes,Map<String, List<String>> cntDia, int maxPorDia,Contexto ctx) {
        Set<String> disp     = ctx.disponibilidad(prof.id());
        Set<String> ocupProf = ctx.slotsProf(prof.id());
        Set<String> ocupGrup = ctx.slotsGrupo(grupo.codigo());
        Set<String> horasSet = new HashSet<>(Arrays.asList(horas));
        Random rng = new Random();
        int diaInicio = 0; // rotar el dia de inicio para no saturar siempre el Lunes

        while (restantes > 0 && ctx.horasUsadas(prof.id()) < prof.horasMax()) {
            boolean ok = false;

            for (int i = 0; i < DIAS.length; i++) {
                int    dx  = (diaInicio + i) % DIAS.length;
                String dia = DIAS[dx];

                List<String> yaEnEsteDia = cntDia.getOrDefault(dia, Collections.emptyList());
                if (yaEnEsteDia.size() >= maxPorDia) continue; // dia lleno

                String horaElegida = null;

                if (yaEnEsteDia.isEmpty()) {
                    // Pasada 1: slot libre aleatorio del turno para distribuir entre dias
                    List<String> mezcladas = new ArrayList<>(Arrays.asList(horas));
                    Collections.shuffle(mezcladas, rng);
                    for (String hora : mezcladas) {
                        String c = dia + "|" + hora;
                        if (disp.contains(c) && !ocupProf.contains(c) && !ocupGrup.contains(c)) {
                            horaElegida = hora;
                            break;
                        }
                    }
                } else {
                    // Pasada 2: buscar slot contiguo (+1 o -1 hora) al ya asignado ese dia
                    busqueda:
                    for (String yaHora : yaEnEsteDia) {
                        int h = horaInt(yaHora);
                        for (int delta : new int[]{-1, +1}) {
                            String candidata = String.format("%02d:00", h + delta);
                            if (!horasSet.contains(candidata)) continue; // fuera del turno
                            String c = dia + "|" + candidata;
                            if (disp.contains(c) && !ocupProf.contains(c) && !ocupGrup.contains(c)) {
                                horaElegida = candidata;
                                break busqueda;
                            }
                        }
                    }
                }

                if (horaElegida == null) continue; // no hay slot libre en este dia

                // ── Confirmar asignacion del bloque ──────────────────────────
                String slot = dia + "|" + horaElegida;

                // Actualizar estado interno del Contexto
                ctx.marcarAsignado(prof.id(), grupo.codigo(), slot);
                cntDia.computeIfAbsent(dia, k -> new ArrayList<>()).add(horaElegida);

                // Acumular bloque para el INSERT masivo del [PASO 6]
                ctx.acumularBloque(
                    new BloqueProfesor(prof.rfc(), prof.nombre(),
                                       mat.nombre(), grupo.codigo(), dia, horaElegida),
                    new BloqueGrupo(grupo.codigo(), mat.nombre(),
                                    prof.nombre(), dia, horaElegida)
                );

                restantes--;
                diaInicio = (dx + 1) % DIAS.length; // siguiente iteracion empieza en el dia siguiente
                ok = true;
                break; // un bloque asignado por iteracion del while
            }

            if (!ok) break; // ninguno de los 5 dias tenia slot: no se puede asignar mas
        }

        return restantes; // 0 = todo asignado; >0 = horas que quedaron sin asignar
    }

    // ===============================================================
    //  [PASO 6] flushBloques()
    //  Manda todos los bloques acumulados en pendingProf y pendingGrupo
    //  a la BD en dos batch INSERTs (uno por tabla).
    //  Se ejecuta una sola vez al final, minimizando los viajes a la BD.
    // ===============================================================

    private void flushBloques(Connection conn, Contexto ctx) throws SQLException {
        if (ctx.pendingProf.isEmpty()) return; // nada que persistir

        // Batch INSERT en horario_profesores
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO horario_profesores" +
                "(rfc_profesor,nombre_profesor,materia,grupo,dia,hora) VALUES(?,?,?,?,?,?)")) {
            for (BloqueProfesor bp : ctx.pendingProf) {
                ps.setString(1, bp.rfc());
                ps.setString(2, bp.nombreProfesor());
                ps.setString(3, bp.materia());
                ps.setString(4, bp.grupo());
                ps.setString(5, bp.dia());
                ps.setString(6, bp.hora());
                ps.addBatch(); // acumular
            }
            ps.executeBatch(); // ejecutar todos juntos
        }

        // Batch INSERT en horario_grupos
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO horario_grupos(grupo,materia,profesor,dia,hora) VALUES(?,?,?,?,?)")) {
            for (BloqueGrupo bg : ctx.pendingGrupo) {
                ps.setString(1, bg.grupo());
                ps.setString(2, bg.materia());
                ps.setString(3, bg.profesor());
                ps.setString(4, bg.dia());
                ps.setString(5, bg.hora());
                ps.addBatch(); // acumular
            }
            ps.executeBatch(); // ejecutar todos juntos
        }

        System.out.printf("  %d bloques persistidos en batch.%n", ctx.pendingProf.size());
    }

    // ===============================================================
    //  [PASO 7] persistirHorarioGeneral()
    //  Actualiza la cuadricula horario_general con las etiquetas
    //  acumuladas en hGeneral durante el algoritmo.
    //  Usa batch UPDATE: una fila por hora del dia (max 14 updates).
    // ===============================================================

    private void persistirHorarioGeneral(Connection conn,
            Map<String, Map<String, String>> hGeneral) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE horario_general " +
                "SET lunes=?, martes=?, miercoles=?, jueves=?, viernes=? WHERE hora=?")) {
            for (Map.Entry<String, Map<String, String>> e : hGeneral.entrySet()) {
                Map<String, String> dias = e.getValue();
                // getOrDefault: si no hay clase esa hora/dia, dejar cadena vacia
                ps.setString(1, dias.getOrDefault("Lunes",     ""));
                ps.setString(2, dias.getOrDefault("Martes",    ""));
                ps.setString(3, dias.getOrDefault("Miercoles", ""));
                ps.setString(4, dias.getOrDefault("Jueves",    ""));
                ps.setString(5, dias.getOrDefault("Viernes",   ""));
                ps.setString(6, e.getKey()); // la hora es el WHERE
                ps.addBatch(); // acumular
            }
            ps.executeBatch(); // ejecutar todos juntos
        }
        System.out.printf("  horario_general actualizado (%d horas con clases).%n",
                          hGeneral.size());
    }

    // ===============================================================
    //  UTILIDADES
    // ===============================================================

    /**
     * Extrae la parte entera de una hora en formato "HH:mm".
     * Ejemplo: "07:00" -> 7, "14:00" -> 14.
     * Retorna 0 si el formato es invalido.
     */
    private int horaInt(String h) {
        try { return Integer.parseInt(h.split(":")[0]); } catch (Exception e) { return 0; }
    }

    /**
     * Normaliza el nombre de un dia eliminando tildes y convirtiendo
     * a la forma canonica del mapa DIA_NORM.
     * Permite aceptar "Miercoles", "MIERCOLES", "miercoles", etc.
     */
    private String normDia(String dia) {
        if (dia == null) return "";
        String sinTildes = Normalizer.normalize(dia.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
        return DIA_NORM.getOrDefault(sinTildes, dia.trim());
    }
}

