package horarios.dao;

import java.sql.*;
import java.text.Normalizer;
import java.util.*;

public class HorarioDAO {

    private static final String[] DIAS      = {"Lunes","Martes","Miercoles","Jueves","Viernes"};
    private static final String[] HORAS_MAT = {"07:00","08:00","09:00","10:00","11:00","12:00","13:00"};
    private static final String[] HORAS_VES = {"14:00","15:00","16:00","17:00","18:00","19:00","20:00"};
    private static final int MAX_POR_DIA = 2;

    private static final Map<String, String> DIA_NORM = new HashMap<>();
    static {
        DIA_NORM.put("lunes",     "Lunes");
        DIA_NORM.put("martes",    "Martes");
        DIA_NORM.put("miercoles", "Miercoles");
        DIA_NORM.put("jueves",    "Jueves");
        DIA_NORM.put("viernes",   "Viernes");
    }

    // RECORD: 'horasAsignadasReales' controla el tope exacto de clases;
    // 'horasMaxPlaza' se usa solo para el hueco de actividades complementarias.
    record ProfesorSlot(
        int id,
        String rfc,
        String nombre,
        int horasMaxPlaza,          // Horas tope de su plaza (ej. 40 hrs) - solo para actividades
        int horasAsignadasReales,   // SUMA REAL de horas_semanales en profesor_materia (ej. 10 hrs)
        String actividadClave,     
        String actividadNombre,    
        int actividadHoras,        
        int actividadTope,         
        Set<Integer> materias,
        Map<Integer, Set<Integer>> matGrupos
    ) {
        /**
         * Techo real de CLASES para este profesor: sus horas asignadas en
         * profesor_materia, pero nunca por encima de lo que su plaza permite.
         * Protege contra el caso borde de que profesor_materia termine con
         * mas horas de las que su categoria autoriza (ej. edicion manual
         * futura que no pase por la validacion de la ventana de Materias).
         */
        int limiteClases() {
            return Math.min(horasAsignadasReales, horasMaxPlaza);
        }
    }

    record GrupoSlot(int id, String codigo, String turno, int especialidadId, int semestre) {}
    record MateriaSlot(int id, String nombre, int horasSemanales) {}
    record BloqueProfesor(String rfc, String nombreProfesor, String materia, String grupo, String dia, String hora) {}
    record BloqueGrupo(String grupo, String materia, String profesor, String dia, String hora) {}

    private static class Contexto {
        final List<ProfesorSlot> catalogo = new ArrayList<>();
        final Map<Integer, Set<String>> dispCache = new HashMap<>();
        final Map<String, List<MateriaSlot>> materiasCache = new HashMap<>();

        private final Map<Integer, Set<String>>  profOcup  = new HashMap<>();
        private final Map<String,  Set<String>>  grupoOcup = new HashMap<>();
        private final Map<Integer, Integer>      profHoras = new HashMap<>();
        private final Map<Integer, Map<String, Integer>> porGrupo = new HashMap<>();

        final List<BloqueProfesor> pendingProf  = new ArrayList<>();
        final List<BloqueGrupo>   pendingGrupo = new ArrayList<>();
        final Map<String, Map<String, String>> hGeneral = new LinkedHashMap<>();

        void inicializarProfesor(int profId) {
            profOcup .putIfAbsent(profId, new HashSet<>());
            profHoras.putIfAbsent(profId, 0);
            porGrupo .putIfAbsent(profId, new HashMap<>());
        }

        void inicializarGrupo(String codigoGrupo) {
            grupoOcup.putIfAbsent(codigoGrupo, new HashSet<>());
        }

        Set<String> slotsProf(int profId) { return profOcup.getOrDefault(profId, Collections.emptySet()); }
        Set<String> slotsGrupo(String grupo) { return grupoOcup.getOrDefault(grupo, Collections.emptySet()); }
        Set<String> disponibilidad(int profId) { return dispCache.getOrDefault(profId, Collections.emptySet()); }
        int horasUsadas(int profId) { return profHoras.getOrDefault(profId, 0); }
        int vecesEnGrupo(int profId, String g) { return porGrupo.getOrDefault(profId, Collections.emptyMap()).getOrDefault(g, 0); }

        void marcarAsignado(int profId, String grupo, String slot) {
            profOcup .computeIfAbsent(profId, k -> new HashSet<>()).add(slot);
            grupoOcup.computeIfAbsent(grupo,  k -> new HashSet<>()).add(slot);
            profHoras.merge(profId, 1, Integer::sum);
            porGrupo .computeIfAbsent(profId, k -> new HashMap<>()).merge(grupo, 1, Integer::sum);
        }

        void marcarActividad(int profId, String slot) {
            profOcup.computeIfAbsent(profId, k -> new HashSet<>()).add(slot);
            profHoras.merge(profId, 1, Integer::sum);
        }

        void acumularBloque(BloqueProfesor bp, BloqueGrupo bg) {
            pendingProf .add(bp);
            pendingGrupo.add(bg);
            String etiqueta = bp.grupo() + ": " + bp.materia();
            hGeneral.computeIfAbsent(bp.hora(), k -> new LinkedHashMap<>()).merge(bp.dia(), etiqueta, (prev, n) -> prev + " / " + n);
        }

        void acumularBloqueActividad(BloqueProfesor bp) {
            pendingProf.add(bp);
            String etiqueta = bp.materia() + " [" + bp.nombreProfesor() + "]";
            hGeneral.computeIfAbsent(bp.hora(), k -> new LinkedHashMap<>()).merge(bp.dia(), etiqueta, (prev, n) -> prev + " / " + n);
        }
    }

    public boolean generarHorario(int usuarioId) {
        System.out.println("================================================");
        System.out.println("  GENERACION INTELIGENTE DE HORARIOS v9 (CORREGIDO)");
        System.out.printf ("  Director/Usuario ID: %d%n", usuarioId);
        System.out.println("================================================");

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) { System.err.println("Sin conexion."); return false; }
            conn.setAutoCommit(false);

            limpiarHorarios(conn, usuarioId);
            inicializarHorarioGeneral(conn);
            Contexto ctx = inicializarContexto(conn);
            List<GrupoSlot> grupos = cargarGrupos(conn);

            // Prioridad de grupos: los que tienen mas horas-clase totales se procesan
            // primero (mientras hay mas disponibilidad libre), en vez del orden fijo
            // especialidad/semestre/codigo. Esto es lo que confirmo el plantel: la
            // prioridad de asignacion es por carga horaria, no por orden alfabetico.
            grupos.sort(Comparator.comparingInt((GrupoSlot g) -> totalHorasGrupo(g, ctx)).reversed());

            int total = grupos.size(), idx = 0;
            for (GrupoSlot grupo : grupos)
                procesarGrupo(grupo, ++idx, total, ctx);

            asignarActividadesComplementarias(ctx);
            flushBloques(conn, ctx);
            persistirHorarioGeneral(conn, ctx.hGeneral);

            conn.commit();
            System.out.println("\n  HORARIO GENERADO CON EXITO.");
            return true;

        } catch (SQLException e) {
            System.err.println("Error SQL: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void limpiarHorarios(Connection conn, int usuarioId) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("TRUNCATE TABLE horario_general, horario_grupos, horario_profesores RESTART IDENTITY CASCADE");
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO horario_meta(usuario_id, fecha_generacion) VALUES(?, NOW()) " +
                "ON CONFLICT (id) DO UPDATE SET usuario_id = EXCLUDED.usuario_id, fecha_generacion = EXCLUDED.fecha_generacion")) {
            ps.setInt(1, usuarioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("  (Nota: tabla horario_meta no encontrada, metadato omitido)");
        }
    }

    private void inicializarHorarioGeneral(Connection conn) throws SQLException {
        String[] all = {"07:00","08:00","09:00","10:00","11:00","12:00","13:00",
                        "14:00","15:00","16:00","17:00","18:00","19:00","20:00"};

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO horario_general(hora,lunes,martes,miercoles,jueves,viernes) VALUES(?,'','','','','')")) {
            for (String h : all) {
                ps.setString(1, h);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private Contexto inicializarContexto(Connection conn) throws SQLException {
        Contexto ctx = new Contexto();
        cargarCatalogoProfesores(conn, ctx);
        ctx.dispCache.putAll(cargarTodasDisponibilidades(conn));
        ctx.materiasCache.putAll(cargarTodasMaterias(conn));

        for (ProfesorSlot prof : ctx.catalogo)
            ctx.inicializarProfesor(prof.id());

        return ctx;
    }

    // Trae, ademas de los datos base, la carga academica REAL asignada
    // (subconsulta SUM sobre profesor_materia+materias) y la horasMaxPlaza
    // (de la categoria), para poder distinguir ambos conceptos.
    private void cargarCatalogoProfesores(Connection conn, Contexto ctx) throws SQLException {
        Map<Integer, ProfesorSlot> idx = new HashMap<>();

        String sql = "SELECT p.id_profesor, p.rfc, p.nombre, p.apellidos, " +
                     "       COALESCE(cd.horas_frente_grupo, 0) AS horas_max_plaza, " +
                     "       COALESCE(pm_totales.total_horas, 0) AS horas_asignadas_reales, " +
                     "       COALESCE(cd.horas_actividades_complementarias,0) AS actividad_tope, " +
                     "       p.actividad_complementaria_clave, " +
                     "       ac.actividad AS actividad_nombre, " +
                     "       COALESCE(ac.horas, 0) AS actividad_horas " +
                     "FROM profesor p " +
                     "LEFT JOIN categoria_docente cd ON cd.clave = p.categoria_docente_clave " +
                     "LEFT JOIN actividades_complementarias ac ON ac.clave = p.actividad_complementaria_clave " +
                     "LEFT JOIN (" +
                     "    SELECT pm.profesor_id, SUM(m.horas_semanales) AS total_horas" +
                     "    FROM profesor_materia pm" +
                     "    JOIN materias m ON pm.materia_id = m.id_materia" +
                     "    GROUP BY pm.profesor_id" +
                     ") pm_totales ON p.id_profesor = pm_totales.profesor_id " +
                     "ORDER BY p.id_profesor ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ProfesorSlot prof = new ProfesorSlot(
                    rs.getInt("id_profesor"),
                    rs.getString("rfc"),
                    rs.getString("nombre") + " " + rs.getString("apellidos"),
                    rs.getInt("horas_max_plaza"),
                    rs.getInt("horas_asignadas_reales"),
                    rs.getString("actividad_complementaria_clave"),
                    rs.getString("actividad_nombre"),
                    rs.getInt("actividad_horas"),
                    rs.getInt("actividad_tope"),
                    new HashSet<>(),
                    new HashMap<>()
                );
                ctx.catalogo.add(prof);
                idx.put(prof.id(), prof);
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT profesor_id, materia_id, grupo_id FROM profesor_materia");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ProfesorSlot prof = idx.get(rs.getInt("profesor_id"));
                if (prof == null) continue;

                int matId = rs.getInt("materia_id");
                prof.materias().add(matId);

                int grupoId = rs.getInt("grupo_id");
                if (!rs.wasNull()) {
                    prof.matGrupos().computeIfAbsent(matId, k -> new HashSet<>()).add(grupoId);
                } else {
                    prof.matGrupos().putIfAbsent(matId, new HashSet<>());
                }
            }
        }
    }

    private Map<Integer, Set<String>> cargarTodasDisponibilidades(Connection conn) throws SQLException {
        Map<Integer, Set<String>> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT profesor_id, dia, hora_inicio, hora_fin FROM profesor_disponibilidad ORDER BY profesor_id, dia, hora_inicio");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int pid = rs.getInt("profesor_id");
                String dia = normDia(rs.getString("dia"));
                int ini = horaInt(rs.getString("hora_inicio"));
                int fin = horaInt(rs.getString("hora_fin"));
                if (fin <= ini) continue;

                Set<String> slots = map.computeIfAbsent(pid, k -> new LinkedHashSet<>());
                for (int h = ini; h < fin; h++)
                    slots.add(dia + "|" + String.format("%02d:00", h));
            }
        }
        return map;
    }

    private Map<String, List<MateriaSlot>> cargarTodasMaterias(Connection conn) throws SQLException {
        Map<String, List<MateriaSlot>> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_materia, nombre, horas_semanales, id_especialidad, id_semestre FROM materias");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String key = rs.getInt("id_especialidad") + "|" + rs.getInt("id_semestre");
                map.computeIfAbsent(key, k -> new ArrayList<>())
                   .add(new MateriaSlot(rs.getInt("id_materia"), rs.getString("nombre"), rs.getInt("horas_semanales")));
            }
        }
        return map;
    }

    private List<GrupoSlot> cargarGrupos(Connection conn) throws SQLException {
        List<GrupoSlot> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, codigo, turno, especialidad_id, semestre FROM grupos ORDER BY especialidad_id, semestre, codigo");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new GrupoSlot(rs.getInt("id"), rs.getString("codigo"), rs.getString("turno"), rs.getInt("especialidad_id"), rs.getInt("semestre")));
        }
        return lista;
    }

    /** Suma las horas_semanales de todas las materias que le tocan a este grupo
     *  (segun su especialidad+semestre), para poder ordenar los grupos por carga
     *  total antes de procesarlos. */
    private int totalHorasGrupo(GrupoSlot g, Contexto ctx) {
        String clave = g.especialidadId() + "|" + g.semestre();
        return ctx.materiasCache.getOrDefault(clave, Collections.emptyList())
                  .stream().mapToInt(MateriaSlot::horasSemanales).sum();
    }

    private void procesarGrupo(GrupoSlot grupo, int idx, int total, Contexto ctx) {
        System.out.printf("%n  [%d/%d] Grupo: %-18s | Turno: %-10s | Sem %d%n", idx, total, grupo.codigo(), grupo.turno(), grupo.semestre());

        ctx.inicializarGrupo(grupo.codigo());
        String clave = grupo.especialidadId() + "|" + grupo.semestre();
        List<MateriaSlot> materias = new ArrayList<>(ctx.materiasCache.getOrDefault(clave, Collections.emptyList()));

        materias.sort(Comparator.comparingInt(MateriaSlot::horasSemanales).reversed());
        String[] horas = "Matutino".equalsIgnoreCase(grupo.turno()) ? HORAS_MAT : HORAS_VES;

        for (MateriaSlot mat : materias)
            procesarMateria(mat, grupo, horas, ctx);
    }

    private void procesarMateria(MateriaSlot mat, GrupoSlot grupo, String[] horas, Contexto ctx) {
        List<ProfesorSlot> ordenado = new ArrayList<>(ctx.catalogo);
        ordenado.sort(Comparator.comparingInt((ProfesorSlot p) -> ctx.horasUsadas(p.id())).thenComparingInt(ProfesorSlot::id));

        ProfesorSlot prof = elegirProfesor(mat, grupo, horas, ordenado, ctx);
        if (prof == null) {
            System.out.printf("      !! SIN PROFESOR para '%s'%n", mat.nombre());
            return;
        }

        Map<String, List<String>> cntDia = new HashMap<>();
        int rest = mat.horasSemanales();

        rest = asignarBloques(prof, mat, grupo, horas, rest, cntDia, 1, ctx);

        if (rest > 0)
            rest = asignarBloques(prof, mat, grupo, horas, rest, cntDia, MAX_POR_DIA, ctx);

        System.out.printf("      %-35s | %d/%d hrs | Prof: %s%n", mat.nombre(), mat.horasSemanales() - rest, mat.horasSemanales(), prof.nombre());
        if (rest > 0)
            System.out.printf("        >> INCOMPLETA: faltan %d hrs%n", rest);
    }

    // Valida contra prof.limiteClases() -- su carga REAL de profesor_materia,
    // nunca por encima de lo que su plaza autoriza (ver ProfesorSlot.limiteClases()).
    private ProfesorSlot elegirProfesor(MateriaSlot mat, GrupoSlot grupo, String[] horas, List<ProfesorSlot> ordenado, Contexto ctx) {
        ProfesorSlot fallback = null;

        for (ProfesorSlot prof : ordenado) {

            if (!prof.materias().contains(mat.id())) continue;

            Set<Integer> gruposPermitidos = prof.matGrupos().get(mat.id());
            if (gruposPermitidos != null && !gruposPermitidos.isEmpty() && !gruposPermitidos.contains(grupo.id())) continue;

            // Si ya cumplio su carga REAL asignada (profesor_materia), se omite -
            // sin importar cuanta disponibilidad u horas de plaza le queden libres.
            if (ctx.horasUsadas(prof.id()) >= prof.limiteClases()) continue;

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

            if (ctx.vecesEnGrupo(prof.id(), grupo.codigo()) == 0) return prof;
            if (fallback == null) fallback = prof;
        }

        return fallback;
    }

    private int asignarBloques(ProfesorSlot prof, MateriaSlot mat, GrupoSlot grupo, String[] horas, int restantes, Map<String, List<String>> cntDia, int maxPorDia, Contexto ctx) {
        Set<String> disp     = ctx.disponibilidad(prof.id());
        Set<String> ocupProf = ctx.slotsProf(prof.id());
        Set<String> ocupGrup = ctx.slotsGrupo(grupo.codigo());
        Set<String> horasSet = new HashSet<>(Arrays.asList(horas));
        Random rng = new Random();
        int diaInicio = 0;

        // Tope real de clases: prof.limiteClases() (nunca prof.horasMaxPlaza()).
        while (restantes > 0 && ctx.horasUsadas(prof.id()) < prof.limiteClases()) {
            boolean ok = false;

            for (int i = 0; i < DIAS.length; i++) {
                int dx = (diaInicio + i) % DIAS.length;
                String dia = DIAS[dx];

                List<String> yaEnEsteDia = cntDia.getOrDefault(dia, Collections.emptyList());
                if (yaEnEsteDia.size() >= maxPorDia) continue;

                String horaElegida = null;

                if (yaEnEsteDia.isEmpty()) {
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
                    busqueda:
                    for (String yaHora : yaEnEsteDia) {
                        int h = horaInt(yaHora);
                        for (int delta : new int[]{-1, +1}) {
                            String candidata = String.format("%02d:00", h + delta);
                            if (!horasSet.contains(candidata)) continue;
                            String c = dia + "|" + candidata;
                            if (disp.contains(c) && !ocupProf.contains(c) && !ocupGrup.contains(c)) {
                                horaElegida = candidata;
                                break busqueda;
                            }
                        }
                    }
                }

                if (horaElegida == null) continue;

                String slot = dia + "|" + horaElegida;
                ctx.marcarAsignado(prof.id(), grupo.codigo(), slot);
                cntDia.computeIfAbsent(dia, k -> new ArrayList<>()).add(horaElegida);

                ctx.acumularBloque(
                    new BloqueProfesor(prof.rfc(), prof.nombre(), mat.nombre(), grupo.codigo(), dia, horaElegida),
                    new BloqueGrupo(grupo.codigo(), mat.nombre(), prof.nombre(), dia, horaElegida)
                );

                restantes--;
                diaInicio = (dx + 1) % DIAS.length;
                ok = true;
                break;
            }

            if (!ok) break;
        }

        return restantes;
    }

    // Rellena huecos de plaza (horasMaxPlaza) con actividades complementarias,
    // consolidando por dia y por cercania a bloques ya ocupados.
    private void asignarActividadesComplementarias(Contexto ctx) {
        String[] todasLasHoras = new String[HORAS_MAT.length + HORAS_VES.length];
        System.arraycopy(HORAS_MAT, 0, todasLasHoras, 0, HORAS_MAT.length);
        System.arraycopy(HORAS_VES, 0, todasLasHoras, HORAS_MAT.length, HORAS_VES.length);

        for (ProfesorSlot prof : ctx.catalogo) {
            if (prof.actividadClave() == null) continue;

            // Aqui SI se usa horasMaxPlaza (no limiteClases): la actividad rellena
            // el resto de la PLAZA, mas alla de lo que profesor_materia le haya
            // asignado en clases reales.
            int huecoPlaza = prof.horasMaxPlaza() - ctx.horasUsadas(prof.id());
            if (huecoPlaza <= 0) continue;

            int aAsignar = Math.min(huecoPlaza, Math.min(prof.actividadHoras(), prof.actividadTope()));
            if (aAsignar <= 0) continue;

            Set<String> disp = ctx.disponibilidad(prof.id());
            Set<String> ocup = ctx.slotsProf(prof.id());
            int asignadas = 0;

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
                    ocup.add(slot);
                    ctx.acumularBloqueActividad(
                        new BloqueProfesor(prof.rfc(), prof.nombre(), prof.actividadNombre(), "(Actividad)", dia, hora)
                    );
                    asignadas++;
                }
            }

            if (asignadas > 0) {
                System.out.printf("      + %-30s | %d hrs de '%s' (actividad complementaria)%n",
                                  prof.nombre(), asignadas, prof.actividadNombre());
            }
        }
    }

    private int contarHorasOcupadasDia(Set<String> ocup, String dia, String[] todasLasHoras) {
        int n = 0;
        for (String hora : todasLasHoras) if (ocup.contains(dia + "|" + hora)) n++;
        return n;
    }

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

    private void flushBloques(Connection conn, Contexto ctx) throws SQLException {
        if (ctx.pendingProf.isEmpty()) return;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO horario_profesores(rfc_profesor,nombre_profesor,materia,grupo,dia,hora) VALUES(?,?,?,?,?,?)")) {
            for (BloqueProfesor bp : ctx.pendingProf) {
                ps.setString(1, bp.rfc());
                ps.setString(2, bp.nombreProfesor());
                ps.setString(3, bp.materia());
                ps.setString(4, bp.grupo());
                ps.setString(5, bp.dia());
                ps.setString(6, bp.hora());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO horario_grupos(grupo,materia,profesor,dia,hora) VALUES(?,?,?,?,?)")) {
            for (BloqueGrupo bg : ctx.pendingGrupo) {
                ps.setString(1, bg.grupo());
                ps.setString(2, bg.materia());
                ps.setString(3, bg.profesor());
                ps.setString(4, bg.dia());
                ps.setString(5, bg.hora());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        System.out.printf("  %d bloques persistidos en batch.%n", ctx.pendingProf.size());
    }

    private void persistirHorarioGeneral(Connection conn, Map<String, Map<String, String>> hGeneral) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE horario_general SET lunes=?, martes=?, miercoles=?, jueves=?, viernes=? WHERE hora=?")) {
            for (Map.Entry<String, Map<String, String>> e : hGeneral.entrySet()) {
                Map<String, String> dias = e.getValue();
                ps.setString(1, dias.getOrDefault("Lunes",     ""));
                ps.setString(2, dias.getOrDefault("Martes",    ""));
                ps.setString(3, dias.getOrDefault("Miercoles", ""));
                ps.setString(4, dias.getOrDefault("Jueves",    ""));
                ps.setString(5, dias.getOrDefault("Viernes",   ""));
                ps.setString(6, e.getKey());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        System.out.printf("  horario_general actualizado (%d horas con clases).%n", hGeneral.size());
    }

    private int horaInt(String h) {
        try { return Integer.parseInt(h.split(":")[0]); } catch (Exception e) { return 0; }
    }

    private String normDia(String dia) {
        if (dia == null) return "";
        String sinTildes = Normalizer.normalize(dia.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
        return DIA_NORM.getOrDefault(sinTildes, dia.trim());
    }
}