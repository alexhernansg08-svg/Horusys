--
-- PostgreSQL database dump
--

-- Dumped from database version 13.20
-- Dumped by pg_dump version 13.20

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;
-- L-3: Stored procedure eliminada (dead code - nunca llamada desde Java)

-- L-3: Stored procedure eliminada (dead code - nunca llamada desde Java)

-- L-3: Stored procedure eliminada (dead code - nunca llamada desde Java)

-- L-3: Stored procedure eliminada (dead code - nunca llamada desde Java)

-- L-3: Stored procedure eliminada (dead code - nunca llamada desde Java)

-- L-3: Stored procedure eliminada (dead code - nunca llamada desde Java)

--
-- Name: horario_grupo_tabla(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.horario_grupo_tabla(codigo_grupo text, p_usuario_id integer DEFAULT NULL) RETURNS TABLE("HORA" text, "LUNES" text, "MARTES" text, "MIERCOLES" text, "JUEVES" text, "VIERNES" text)
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN QUERY
    SELECT 
        TO_CHAR(h.hora_inicio, 'HH24:MI')::TEXT AS "HORA",
        MAX(CASE WHEN h.dia_semana = 'Lunes'     THEN m.codigo || ' (' || INITCAP(LEFT(p.nombre,1)) || '. ' || p.apellido || ')' END)::TEXT AS "LUNES",
        MAX(CASE WHEN h.dia_semana = 'Martes'    THEN m.codigo || ' (' || INITCAP(LEFT(p.nombre,1)) || '. ' || p.apellido || ')' END)::TEXT AS "MARTES",
        MAX(CASE WHEN h.dia_semana = 'Miercoles' THEN m.codigo || ' (' || INITCAP(LEFT(p.nombre,1)) || '. ' || p.apellido || ')' END)::TEXT AS "MIERCOLES",
        MAX(CASE WHEN h.dia_semana = 'Jueves'    THEN m.codigo || ' (' || INITCAP(LEFT(p.nombre,1)) || '. ' || p.apellido || ')' END)::TEXT AS "JUEVES",
        MAX(CASE WHEN h.dia_semana = 'Viernes'   THEN m.codigo || ' (' || INITCAP(LEFT(p.nombre,1)) || '. ' || p.apellido || ')' END)::TEXT AS "VIERNES"
    FROM horario_generado h
    JOIN materias m ON h.materia_id = m.id
    JOIN profesores p ON h.profesor_id = p.id
    JOIN grupos g ON h.grupo_id = g.id
    WHERE g.codigo = codigo_grupo
      AND (p_usuario_id IS NULL OR g.usuario_id = p_usuario_id)
    GROUP BY h.hora_inicio
    ORDER BY h.hora_inicio;
END;
$$;


ALTER FUNCTION public.horario_grupo_tabla(codigo_grupo text) OWNER TO postgres;

--
-- Name: horario_por_grupo(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.horario_por_grupo(codigo_grupo text, p_usuario_id integer DEFAULT NULL) RETURNS TABLE(grupo text, hora text, dia_semana text, materia text, profesor text)
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN QUERY
    SELECT 
        g.codigo::TEXT AS grupo,
        TO_CHAR(h.hora_inicio, 'HH24:MI') AS hora,
        h.dia_semana::TEXT,
        m.codigo::TEXT AS materia,
        INITCAP(LEFT(p.nombre,1)) || '. ' || p.apellido AS profesor
    FROM horario_generado h
    JOIN grupos g ON h.grupo_id = g.id
    JOIN materias m ON h.materia_id = m.id
    JOIN profesores p ON h.profesor_id = p.id
    WHERE g.codigo = codigo_grupo
      AND (p_usuario_id IS NULL OR g.usuario_id = p_usuario_id)
    ORDER BY 
        CASE h.dia_semana 
            WHEN 'Lunes'     THEN 1
            WHEN 'Martes'    THEN 2
            WHEN 'Miercoles' THEN 3
            WHEN 'Jueves'    THEN 4
            WHEN 'Viernes'   THEN 5
        END,
        h.hora_inicio;
END;
$$;


ALTER FUNCTION public.horario_por_grupo(codigo_grupo text) OWNER TO postgres;

--
-- Name: horario_profesor_tabla(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.horario_profesor_tabla(rfc_profesor text) RETURNS TABLE("HORA" text, "LUNES" text, "MARTES" text, "MIERCOLES" text, "JUEVES" text, "VIERNES" text)
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN QUERY
    SELECT 
        TO_CHAR(h.hora_inicio, 'HH24:MI')::TEXT AS "HORA",
        MAX(CASE WHEN h.dia_semana = 'Lunes'     THEN g.codigo || ' - ' || m.codigo END)::TEXT AS "LUNES",
        MAX(CASE WHEN h.dia_semana = 'Martes'    THEN g.codigo || ' - ' || m.codigo END)::TEXT AS "MARTES",
        MAX(CASE WHEN h.dia_semana = 'Miercoles' THEN g.codigo || ' - ' || m.codigo END)::TEXT AS "MIERCOLES",
        MAX(CASE WHEN h.dia_semana = 'Jueves'    THEN g.codigo || ' - ' || m.codigo END)::TEXT AS "JUEVES",
        MAX(CASE WHEN h.dia_semana = 'Viernes'   THEN g.codigo || ' - ' || m.codigo END)::TEXT AS "VIERNES"
    FROM horario_generado h
    JOIN grupos g ON h.grupo_id = g.id
    JOIN materias m ON h.materia_id = m.id
    JOIN profesores p ON h.profesor_id = p.id
    WHERE p.rfc = rfc_profesor
    GROUP BY h.hora_inicio
    ORDER BY h.hora_inicio;
END;
$$;


ALTER FUNCTION public.horario_profesor_tabla(rfc_profesor text) OWNER TO postgres;

--
-- Name: validar_horario_completo(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.validar_horario_completo() RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE
    choques INT := 0;
    huecos INT := 0;
BEGIN
    -- CONTAR CHOQUES (mismo grupo en dos dias)
    SELECT COUNT(*) INTO choques
    FROM horario_general
    WHERE 
        (lunes LIKE '%1A%' AND martes LIKE '%1A%') OR
        (lunes LIKE '%1B%' AND miercoles LIKE '%1B%') OR
        (martes LIKE '%2A%' AND miercoles LIKE '%2A%') OR
        (jueves LIKE '%3A%' AND viernes LIKE '%3A%');

    -- CONTAR HUECOS (celdas vacias fuera de descanso)
    SELECT COUNT(*) INTO huecos
    FROM horario_general
    WHERE 
        hora NOT LIKE '%DESCANSO%' AND
        (lunes IS NULL OR lunes = '' OR
         martes IS NULL OR martes = '' OR
         miercoles IS NULL OR miercoles = '' OR
         jueves IS NULL OR jueves = '' OR
         viernes IS NULL OR viernes = '');

    -- SIEMPRE DEVOLVER BOOLEANO
    RETURN (choques = 0 AND huecos = 0);
END;
$$;


ALTER FUNCTION public.validar_horario_completo() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: aula; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.aula (
    id_aula integer NOT NULL,
    numero character varying(20) NOT NULL,
    capacidad smallint DEFAULT 30 NOT NULL,
    tipo character varying(50) DEFAULT 'Salon'::character varying NOT NULL
);


ALTER TABLE public.aula OWNER TO postgres;

--
-- Name: aula_id_aula_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.aula_id_aula_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.aula_id_aula_seq OWNER TO postgres;

--
-- Name: aula_id_aula_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.aula_id_aula_seq OWNED BY public.aula.id_aula;


--
-- Name: bitacora; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bitacora (
    id_bitacora integer NOT NULL,
    id_usuario integer,  -- FIX: nullable para permitir SET NULL al borrar usuario (conserva historial)
    tabla character varying(50) NOT NULL,
    id_registro integer,
    accion character varying(30) NOT NULL,
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    detalle text
);


ALTER TABLE public.bitacora OWNER TO postgres;

--
-- Name: bitacora_id_bitacora_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.bitacora_id_bitacora_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.bitacora_id_bitacora_seq OWNER TO postgres;

--
-- Name: bitacora_id_bitacora_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.bitacora_id_bitacora_seq OWNED BY public.bitacora.id_bitacora;


--
-- Name: ciclo; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ciclo (
    id_ciclo integer NOT NULL,
    nombre character varying(20) NOT NULL,
    numero character varying(10) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    activo boolean DEFAULT false NOT NULL,
    CONSTRAINT ck_ciclo_fecha CHECK ((fecha_inicio < fecha_fin))
);


ALTER TABLE public.ciclo OWNER TO postgres;

--
-- Name: ciclo_id_ciclo_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ciclo_id_ciclo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ciclo_id_ciclo_seq OWNER TO postgres;

--
-- Name: ciclo_id_ciclo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.ciclo_id_ciclo_seq OWNED BY public.ciclo.id_ciclo;


--
-- Name: clave_sistema; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.clave_sistema (
    id_clave integer NOT NULL,
    clave_hash character varying(255) NOT NULL,
    activa boolean DEFAULT true NOT NULL,
    fecha_cambio timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    id_director integer NOT NULL
);


ALTER TABLE public.clave_sistema OWNER TO postgres;

--
-- Name: clave_sistema_id_clave_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.clave_sistema_id_clave_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.clave_sistema_id_clave_seq OWNER TO postgres;

--
-- Name: clave_sistema_id_clave_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.clave_sistema_id_clave_seq OWNED BY public.clave_sistema.id_clave;


--
-- Name: detalle_horario; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.detalle_horario (
    id_detalle integer NOT NULL,
    id_horario integer NOT NULL,
    id_materia integer NOT NULL,
    id_profesor integer NOT NULL,
    id_aula integer NOT NULL,
    dia_semana character varying(20) NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    CONSTRAINT ck_dh_horas CHECK ((hora_inicio < hora_fin))
);


ALTER TABLE public.detalle_horario OWNER TO postgres;

--
-- Name: detalle_horario_id_detalle_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.detalle_horario_id_detalle_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.detalle_horario_id_detalle_seq OWNER TO postgres;

--
-- Name: detalle_horario_id_detalle_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.detalle_horario_id_detalle_seq OWNED BY public.detalle_horario.id_detalle;


--
-- Name: especialidades; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.especialidades (
    id integer NOT NULL,
    nombre character varying(100) NOT NULL,
    codigo character varying(20) NOT NULL
);


ALTER TABLE public.especialidades OWNER TO postgres;

--
-- Name: especialidades_id_seq1; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.especialidades_id_seq1
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.especialidades_id_seq1 OWNER TO postgres;

--
-- Name: especialidades_id_seq1; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.especialidades_id_seq1 OWNED BY public.especialidades.id;


--
-- Name: grupos; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.grupos (
    id integer NOT NULL,
    nombre character varying(100) NOT NULL,
    codigo character varying(20) NOT NULL,
    especialidad_id integer,
    semestre integer NOT NULL,
    turno character varying(20) NOT NULL,
    especialidad character varying(50),
    capacidad integer DEFAULT 40
);


ALTER TABLE public.grupos OWNER TO postgres;

--
-- Name: grupos_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.grupos_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.grupos_id_seq OWNER TO postgres;

--
-- Name: grupos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.grupos_id_seq OWNED BY public.grupos.id;


--
-- Name: horario; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.horario (
    id_horario integer NOT NULL,
    id_ciclo integer NOT NULL,
    id_grupo integer NOT NULL,
    tipo character varying(20) DEFAULT 'General'::character varying NOT NULL,
    estatus character varying(20) DEFAULT 'Borrador'::character varying NOT NULL
);


ALTER TABLE public.horario OWNER TO postgres;

--
-- Name: horario_general; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.horario_general (
    id integer NOT NULL,
    hora character varying(10) NOT NULL,
    lunes text DEFAULT ''::text,
    martes text DEFAULT ''::text,
    miercoles text DEFAULT ''::text,
    jueves text DEFAULT ''::text,
    viernes text DEFAULT ''::text
);


ALTER TABLE public.horario_general OWNER TO postgres;

--
-- Name: horario_general_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.horario_general_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.horario_general_id_seq OWNER TO postgres;

--
-- Name: horario_general_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.horario_general_id_seq OWNED BY public.horario_general.id;


--
-- Name: horario_grupos; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.horario_grupos (
    id integer NOT NULL,
    grupo character varying(20) NOT NULL,
    materia text,
    profesor text,
    dia character varying(15),
    hora character varying(10)
);


ALTER TABLE public.horario_grupos OWNER TO postgres;

--
-- Name: horario_grupos_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.horario_grupos_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.horario_grupos_id_seq OWNER TO postgres;

--
-- Name: horario_grupos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.horario_grupos_id_seq OWNED BY public.horario_grupos.id;


--
-- Name: horario_id_horario_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.horario_id_horario_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.horario_id_horario_seq OWNER TO postgres;

--
-- Name: horario_id_horario_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.horario_id_horario_seq OWNED BY public.horario.id_horario;


--
-- Name: horario_profesores; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.horario_profesores (
    id integer NOT NULL,
    rfc_profesor character varying(20) NOT NULL,
    nombre_profesor text,
    materia text,
    grupo text,
    dia character varying(15),
    hora character varying(10)
);


ALTER TABLE public.horario_profesores OWNER TO postgres;

--
-- Name: horario_profesores_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.horario_profesores_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.horario_profesores_id_seq OWNER TO postgres;

--
-- Name: horario_profesores_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.horario_profesores_id_seq OWNED BY public.horario_profesores.id;


--
-- Name: materia_grupos; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.materia_grupos (
    materia_id integer NOT NULL,
    grupo_id integer NOT NULL
);


ALTER TABLE public.materia_grupos OWNER TO postgres;

--
-- Name: materias; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.materias (
    id_materia integer NOT NULL,
    id_especialidad integer NOT NULL,
    id_semestre integer NOT NULL,
    nombre character varying(100) NOT NULL,
    clave character varying(10) NOT NULL,
    horas_semanales integer DEFAULT 6
);


ALTER TABLE public.materias OWNER TO postgres;

--
-- Name: materias_id_materia_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.materias_id_materia_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.materias_id_materia_seq OWNER TO postgres;

--
-- Name: materias_id_materia_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.materias_id_materia_seq OWNED BY public.materias.id_materia;


--
-- Name: plan_estudio; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.plan_estudio (
    id_plan integer NOT NULL,
    id_especialidad integer NOT NULL,
    nombre character varying(100) NOT NULL,
    horas_asignadas integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.plan_estudio OWNER TO postgres;

--
-- Name: plan_estudio_id_plan_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.plan_estudio_id_plan_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.plan_estudio_id_plan_seq OWNER TO postgres;

--
-- Name: plan_estudio_id_plan_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.plan_estudio_id_plan_seq OWNED BY public.plan_estudio.id_plan;


--
-- Name: plan_estudio_materia; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.plan_estudio_materia (
    id integer NOT NULL,
    id_plan integer NOT NULL,
    id_materia integer NOT NULL
);


ALTER TABLE public.plan_estudio_materia OWNER TO postgres;

--
-- Name: plan_estudio_materia_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.plan_estudio_materia_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.plan_estudio_materia_id_seq OWNER TO postgres;

--
-- Name: plan_estudio_materia_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.plan_estudio_materia_id_seq OWNED BY public.plan_estudio_materia.id;


--
-- Name: profesor; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.profesor (
    id_profesor integer NOT NULL,
    nombre character varying(80) NOT NULL,
    apellidos character varying(80) NOT NULL,
    email character varying(100) NOT NULL,
    telefono character varying(20),
    pregrado character varying(100),
    fecha_ingreso date NOT NULL,
    prioridad smallint DEFAULT 99 NOT NULL,
    horas_academicas integer DEFAULT 20,
    rfc character varying(13)
);


ALTER TABLE public.profesor OWNER TO postgres;

--
-- Name: profesor_disponibilidad; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.profesor_disponibilidad (
    id integer NOT NULL,
    profesor_id integer,
    dia character varying(20) NOT NULL,
    hora_inicio character varying(5) NOT NULL,
    hora_fin character varying(5) NOT NULL
);


ALTER TABLE public.profesor_disponibilidad OWNER TO postgres;

--
-- Name: profesor_disponibilidad_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.profesor_disponibilidad_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.profesor_disponibilidad_id_seq OWNER TO postgres;

--
-- Name: profesor_disponibilidad_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.profesor_disponibilidad_id_seq OWNED BY public.profesor_disponibilidad.id;


--
-- Name: profesor_materia; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.profesor_materia (
    id integer NOT NULL,
    profesor_id integer NOT NULL,
    materia_id integer NOT NULL,
    especialidad_id integer,
    fecha_asignacion date DEFAULT CURRENT_DATE
);


ALTER TABLE public.profesor_materia OWNER TO postgres;

--
-- Name: profesor_materia_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.profesor_materia_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.profesor_materia_id_seq OWNER TO postgres;

--
-- Name: profesor_materia_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.profesor_materia_id_seq OWNED BY public.profesor_materia.id;


--
-- Name: profesores_id_profesor_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.profesores_id_profesor_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.profesores_id_profesor_seq OWNER TO postgres;

--
-- Name: profesores_id_profesor_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.profesores_id_profesor_seq OWNED BY public.profesor.id_profesor;


--
-- Name: semestre; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.semestre (
    id_semestre integer NOT NULL,
    id_plan integer NOT NULL,
    numero smallint NOT NULL,
    descripcion character varying(100)
);


ALTER TABLE public.semestre OWNER TO postgres;

--
-- Name: semestre_id_semestre_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.semestre_id_semestre_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.semestre_id_semestre_seq OWNER TO postgres;

--
-- Name: semestre_id_semestre_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.semestre_id_semestre_seq OWNED BY public.semestre.id_semestre;


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.usuarios (
    id integer NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(64) NOT NULL,
    email character varying(100) NOT NULL,
    fecha_registro timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.usuarios OWNER TO postgres;

--
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.usuarios_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.usuarios_id_seq OWNER TO postgres;

--
-- Name: usuarios_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.usuarios_id_seq OWNED BY public.usuarios.id;


--
-- Name: aula id_aula; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.aula ALTER COLUMN id_aula SET DEFAULT nextval('public.aula_id_aula_seq'::regclass);


--
-- Name: bitacora id_bitacora; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bitacora ALTER COLUMN id_bitacora SET DEFAULT nextval('public.bitacora_id_bitacora_seq'::regclass);


--
-- Name: ciclo id_ciclo; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ciclo ALTER COLUMN id_ciclo SET DEFAULT nextval('public.ciclo_id_ciclo_seq'::regclass);


--
-- Name: clave_sistema id_clave; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clave_sistema ALTER COLUMN id_clave SET DEFAULT nextval('public.clave_sistema_id_clave_seq'::regclass);


--
-- Name: detalle_horario id_detalle; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.detalle_horario ALTER COLUMN id_detalle SET DEFAULT nextval('public.detalle_horario_id_detalle_seq'::regclass);


--
-- Name: especialidades id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.especialidades ALTER COLUMN id SET DEFAULT nextval('public.especialidades_id_seq1'::regclass);


--
-- Name: grupos id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.grupos ALTER COLUMN id SET DEFAULT nextval('public.grupos_id_seq'::regclass);


--
-- Name: horario id_horario; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.horario ALTER COLUMN id_horario SET DEFAULT nextval('public.horario_id_horario_seq'::regclass);


--
-- Name: horario_general id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.horario_general ALTER COLUMN id SET DEFAULT nextval('public.horario_general_id_seq'::regclass);


--
-- Name: horario_grupos id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.horario_grupos ALTER COLUMN id SET DEFAULT nextval('public.horario_grupos_id_seq'::regclass);


--
-- Name: horario_profesores id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.horario_profesores ALTER COLUMN id SET DEFAULT nextval('public.horario_profesores_id_seq'::regclass);


--
-- Name: materias id_materia; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.materias ALTER COLUMN id_materia SET DEFAULT nextval('public.materias_id_materia_seq'::regclass);


--
-- Name: plan_estudio id_plan; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plan_estudio ALTER COLUMN id_plan SET DEFAULT nextval('public.plan_estudio_id_plan_seq'::regclass);


--
-- Name: plan_estudio_materia id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plan_estudio_materia ALTER COLUMN id SET DEFAULT nextval('public.plan_estudio_materia_id_seq'::regclass);


--
-- Name: profesor id_profesor; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profesor ALTER COLUMN id_profesor SET DEFAULT nextval('public.profesores_id_profesor_seq'::regclass);


--
-- Name: profesor_disponibilidad id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profesor_disponibilidad ALTER COLUMN id SET DEFAULT nextval('public.profesor_disponibilidad_id_seq'::regclass);


--
-- Name: profesor_materia id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profesor_materia ALTER COLUMN id SET DEFAULT nextval('public.profesor_materia_id_seq'::regclass);


--
-- Name: semestre id_semestre; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.semestre ALTER COLUMN id_semestre SET DEFAULT nextval('public.semestre_id_semestre_seq'::regclass);


--
-- Name: usuarios id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuarios ALTER COLUMN id SET DEFAULT nextval('public.usuarios_id_seq'::regclass);


--
-- Data for Name: aula; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.aula (id_aula, numero, capacidad, tipo) FROM stdin;
1	A101	35	Salon
2	A102	30	Salon
3	LAB1	25	Laboratorio
4	AUD1	80	Auditorio
\.


--
-- Data for Name: bitacora; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bitacora (id_bitacora, id_usuario, tabla, id_registro, accion, fecha, detalle) FROM stdin;
\.


--
-- Data for Name: ciclo; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ciclo (id_ciclo, nombre, numero, fecha_inicio, fecha_fin, activo) FROM stdin;
1	Ciclo 2026-1	2026-1	2026-01-20	2026-06-30	t
\.


--
-- Data for Name: clave_sistema; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.clave_sistema (id_clave, clave_hash, activa, fecha_cambio, id_director) FROM stdin;
1	$2a$10$ClaveMaestraSeguraParaElSistema2026	t	2026-04-05 20:46:15.674203	1
2	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-07 06:17:02.087944	1
3	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-07 06:38:39.993998	2
4	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-07 08:43:24.749879	3
5	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-08 18:51:42.735745	4
6	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-09 08:58:45.265908	5
7	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-09 10:17:04.76333	6
8	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-09 10:25:00.571348	7
9	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-09 10:32:42.267211	8
10	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-09 10:40:32.766751	9
11	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-09 10:47:20.175519	10
12	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-09 10:50:38.435056	11
13	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-09 10:54:54.270746	12
14	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-09 12:26:55.292975	13
15	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-09 12:51:52.753079	14
16	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-10 17:18:28.180635	15
17	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-10 18:44:42.577324	16
18	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-10 21:09:28.083032	17
19	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-10 21:13:54.213267	18
20	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-10 21:17:04.921335	19
21	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-10 21:22:53.508186	20
22	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-10 21:31:57.186911	21
23	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-11 18:54:37.364184	22
24	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-11 21:23:52.318545	23
25	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-12 12:09:56.14858	24
26	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-12 12:18:37.220795	25
27	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-14 16:39:23.840433	26
28	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-14 21:58:51.854742	27
29	47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=	t	2026-04-15 11:18:57.013673	28
30	BcOHtVeFvfrItajyZD0nTbB4BZxsEcb9IEGpUf+1P5Q=	t	2026-04-16 13:25:18.469628	29
\.


--
-- Data for Name: detalle_horario; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.detalle_horario (id_detalle, id_horario, id_materia, id_profesor, id_aula, dia_semana, hora_inicio, hora_fin) FROM stdin;
\.


--
-- Data for Name: especialidades; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.especialidades (id, nombre, codigo) FROM stdin;
1	Programacion	PROG
2	Mecanica Industrial	MECA
3	Electricidad	ELEC
4	Mecatronica	MECAT
5	Ciberseguridad	CIBER
6	Gestion e Innovacion Turistica	GIT
7	Inteligencia Artificial	IA
\.


--
-- Data for Name: grupos; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.grupos (id, nombre, codigo, especialidad_id, semestre, turno, especialidad, capacidad) FROM stdin;
1	A	A-PROG-1	1	1	Matutino	\N	40
2	B	B-PROG-1	1	1	Matutino	\N	40
3	C	C-PROG-1	1	1	Matutino	\N	40
4	D	D-PROG-1	1	1	Matutino	\N	40
5	E	E-PROG-1	1	1	Matutino	\N	40
6	F	F-PROG-1	1	1	Matutino	\N	40
7	G	G-PROG-1	1	1	Matutino	\N	40
8	A	A-PROG-2	1	2	Matutino	\N	38
9	B	B-PROG-2	1	2	Matutino	\N	38
10	C	C-PROG-2	1	2	Matutino	\N	38
11	D	D-PROG-2	1	2	Matutino	\N	38
12	E	E-PROG-2	1	2	Matutino	\N	38
13	F	F-PROG-2	1	2	Matutino	\N	38
14	G	G-PROG-2	1	2	Matutino	\N	38
15	A	A-PROG-3	1	3	Matutino	\N	36
16	B	B-PROG-3	1	3	Matutino	\N	36
17	C	C-PROG-3	1	3	Matutino	\N	36
18	D	D-PROG-3	1	3	Matutino	\N	36
19	E	E-PROG-3	1	3	Matutino	\N	36
20	F	F-PROG-3	1	3	Matutino	\N	36
21	G	G-PROG-3	1	3	Matutino	\N	36
22	A	A-PROG-4	1	4	Matutino	\N	35
23	B	B-PROG-4	1	4	Matutino	\N	35
24	C	C-PROG-4	1	4	Matutino	\N	35
25	D	D-PROG-4	1	4	Matutino	\N	35
26	E	E-PROG-4	1	4	Matutino	\N	35
27	F	F-PROG-4	1	4	Matutino	\N	35
28	G	G-PROG-4	1	4	Matutino	\N	35
29	A	A-PROG-5	1	5	Matutino	\N	33
30	B	B-PROG-5	1	5	Matutino	\N	33
31	C	C-PROG-5	1	5	Matutino	\N	33
32	D	D-PROG-5	1	5	Matutino	\N	33
33	E	E-PROG-5	1	5	Matutino	\N	33
34	F	F-PROG-5	1	5	Matutino	\N	33
35	G	G-PROG-5	1	5	Matutino	\N	33
36	A	A-PROG-6	1	6	Matutino	\N	32
37	B	B-PROG-6	1	6	Matutino	\N	32
38	C	C-PROG-6	1	6	Matutino	\N	32
39	D	D-PROG-6	1	6	Matutino	\N	32
40	E	E-PROG-6	1	6	Matutino	\N	32
41	F	F-PROG-6	1	6	Matutino	\N	32
42	G	G-PROG-6	1	6	Matutino	\N	32
43	H	H-PROG-1	1	1	Vespertino	\N	40
44	I	I-PROG-1	1	1	Vespertino	\N	40
45	J	J-PROG-1	1	1	Vespertino	\N	40
46	K	K-PROG-1	1	1	Vespertino	\N	40
47	L	L-PROG-1	1	1	Vespertino	\N	40
48	H	H-PROG-2	1	2	Vespertino	\N	38
49	I	I-PROG-2	1	2	Vespertino	\N	38
50	J	J-PROG-2	1	2	Vespertino	\N	38
51	K	K-PROG-2	1	2	Vespertino	\N	38
52	L	L-PROG-2	1	2	Vespertino	\N	38
53	H	H-PROG-3	1	3	Vespertino	\N	36
54	I	I-PROG-3	1	3	Vespertino	\N	36
55	J	J-PROG-3	1	3	Vespertino	\N	36
56	K	K-PROG-3	1	3	Vespertino	\N	36
57	L	L-PROG-3	1	3	Vespertino	\N	36
58	H	H-PROG-4	1	4	Vespertino	\N	35
59	I	I-PROG-4	1	4	Vespertino	\N	35
60	J	J-PROG-4	1	4	Vespertino	\N	35
61	K	K-PROG-4	1	4	Vespertino	\N	35
62	L	L-PROG-4	1	4	Vespertino	\N	35
63	H	H-PROG-5	1	5	Vespertino	\N	33
64	I	I-PROG-5	1	5	Vespertino	\N	33
65	J	J-PROG-5	1	5	Vespertino	\N	33
66	K	K-PROG-5	1	5	Vespertino	\N	33
67	L	L-PROG-5	1	5	Vespertino	\N	33
68	H	H-PROG-6	1	6	Vespertino	\N	32
69	I	I-PROG-6	1	6	Vespertino	\N	32
70	J	J-PROG-6	1	6	Vespertino	\N	32
71	K	K-PROG-6	1	6	Vespertino	\N	32
72	L	L-PROG-6	1	6	Vespertino	\N	32
73	A	A-MECA-1	2	1	Matutino	\N	40
74	B	B-MECA-1	2	1	Matutino	\N	40
75	C	C-MECA-1	2	1	Matutino	\N	40
76	D	D-MECA-1	2	1	Matutino	\N	40
77	E	E-MECA-1	2	1	Matutino	\N	40
78	F	F-MECA-1	2	1	Matutino	\N	40
79	G	G-MECA-1	2	1	Matutino	\N	40
80	A	A-MECA-2	2	2	Matutino	\N	38
81	B	B-MECA-2	2	2	Matutino	\N	38
82	C	C-MECA-2	2	2	Matutino	\N	38
83	D	D-MECA-2	2	2	Matutino	\N	38
84	E	E-MECA-2	2	2	Matutino	\N	38
85	F	F-MECA-2	2	2	Matutino	\N	38
86	G	G-MECA-2	2	2	Matutino	\N	38
87	A	A-MECA-3	2	3	Matutino	\N	36
88	B	B-MECA-3	2	3	Matutino	\N	36
89	C	C-MECA-3	2	3	Matutino	\N	36
90	D	D-MECA-3	2	3	Matutino	\N	36
91	E	E-MECA-3	2	3	Matutino	\N	36
92	F	F-MECA-3	2	3	Matutino	\N	36
93	G	G-MECA-3	2	3	Matutino	\N	36
94	A	A-ELEC-1	3	1	Matutino	\N	40
95	B	B-ELEC-1	3	1	Matutino	\N	40
96	C	C-ELEC-1	3	1	Matutino	\N	40
97	D	D-ELEC-1	3	1	Matutino	\N	40
98	E	E-ELEC-1	3	1	Matutino	\N	40
99	F	F-ELEC-1	3	1	Matutino	\N	40
100	G	G-ELEC-1	3	1	Matutino	\N	40
101	A	A-ELEC-2	3	2	Matutino	\N	38
102	B	B-ELEC-2	3	2	Matutino	\N	38
103	C	C-ELEC-2	3	2	Matutino	\N	38
104	D	D-ELEC-2	3	2	Matutino	\N	38
105	E	E-ELEC-2	3	2	Matutino	\N	38
106	F	F-ELEC-2	3	2	Matutino	\N	38
107	G	G-ELEC-2	3	2	Matutino	\N	38
108	A	A-ELEC-3	3	3	Matutino	\N	36
109	B	B-ELEC-3	3	3	Matutino	\N	36
110	C	C-ELEC-3	3	3	Matutino	\N	36
111	D	D-ELEC-3	3	3	Matutino	\N	36
112	E	E-ELEC-3	3	3	Matutino	\N	36
113	F	F-ELEC-3	3	3	Matutino	\N	36
114	G	G-ELEC-3	3	3	Matutino	\N	36
115	H	H-ELEC-1	3	1	Vespertino	\N	40
116	I	I-ELEC-1	3	1	Vespertino	\N	40
117	J	J-ELEC-1	3	1	Vespertino	\N	40
118	K	K-ELEC-1	3	1	Vespertino	\N	40
119	L	L-ELEC-1	3	1	Vespertino	\N	40
120	H	H-ELEC-2	3	2	Vespertino	\N	38
121	I	I-ELEC-2	3	2	Vespertino	\N	38
122	J	J-ELEC-2	3	2	Vespertino	\N	38
123	K	K-ELEC-2	3	2	Vespertino	\N	38
124	L	L-ELEC-2	3	2	Vespertino	\N	38
125	H	H-ELEC-3	3	3	Vespertino	\N	36
126	I	I-ELEC-3	3	3	Vespertino	\N	36
127	J	J-ELEC-3	3	3	Vespertino	\N	36
128	K	K-ELEC-3	3	3	Vespertino	\N	36
129	L	L-ELEC-3	3	3	Vespertino	\N	36
130	A	A-MECAT-1	4	1	Matutino	\N	40
131	B	B-MECAT-1	4	1	Matutino	\N	40
132	C	C-MECAT-1	4	1	Matutino	\N	40
133	D	D-MECAT-1	4	1	Matutino	\N	40
134	E	E-MECAT-1	4	1	Matutino	\N	40
135	F	F-MECAT-1	4	1	Matutino	\N	40
136	G	G-MECAT-1	4	1	Matutino	\N	40
137	A	A-MECAT-2	4	2	Matutino	\N	38
138	B	B-MECAT-2	4	2	Matutino	\N	38
139	C	C-MECAT-2	4	2	Matutino	\N	38
140	D	D-MECAT-2	4	2	Matutino	\N	38
141	E	E-MECAT-2	4	2	Matutino	\N	38
142	F	F-MECAT-2	4	2	Matutino	\N	38
143	G	G-MECAT-2	4	2	Matutino	\N	38
144	A	A-MECAT-3	4	3	Matutino	\N	36
145	B	B-MECAT-3	4	3	Matutino	\N	36
146	C	C-MECAT-3	4	3	Matutino	\N	36
147	D	D-MECAT-3	4	3	Matutino	\N	36
148	E	E-MECAT-3	4	3	Matutino	\N	36
149	F	F-MECAT-3	4	3	Matutino	\N	36
150	G	G-MECAT-3	4	3	Matutino	\N	36
151	H	H-CIBER-1	5	1	Vespertino	\N	40
152	I	I-CIBER-1	5	1	Vespertino	\N	40
153	J	J-CIBER-1	5	1	Vespertino	\N	40
154	K	K-CIBER-1	5	1	Vespertino	\N	40
155	L	L-CIBER-1	5	1	Vespertino	\N	40
156	H	H-CIBER-2	5	2	Vespertino	\N	38
157	I	I-CIBER-2	5	2	Vespertino	\N	38
158	J	J-CIBER-2	5	2	Vespertino	\N	38
159	K	K-CIBER-2	5	2	Vespertino	\N	38
160	L	L-CIBER-2	5	2	Vespertino	\N	38
161	H	H-CIBER-3	5	3	Vespertino	\N	36
162	I	I-CIBER-3	5	3	Vespertino	\N	36
163	J	J-CIBER-3	5	3	Vespertino	\N	36
164	K	K-CIBER-3	5	3	Vespertino	\N	36
165	L	L-CIBER-3	5	3	Vespertino	\N	36
166	A	A-GIT-1	6	1	Matutino	\N	40
167	B	B-GIT-1	6	1	Matutino	\N	40
168	C	C-GIT-1	6	1	Matutino	\N	40
169	D	D-GIT-1	6	1	Matutino	\N	40
170	E	E-GIT-1	6	1	Matutino	\N	40
171	F	F-GIT-1	6	1	Matutino	\N	40
172	G	G-GIT-1	6	1	Matutino	\N	40
173	A	A-GIT-2	6	2	Matutino	\N	38
174	B	B-GIT-2	6	2	Matutino	\N	38
175	C	C-GIT-2	6	2	Matutino	\N	38
176	D	D-GIT-2	6	2	Matutino	\N	38
177	E	E-GIT-2	6	2	Matutino	\N	38
178	F	F-GIT-2	6	2	Matutino	\N	38
179	G	G-GIT-2	6	2	Matutino	\N	38
180	A	A-GIT-3	6	3	Matutino	\N	36
181	B	B-GIT-3	6	3	Matutino	\N	36
182	C	C-GIT-3	6	3	Matutino	\N	36
183	D	D-GIT-3	6	3	Matutino	\N	36
184	E	E-GIT-3	6	3	Matutino	\N	36
185	F	F-GIT-3	6	3	Matutino	\N	36
186	G	G-GIT-3	6	3	Matutino	\N	36
187	H	H-GIT-1	6	1	Vespertino	\N	40
188	I	I-GIT-1	6	1	Vespertino	\N	40
189	J	J-GIT-1	6	1	Vespertino	\N	40
190	K	K-GIT-1	6	1	Vespertino	\N	40
191	L	L-GIT-1	6	1	Vespertino	\N	40
192	H	H-GIT-2	6	2	Vespertino	\N	38
193	I	I-GIT-2	6	2	Vespertino	\N	38
194	J	J-GIT-2	6	2	Vespertino	\N	38
195	K	K-GIT-2	6	2	Vespertino	\N	38
196	L	L-GIT-2	6	2	Vespertino	\N	38
197	H	H-GIT-3	6	3	Vespertino	\N	36
198	I	I-GIT-3	6	3	Vespertino	\N	36
199	J	J-GIT-3	6	3	Vespertino	\N	36
200	K	K-GIT-3	6	3	Vespertino	\N	36
201	L	L-GIT-3	6	3	Vespertino	\N	36
202	A	A-IA-1	7	1	Matutino	\N	40
203	B	B-IA-1	7	1	Matutino	\N	40
204	C	C-IA-1	7	1	Matutino	\N	40
205	D	D-IA-1	7	1	Matutino	\N	40
206	E	E-IA-1	7	1	Matutino	\N	40
207	F	F-IA-1	7	1	Matutino	\N	40
208	G	G-IA-1	7	1	Matutino	\N	40
209	A	A-IA-2	7	2	Matutino	\N	38
210	B	B-IA-2	7	2	Matutino	\N	38
211	C	C-IA-2	7	2	Matutino	\N	38
212	D	D-IA-2	7	2	Matutino	\N	38
213	E	E-IA-2	7	2	Matutino	\N	38
214	F	F-IA-2	7	2	Matutino	\N	38
215	G	G-IA-2	7	2	Matutino	\N	38
216	A	A-IA-3	7	3	Matutino	\N	36
217	B	B-IA-3	7	3	Matutino	\N	36
218	C	C-IA-3	7	3	Matutino	\N	36
219	D	D-IA-3	7	3	Matutino	\N	36
220	E	E-IA-3	7	3	Matutino	\N	36
221	F	F-IA-3	7	3	Matutino	\N	36
222	G	G-IA-3	7	3	Matutino	\N	36
223	H	H-IA-1	7	1	Vespertino	\N	40
224	I	I-IA-1	7	1	Vespertino	\N	40
225	J	J-IA-1	7	1	Vespertino	\N	40
226	K	K-IA-1	7	1	Vespertino	\N	40
227	L	L-IA-1	7	1	Vespertino	\N	40
228	H	H-IA-2	7	2	Vespertino	\N	38
229	I	I-IA-2	7	2	Vespertino	\N	38
230	J	J-IA-2	7	2	Vespertino	\N	38
231	K	K-IA-2	7	2	Vespertino	\N	38
232	L	L-IA-2	7	2	Vespertino	\N	38
233	H	H-IA-3	7	3	Vespertino	\N	36
234	I	I-IA-3	7	3	Vespertino	\N	36
235	J	J-IA-3	7	3	Vespertino	\N	36
236	K	K-IA-3	7	3	Vespertino	\N	36
237	L	L-IA-3	7	3	Vespertino	\N	36
\.


--
-- Data for Name: horario; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.horario (id_horario, id_ciclo, id_grupo, tipo, estatus) FROM stdin;
1	1	1	General	Borrador
\.


--
-- Data for Name: horario_general; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.horario_general (id, hora, lunes, martes, miercoles, jueves, viernes) FROM stdin;
14	20:00	I-CIBER-2: Criptografia	I-CIBER-2: Hacking Etico I	J-CIBER-2: Redes Avanzadas	J-CIBER-2: Redes Avanzadas	J-CIBER-2: Redes Avanzadas
12	18:00	I-CIBER-1: Intro a Ciberseguridad / I-CIBER-2: Redes Avanzadas / H-IA-1: Programacion Python	I-CIBER-1: Intro a Ciberseguridad / I-CIBER-2: Criptografia / H-IA-1: Programacion Python	I-CIBER-1: Intro a Ciberseguridad / I-CIBER-2: Criptografia	I-CIBER-1: Intro a Ciberseguridad / I-CIBER-2: Criptografia	I-CIBER-2: Criptografia
10	16:00	J-PROG-1: Ingles Tecnico I / H-CIBER-1: Sistemas Operativos Linux / H-CIBER-2: Criptografia / H-IA-1: Matematicas para IA	J-PROG-1: Ingles Tecnico I / H-CIBER-1: Sistemas Operativos Linux / H-CIBER-2: Hacking Etico I / H-IA-1: Matematicas para IA	J-PROG-1: Ingles Tecnico I / H-CIBER-1: Sistemas Operativos Linux / H-CIBER-2: Hacking Etico I / H-IA-1: Programacion Python	H-CIBER-1: Sistemas Operativos Linux / H-CIBER-2: Hacking Etico I	I-CIBER-1: Fundamentos de Redes / H-CIBER-2: Hacking Etico I
13	19:00	I-CIBER-1: Sistemas Operativos Linux / I-CIBER-2: Redes Avanzadas / H-IA-1: Programacion Python	I-CIBER-2: Hacking Etico I / H-IA-1: Programacion Python	I-CIBER-2: Hacking Etico I	I-CIBER-2: Hacking Etico I	I-CIBER-2: Hacking Etico I
1	07:00	A-PROG-1: Fundamentos de Programacion / B-PROG-1: Ingles Tecnico I / C-PROG-1: Introduccion a TI / A-PROG-3: Desarrollo Web Front-End / B-PROG-3: Redes I / A-PROG-4: Programacion Movil I / B-PROG-4: Seguridad Informatica / A-PROG-6: IA Aplicada / B-PROG-6: Emprendimiento TI / A-MECA-1: Dibujo Tecnico / A-MECA-2: Procesos de Manufactura / A-ELEC-1: Circuitos Electricos I / A-MECAT-1: Fundamentos de Mecatronica / A-GIT-1: Fundamentos de Turismo / B-GIT-1: Ingles para Turismo I / A-GIT-2: Ingles para Turismo II / A-IA-1: Fundamentos de IA	A-PROG-1: Fundamentos de Programacion / B-PROG-1: Ingles Tecnico I / C-PROG-1: Introduccion a TI / A-PROG-3: Desarrollo Web Front-End / B-PROG-3: Redes I / A-PROG-4: Programacion Movil I / B-PROG-4: Seguridad Informatica / A-PROG-6: IA Aplicada / B-PROG-6: Emprendimiento TI / A-MECA-1: Dibujo Tecnico / A-MECA-2: Procesos de Manufactura / A-ELEC-1: Circuitos Electricos I / A-MECAT-1: Fundamentos de Mecatronica / A-GIT-1: Fundamentos de Turismo / B-GIT-1: Ingles para Turismo I / A-GIT-2: Ingles para Turismo II / A-IA-1: Fundamentos de IA	A-PROG-1: Fundamentos de Programacion / B-PROG-1: Ingles Tecnico I / C-PROG-1: Introduccion a TI / A-PROG-3: Desarrollo Web Front-End / B-PROG-3: Redes I / A-PROG-4: Programacion Movil I / B-PROG-4: Seguridad Informatica / A-PROG-6: IA Aplicada / B-PROG-6: Emprendimiento TI / A-MECA-1: Dibujo Tecnico / A-MECA-2: Procesos de Manufactura / A-ELEC-1: Circuitos Electricos I / A-MECAT-1: Fundamentos de Mecatronica / C-MECAT-1: Matematicas I Mecat / A-GIT-1: Fundamentos de Turismo / A-GIT-2: Ingles para Turismo II / A-IA-1: Fundamentos de IA	A-PROG-1: Fundamentos de Programacion / B-PROG-1: Introduccion a TI / A-PROG-3: Desarrollo Web Front-End / B-PROG-3: Redes I / A-PROG-4: Programacion Movil I / B-PROG-4: Seguridad Informatica / A-PROG-6: IA Aplicada / A-MECA-1: Dibujo Tecnico / A-MECA-2: Procesos de Manufactura / A-ELEC-1: Circuitos Electricos I / C-ELEC-1: Seguridad Electrica / A-MECAT-1: Fundamentos de Mecatronica / C-MECAT-1: Matematicas I Mecat / A-GIT-1: Fundamentos de Turismo / A-GIT-2: Ingles para Turismo II / A-IA-1: Fundamentos de IA	A-PROG-1: Fundamentos de Programacion / G-PROG-1: Introduccion a TI / A-PROG-3: Desarrollo Web Front-End / C-PROG-3: Redes I / A-PROG-4: Programacion Movil I / A-PROG-6: IA Aplicada / A-MECA-1: Matematicas I Mec / A-MECA-2: Procesos de Manufactura / A-ELEC-1: Circuitos Electricos I / C-ELEC-1: Seguridad Electrica / A-MECAT-1: Fundamentos de Mecatronica / C-MECAT-1: Matematicas I Mecat / A-IA-1: Fundamentos de IA
2	08:00	A-PROG-1: Matematicas I / B-PROG-1: Introduccion a TI / C-PROG-1: Ingles Tecnico I / A-PROG-3: Desarrollo Web Front-End / B-PROG-3: Ingles Tecnico III / A-PROG-4: Programacion Movil I / C-PROG-4: Seguridad Informatica / A-PROG-6: IA Aplicada / B-PROG-6: Emprendimiento TI / A-MECA-1: Matematicas I Mec / A-MECA-2: Procesos de Manufactura / A-ELEC-1: Matematicas I Elec / A-MECAT-1: Matematicas I Mecat / A-GIT-1: Geografia Turistica / B-GIT-1: Ingles para Turismo I / B-GIT-2: Ingles para Turismo II / A-IA-1: Matematicas para IA	A-PROG-1: Matematicas I / B-PROG-1: Introduccion a TI / C-PROG-1: Ingles Tecnico I / A-PROG-3: Base de Datos II / B-PROG-3: Ingles Tecnico III / A-PROG-4: Seguridad Informatica / B-PROG-4: Programacion Movil I / A-PROG-6: Proyecto Integrador II / C-PROG-6: IA Aplicada / A-MECA-1: Matematicas I Mec / A-MECA-2: Hidraulica y Neumatica / A-ELEC-1: Matematicas I Elec / A-MECAT-1: Matematicas I Mecat / A-GIT-1: Geografia Turistica / B-GIT-1: Ingles para Turismo I / B-GIT-2: Ingles para Turismo II / A-IA-1: Matematicas para IA	A-PROG-1: Matematicas I / B-PROG-1: Introduccion a TI / C-PROG-1: Ingles Tecnico I / A-PROG-3: Base de Datos II / B-PROG-3: Ingles Tecnico III / A-PROG-4: Seguridad Informatica / B-PROG-4: Programacion Movil I / A-PROG-6: Proyecto Integrador II / C-PROG-6: IA Aplicada / A-MECA-1: Matematicas I Mec / A-MECA-2: Hidraulica y Neumatica / A-ELEC-1: Matematicas I Elec / A-MECAT-1: Matematicas I Mecat / C-MECAT-1: Matematicas I Mecat / A-GIT-1: Geografia Turistica / B-GIT-2: Ingles para Turismo II / A-IA-1: Matematicas para IA	A-PROG-1: Matematicas I / C-PROG-1: Introduccion a TI / A-PROG-3: Base de Datos II / C-PROG-3: Redes I / A-PROG-4: Seguridad Informatica / B-PROG-4: Programacion Movil I / A-PROG-6: Proyecto Integrador II / A-MECA-1: Matematicas I Mec / A-MECA-2: Hidraulica y Neumatica / A-ELEC-1: Matematicas I Elec / C-ELEC-1: Seguridad Electrica / A-MECAT-1: Matematicas I Mecat / C-MECAT-1: Matematicas I Mecat / A-GIT-1: Geografia Turistica / B-GIT-2: Ingles para Turismo II / A-IA-1: Matematicas para IA	A-PROG-1: Matematicas I / G-PROG-1: Introduccion a TI / A-PROG-3: Base de Datos II / C-PROG-3: Ingles Tecnico III / B-PROG-4: Programacion Movil I / A-PROG-6: Proyecto Integrador II / B-MECA-1: Matematicas I Mec / A-MECA-2: Hidraulica y Neumatica / A-ELEC-1: Matematicas I Elec / D-ELEC-1: Circuitos Electricos I / A-MECAT-1: Matematicas I Mecat / C-MECAT-1: Dibujo Industrial / A-IA-1: Matematicas para IA
3	09:00	A-PROG-1: Ingles Tecnico I / B-PROG-1: Fundamentos de Programacion / D-PROG-1: Introduccion a TI / A-PROG-3: Base de Datos II / C-PROG-3: Redes I / A-PROG-4: Seguridad Informatica / B-PROG-4: Programacion Movil I / A-PROG-6: Proyecto Integrador II / C-PROG-6: IA Aplicada / A-MECA-1: Seguridad Industrial / A-MECA-2: Hidraulica y Neumatica / A-ELEC-1: Seguridad Electrica / A-MECAT-1: Dibujo Industrial / A-GIT-1: Ingles para Turismo I / C-GIT-1: Fundamentos de Turismo / C-GIT-2: Ingles para Turismo II / A-IA-1: Programacion Python	A-PROG-1: Ingles Tecnico I / B-PROG-1: Fundamentos de Programacion / D-PROG-1: Introduccion a TI / A-PROG-3: Redes I / B-PROG-3: Desarrollo Web Front-End / C-PROG-4: Programacion Movil I / D-PROG-4: Seguridad Informatica / A-PROG-6: Proyecto Integrador II / C-PROG-6: IA Aplicada / A-MECA-1: Seguridad Industrial / A-MECA-2: Matematicas II Mec / A-ELEC-1: Seguridad Electrica / A-MECAT-1: Dibujo Industrial / A-GIT-1: Ingles para Turismo I / C-GIT-1: Fundamentos de Turismo / C-GIT-2: Ingles para Turismo II / A-IA-1: Programacion Python	A-PROG-1: Ingles Tecnico I / B-PROG-1: Fundamentos de Programacion / D-PROG-1: Introduccion a TI / A-PROG-3: Redes I / B-PROG-3: Desarrollo Web Front-End / C-PROG-4: Programacion Movil I / D-PROG-4: Seguridad Informatica / A-PROG-6: Proyecto Integrador II / C-PROG-6: IA Aplicada / A-MECA-1: Seguridad Industrial / A-MECA-2: Matematicas II Mec / A-ELEC-1: Seguridad Electrica / A-MECAT-1: Dibujo Industrial / C-MECAT-1: Dibujo Industrial / A-GIT-1: Ingles para Turismo I / C-GIT-2: Ingles para Turismo II / A-IA-1: Programacion Python	A-PROG-1: Introduccion a TI / B-PROG-1: Fundamentos de Programacion / A-PROG-3: Redes I / B-PROG-3: Desarrollo Web Front-End / C-PROG-4: Programacion Movil I / D-PROG-4: Seguridad Informatica / A-PROG-6: Emprendimiento TI / B-MECA-1: Dibujo Tecnico / A-MECA-2: Matematicas II Mec / B-ELEC-1: Circuitos Electricos I / D-ELEC-1: Circuitos Electricos I / A-MECAT-1: Dibujo Industrial / C-MECAT-1: Dibujo Industrial / A-GIT-1: Ingles para Turismo I / C-GIT-2: Ingles para Turismo II / A-IA-1: Programacion Python	B-PROG-1: Fundamentos de Programacion / B-PROG-3: Desarrollo Web Front-End / C-PROG-4: Programacion Movil I / B-PROG-6: IA Aplicada / A-MECA-2: Matematicas II Mec / B-ELEC-1: Circuitos Electricos I / D-ELEC-1: Circuitos Electricos I / B-MECAT-1: Fundamentos de Mecatronica / A-IA-1: Programacion Python
4	10:00	A-PROG-1: Introduccion a TI / B-PROG-1: Matematicas I / D-PROG-1: Ingles Tecnico I / A-PROG-3: Redes I / B-PROG-3: Desarrollo Web Front-End / B-PROG-4: Programacion Movil I / D-PROG-4: Seguridad Informatica / A-PROG-6: Proyecto Integrador II / C-PROG-6: IA Aplicada / B-MECA-1: Dibujo Tecnico / A-MECA-2: Matematicas II Mec / B-ELEC-1: Circuitos Electricos I / B-MECAT-1: Fundamentos de Mecatronica / B-GIT-1: Fundamentos de Turismo / C-GIT-1: Fundamentos de Turismo / D-GIT-2: Ingles para Turismo II / B-IA-1: Fundamentos de IA	A-PROG-1: Introduccion a TI / B-PROG-1: Matematicas I / D-PROG-1: Ingles Tecnico I / A-PROG-3: Ingles Tecnico III / B-PROG-3: Base de Datos II / C-PROG-4: Seguridad Informatica / D-PROG-4: Programacion Movil I / A-PROG-6: Emprendimiento TI / C-PROG-6: Proyecto Integrador II / B-MECA-1: Dibujo Tecnico / B-MECA-2: Procesos de Manufactura / B-ELEC-1: Circuitos Electricos I / B-MECAT-1: Fundamentos de Mecatronica / B-GIT-1: Fundamentos de Turismo / C-GIT-1: Fundamentos de Turismo / D-GIT-2: Ingles para Turismo II / B-IA-1: Fundamentos de IA	A-PROG-1: Introduccion a TI / B-PROG-1: Matematicas I / D-PROG-1: Ingles Tecnico I / A-PROG-3: Ingles Tecnico III / B-PROG-3: Base de Datos II / C-PROG-4: Seguridad Informatica / D-PROG-4: Programacion Movil I / A-PROG-6: Emprendimiento TI / C-PROG-6: Proyecto Integrador II / B-MECA-1: Dibujo Tecnico / B-MECA-2: Procesos de Manufactura / B-ELEC-1: Circuitos Electricos I / B-MECAT-1: Fundamentos de Mecatronica / C-MECAT-1: Dibujo Industrial / B-GIT-1: Fundamentos de Turismo / D-GIT-2: Ingles para Turismo II / B-IA-1: Fundamentos de IA	B-PROG-1: Matematicas I / D-PROG-1: Introduccion a TI / B-PROG-3: Base de Datos II / C-PROG-3: Ingles Tecnico III / C-PROG-4: Seguridad Informatica / D-PROG-4: Programacion Movil I / B-PROG-6: IA Aplicada / B-MECA-1: Matematicas I Mec / B-MECA-2: Procesos de Manufactura / B-ELEC-1: Matematicas I Elec / D-ELEC-1: Circuitos Electricos I / B-MECAT-1: Fundamentos de Mecatronica / B-GIT-1: Fundamentos de Turismo / D-GIT-2: Ingles para Turismo II / B-IA-1: Fundamentos de IA	B-PROG-1: Matematicas I / B-PROG-3: Base de Datos II / D-PROG-4: Programacion Movil I / B-PROG-6: Proyecto Integrador II / B-ELEC-1: Matematicas I Elec / D-ELEC-1: Matematicas I Elec / B-MECAT-1: Matematicas I Mecat / B-IA-1: Fundamentos de IA
5	11:00	C-PROG-1: Fundamentos de Programacion / E-PROG-1: Ingles Tecnico I / F-PROG-1: Introduccion a TI / A-PROG-3: Ingles Tecnico III / B-PROG-3: Desarrollo Web Front-End / C-PROG-4: Programacion Movil I / E-PROG-4: Seguridad Informatica / A-PROG-6: Emprendimiento TI / C-PROG-6: Proyecto Integrador II / B-MECA-1: Matematicas I Mec / B-MECA-2: Procesos de Manufactura / B-ELEC-1: Matematicas I Elec / B-MECAT-1: Matematicas I Mecat / B-GIT-1: Geografia Turistica / C-GIT-1: Geografia Turistica / E-GIT-2: Ingles para Turismo II / B-IA-1: Matematicas para IA	C-PROG-1: Fundamentos de Programacion / E-PROG-1: Ingles Tecnico I / F-PROG-1: Introduccion a TI / C-PROG-3: Desarrollo Web Front-End / D-PROG-4: Programacion Movil I / E-PROG-4: Seguridad Informatica / B-PROG-6: IA Aplicada / C-PROG-6: Proyecto Integrador II / B-MECA-1: Matematicas I Mec / B-ELEC-1: Matematicas I Elec / B-MECAT-1: Matematicas I Mecat / B-GIT-1: Geografia Turistica / C-GIT-1: Geografia Turistica / E-GIT-2: Ingles para Turismo II / B-IA-1: Matematicas para IA	C-PROG-1: Fundamentos de Programacion / E-PROG-1: Ingles Tecnico I / F-PROG-1: Introduccion a TI / C-PROG-3: Desarrollo Web Front-End / E-PROG-4: Programacion Movil I / B-PROG-6: IA Aplicada / B-MECA-1: Matematicas I Mec / B-ELEC-1: Matematicas I Elec / B-MECAT-1: Matematicas I Mecat / D-MECAT-1: Fundamentos de Mecatronica / B-GIT-1: Geografia Turistica / E-GIT-2: Ingles para Turismo II / B-IA-1: Matematicas para IA	C-PROG-1: Fundamentos de Programacion / E-PROG-1: Introduccion a TI / C-PROG-3: Desarrollo Web Front-End / E-PROG-4: Programacion Movil I / B-PROG-6: Proyecto Integrador II / C-ELEC-1: Circuitos Electricos I / D-ELEC-1: Matematicas I Elec / B-MECAT-1: Matematicas I Mecat / B-GIT-1: Geografia Turistica / E-GIT-2: Ingles para Turismo II / B-IA-1: Matematicas para IA	C-PROG-1: Fundamentos de Programacion / C-PROG-3: Desarrollo Web Front-End / E-PROG-4: Programacion Movil I / C-ELEC-1: Circuitos Electricos I / D-ELEC-1: Matematicas I Elec / B-IA-1: Matematicas para IA
6	12:00	C-PROG-1: Matematicas I / E-PROG-1: Introduccion a TI / F-PROG-1: Ingles Tecnico I / B-PROG-3: Base de Datos II / C-PROG-3: Redes I / C-PROG-4: Programacion Movil I / B-PROG-6: IA Aplicada / C-PROG-6: Proyecto Integrador II / B-MECA-1: Seguridad Industrial / B-ELEC-1: Seguridad Electrica / B-MECAT-1: Dibujo Industrial / B-IA-1: Programacion Python	C-PROG-1: Matematicas I / E-PROG-1: Introduccion a TI / F-PROG-1: Ingles Tecnico I / C-PROG-3: Desarrollo Web Front-End / E-PROG-4: Programacion Movil I / B-PROG-6: Proyecto Integrador II / B-MECA-1: Seguridad Industrial / B-ELEC-1: Seguridad Electrica / B-MECAT-1: Dibujo Industrial / B-IA-1: Programacion Python	C-PROG-1: Matematicas I / E-PROG-1: Introduccion a TI / F-PROG-1: Ingles Tecnico I / C-PROG-3: Base de Datos II / E-PROG-4: Programacion Movil I / B-PROG-6: Proyecto Integrador II / B-MECA-1: Seguridad Industrial / B-ELEC-1: Seguridad Electrica / B-MECAT-1: Dibujo Industrial / B-IA-1: Programacion Python	C-PROG-1: Matematicas I / F-PROG-1: Introduccion a TI / C-PROG-3: Base de Datos II / E-PROG-4: Seguridad Informatica / C-ELEC-1: Matematicas I Elec / D-ELEC-1: Matematicas I Elec / B-MECAT-1: Dibujo Industrial / B-IA-1: Programacion Python	C-PROG-1: Matematicas I / C-PROG-3: Base de Datos II / C-ELEC-1: Matematicas I Elec / D-ELEC-1: Seguridad Electrica / B-IA-1: Programacion Python
7	13:00	D-PROG-1: Fundamentos de Programacion / G-PROG-1: Ingles Tecnico I / C-PROG-3: Desarrollo Web Front-End / D-PROG-4: Programacion Movil I / B-PROG-6: IA Aplicada / C-MECA-1: Dibujo Tecnico / C-ELEC-1: Circuitos Electricos I / C-MECAT-1: Fundamentos de Mecatronica / C-IA-1: Fundamentos de IA	D-PROG-1: Fundamentos de Programacion / G-PROG-1: Ingles Tecnico I / C-PROG-3: Base de Datos II / E-PROG-4: Programacion Movil I / B-PROG-6: Proyecto Integrador II / C-ELEC-1: Circuitos Electricos I / C-MECAT-1: Fundamentos de Mecatronica / C-IA-1: Fundamentos de IA	D-PROG-1: Fundamentos de Programacion / G-PROG-1: Ingles Tecnico I / C-PROG-3: Base de Datos II / E-PROG-4: Seguridad Informatica / B-PROG-6: Proyecto Integrador II / C-ELEC-1: Circuitos Electricos I / C-IA-1: Fundamentos de IA	D-PROG-1: Fundamentos de Programacion / G-PROG-1: Introduccion a TI / D-PROG-3: Desarrollo Web Front-End / C-ELEC-1: Matematicas I Elec / D-ELEC-1: Seguridad Electrica / C-IA-1: Fundamentos de IA	D-PROG-1: Fundamentos de Programacion / D-PROG-3: Desarrollo Web Front-End / C-ELEC-1: Matematicas I Elec / D-ELEC-1: Seguridad Electrica / C-IA-1: Fundamentos de IA
8	14:00	H-PROG-1: Ingles Tecnico I / I-PROG-1: Introduccion a TI / H-CIBER-1: Fundamentos de Redes / H-CIBER-2: Redes Avanzadas / H-IA-1: Fundamentos de IA	H-PROG-1: Ingles Tecnico I / I-PROG-1: Introduccion a TI / H-CIBER-1: Fundamentos de Redes / H-CIBER-2: Redes Avanzadas / H-IA-1: Fundamentos de IA	H-PROG-1: Ingles Tecnico I / I-PROG-1: Introduccion a TI / H-CIBER-1: Fundamentos de Redes / H-CIBER-2: Redes Avanzadas / H-IA-1: Fundamentos de IA	H-PROG-1: Introduccion a TI / H-CIBER-1: Fundamentos de Redes / H-CIBER-2: Redes Avanzadas	H-CIBER-1: Fundamentos de Redes / H-CIBER-2: Redes Avanzadas
9	15:00	H-PROG-1: Introduccion a TI / I-PROG-1: Ingles Tecnico I / H-CIBER-1: Intro a Ciberseguridad / H-CIBER-2: Redes Avanzadas / H-IA-1: Fundamentos de IA	H-PROG-1: Introduccion a TI / I-PROG-1: Ingles Tecnico I / H-CIBER-1: Intro a Ciberseguridad / H-CIBER-2: Criptografia / H-IA-1: Fundamentos de IA	H-PROG-1: Introduccion a TI / I-PROG-1: Ingles Tecnico I / H-CIBER-1: Intro a Ciberseguridad / H-CIBER-2: Criptografia / H-IA-1: Matematicas para IA	I-PROG-1: Introduccion a TI / H-CIBER-1: Intro a Ciberseguridad / H-CIBER-2: Criptografia	H-CIBER-1: Intro a Ciberseguridad / H-CIBER-2: Criptografia
11	17:00	I-CIBER-1: Fundamentos de Redes / H-CIBER-2: Hacking Etico I / H-IA-1: Matematicas para IA	I-CIBER-1: Fundamentos de Redes / I-CIBER-2: Redes Avanzadas / H-IA-1: Matematicas para IA	I-CIBER-1: Fundamentos de Redes / I-CIBER-2: Redes Avanzadas	I-CIBER-1: Fundamentos de Redes / I-CIBER-2: Redes Avanzadas	I-CIBER-1: Intro a Ciberseguridad / I-CIBER-2: Redes Avanzadas
\.


--
-- Data for Name: horario_grupos; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.horario_grupos (id, grupo, materia, profesor, dia, hora) FROM stdin;
1	A-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Lunes	07:00
2	A-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Martes	07:00
3	A-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Miercoles	07:00
4	A-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Jueves	07:00
5	A-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Viernes	07:00
6	A-PROG-1	Matematicas I	Juan Perez Lopez	Lunes	08:00
7	A-PROG-1	Matematicas I	Juan Perez Lopez	Martes	08:00
8	A-PROG-1	Matematicas I	Juan Perez Lopez	Miercoles	08:00
9	A-PROG-1	Matematicas I	Juan Perez Lopez	Jueves	08:00
10	A-PROG-1	Matematicas I	Juan Perez Lopez	Viernes	08:00
11	A-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Lunes	09:00
12	A-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Martes	09:00
13	A-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Miercoles	09:00
14	A-PROG-1	Introduccion a TI	Ana Lopez Mendez	Lunes	10:00
15	A-PROG-1	Introduccion a TI	Ana Lopez Mendez	Martes	10:00
16	A-PROG-1	Introduccion a TI	Ana Lopez Mendez	Miercoles	10:00
17	A-PROG-1	Introduccion a TI	Ana Lopez Mendez	Jueves	09:00
18	B-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Lunes	09:00
19	B-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Martes	09:00
20	B-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Miercoles	09:00
21	B-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Jueves	09:00
22	B-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Viernes	09:00
23	B-PROG-1	Matematicas I	Juan Perez Lopez	Lunes	10:00
24	B-PROG-1	Matematicas I	Juan Perez Lopez	Martes	10:00
25	B-PROG-1	Matematicas I	Juan Perez Lopez	Miercoles	10:00
26	B-PROG-1	Matematicas I	Juan Perez Lopez	Jueves	10:00
27	B-PROG-1	Matematicas I	Juan Perez Lopez	Viernes	10:00
28	B-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Lunes	07:00
29	B-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Martes	07:00
30	B-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Miercoles	07:00
31	B-PROG-1	Introduccion a TI	Ana Lopez Mendez	Lunes	08:00
32	B-PROG-1	Introduccion a TI	Ana Lopez Mendez	Martes	08:00
33	B-PROG-1	Introduccion a TI	Ana Lopez Mendez	Miercoles	08:00
34	B-PROG-1	Introduccion a TI	Ana Lopez Mendez	Jueves	07:00
35	C-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Lunes	11:00
36	C-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Martes	11:00
37	C-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Miercoles	11:00
38	C-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Jueves	11:00
39	C-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Viernes	11:00
40	C-PROG-1	Matematicas I	Juan Perez Lopez	Lunes	12:00
41	C-PROG-1	Matematicas I	Juan Perez Lopez	Martes	12:00
42	C-PROG-1	Matematicas I	Juan Perez Lopez	Miercoles	12:00
43	C-PROG-1	Matematicas I	Juan Perez Lopez	Jueves	12:00
44	C-PROG-1	Matematicas I	Juan Perez Lopez	Viernes	12:00
45	C-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Lunes	08:00
46	C-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Martes	08:00
47	C-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Miercoles	08:00
48	C-PROG-1	Introduccion a TI	Ana Lopez Mendez	Lunes	07:00
49	C-PROG-1	Introduccion a TI	Ana Lopez Mendez	Martes	07:00
50	C-PROG-1	Introduccion a TI	Ana Lopez Mendez	Miercoles	07:00
51	C-PROG-1	Introduccion a TI	Ana Lopez Mendez	Jueves	08:00
52	D-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Lunes	13:00
53	D-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Martes	13:00
54	D-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Miercoles	13:00
55	D-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Jueves	13:00
56	D-PROG-1	Fundamentos de Programacion	Juan Perez Lopez	Viernes	13:00
57	D-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Lunes	10:00
58	D-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Martes	10:00
59	D-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Miercoles	10:00
60	D-PROG-1	Introduccion a TI	Ana Lopez Mendez	Lunes	09:00
61	D-PROG-1	Introduccion a TI	Ana Lopez Mendez	Martes	09:00
62	D-PROG-1	Introduccion a TI	Ana Lopez Mendez	Miercoles	09:00
63	D-PROG-1	Introduccion a TI	Ana Lopez Mendez	Jueves	10:00
64	E-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Lunes	11:00
65	E-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Martes	11:00
66	E-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Miercoles	11:00
67	E-PROG-1	Introduccion a TI	Ana Lopez Mendez	Lunes	12:00
68	E-PROG-1	Introduccion a TI	Ana Lopez Mendez	Martes	12:00
69	E-PROG-1	Introduccion a TI	Ana Lopez Mendez	Miercoles	12:00
70	E-PROG-1	Introduccion a TI	Ana Lopez Mendez	Jueves	11:00
71	F-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Lunes	12:00
72	F-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Martes	12:00
73	F-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Miercoles	12:00
74	F-PROG-1	Introduccion a TI	Ana Lopez Mendez	Lunes	11:00
75	F-PROG-1	Introduccion a TI	Ana Lopez Mendez	Martes	11:00
76	F-PROG-1	Introduccion a TI	Ana Lopez Mendez	Miercoles	11:00
77	F-PROG-1	Introduccion a TI	Ana Lopez Mendez	Jueves	12:00
78	G-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Lunes	13:00
79	G-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Martes	13:00
80	G-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Miercoles	13:00
81	G-PROG-1	Introduccion a TI	Ana Lopez Mendez	Jueves	13:00
82	G-PROG-1	Introduccion a TI	Ana Lopez Mendez	Viernes	07:00
83	G-PROG-1	Introduccion a TI	Ana Lopez Mendez	Viernes	08:00
84	H-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Lunes	14:00
85	H-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Martes	14:00
86	H-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Miercoles	14:00
87	H-PROG-1	Introduccion a TI	Ana Lopez Mendez	Lunes	15:00
88	H-PROG-1	Introduccion a TI	Ana Lopez Mendez	Martes	15:00
89	H-PROG-1	Introduccion a TI	Ana Lopez Mendez	Miercoles	15:00
90	H-PROG-1	Introduccion a TI	Ana Lopez Mendez	Jueves	14:00
91	I-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Lunes	15:00
92	I-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Martes	15:00
93	I-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Miercoles	15:00
94	I-PROG-1	Introduccion a TI	Ana Lopez Mendez	Lunes	14:00
95	I-PROG-1	Introduccion a TI	Ana Lopez Mendez	Martes	14:00
96	I-PROG-1	Introduccion a TI	Ana Lopez Mendez	Miercoles	14:00
97	I-PROG-1	Introduccion a TI	Ana Lopez Mendez	Jueves	15:00
98	J-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Lunes	16:00
99	J-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Martes	16:00
100	J-PROG-1	Ingles Tecnico I	Carlos Hernandez Soto	Miercoles	16:00
101	A-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Lunes	07:00
102	A-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Martes	07:00
103	A-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Miercoles	07:00
104	A-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Jueves	07:00
105	A-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Viernes	07:00
106	A-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Lunes	08:00
107	A-PROG-3	Base de Datos II	Maria Gomez Ruiz	Lunes	09:00
108	A-PROG-3	Base de Datos II	Maria Gomez Ruiz	Martes	08:00
109	A-PROG-3	Base de Datos II	Maria Gomez Ruiz	Miercoles	08:00
110	A-PROG-3	Base de Datos II	Maria Gomez Ruiz	Jueves	08:00
111	A-PROG-3	Base de Datos II	Maria Gomez Ruiz	Viernes	08:00
112	A-PROG-3	Redes I	Fernanda Rios Castellanos	Lunes	10:00
113	A-PROG-3	Redes I	Fernanda Rios Castellanos	Martes	09:00
114	A-PROG-3	Redes I	Fernanda Rios Castellanos	Miercoles	09:00
115	A-PROG-3	Redes I	Fernanda Rios Castellanos	Jueves	09:00
116	A-PROG-3	Ingles Tecnico III	Fernanda Rios Castellanos	Lunes	11:00
117	A-PROG-3	Ingles Tecnico III	Fernanda Rios Castellanos	Martes	10:00
118	A-PROG-3	Ingles Tecnico III	Fernanda Rios Castellanos	Miercoles	10:00
119	B-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Lunes	10:00
120	B-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Martes	09:00
121	B-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Miercoles	09:00
122	B-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Jueves	09:00
123	B-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Viernes	09:00
124	B-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Lunes	11:00
125	B-PROG-3	Base de Datos II	Maria Gomez Ruiz	Lunes	12:00
126	B-PROG-3	Base de Datos II	Maria Gomez Ruiz	Martes	10:00
127	B-PROG-3	Base de Datos II	Maria Gomez Ruiz	Miercoles	10:00
128	B-PROG-3	Base de Datos II	Maria Gomez Ruiz	Jueves	10:00
129	B-PROG-3	Base de Datos II	Maria Gomez Ruiz	Viernes	10:00
130	B-PROG-3	Redes I	Fernanda Rios Castellanos	Lunes	07:00
131	B-PROG-3	Redes I	Fernanda Rios Castellanos	Martes	07:00
132	B-PROG-3	Redes I	Fernanda Rios Castellanos	Miercoles	07:00
133	B-PROG-3	Redes I	Fernanda Rios Castellanos	Jueves	07:00
134	B-PROG-3	Ingles Tecnico III	Fernanda Rios Castellanos	Lunes	08:00
135	B-PROG-3	Ingles Tecnico III	Fernanda Rios Castellanos	Martes	08:00
136	B-PROG-3	Ingles Tecnico III	Fernanda Rios Castellanos	Miercoles	08:00
137	C-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Lunes	13:00
138	C-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Martes	11:00
139	C-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Miercoles	11:00
140	C-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Jueves	11:00
141	C-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Viernes	11:00
142	C-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Martes	12:00
143	C-PROG-3	Base de Datos II	Maria Gomez Ruiz	Martes	13:00
144	C-PROG-3	Base de Datos II	Maria Gomez Ruiz	Miercoles	12:00
145	C-PROG-3	Base de Datos II	Maria Gomez Ruiz	Jueves	12:00
146	C-PROG-3	Base de Datos II	Maria Gomez Ruiz	Viernes	12:00
147	C-PROG-3	Base de Datos II	Maria Gomez Ruiz	Miercoles	13:00
148	C-PROG-3	Redes I	Fernanda Rios Castellanos	Lunes	09:00
149	C-PROG-3	Redes I	Fernanda Rios Castellanos	Jueves	08:00
150	C-PROG-3	Redes I	Fernanda Rios Castellanos	Viernes	07:00
151	C-PROG-3	Redes I	Fernanda Rios Castellanos	Lunes	12:00
152	C-PROG-3	Ingles Tecnico III	Fernanda Rios Castellanos	Jueves	10:00
153	C-PROG-3	Ingles Tecnico III	Fernanda Rios Castellanos	Viernes	08:00
154	D-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Jueves	13:00
155	D-PROG-3	Desarrollo Web Front-End	Maria Gomez Ruiz	Viernes	13:00
156	A-PROG-4	Programacion Movil I	Sofia Torres Vega	Lunes	07:00
157	A-PROG-4	Programacion Movil I	Sofia Torres Vega	Martes	07:00
158	A-PROG-4	Programacion Movil I	Sofia Torres Vega	Miercoles	07:00
159	A-PROG-4	Programacion Movil I	Sofia Torres Vega	Jueves	07:00
160	A-PROG-4	Programacion Movil I	Sofia Torres Vega	Viernes	07:00
161	A-PROG-4	Programacion Movil I	Sofia Torres Vega	Lunes	08:00
162	A-PROG-4	Seguridad Informatica	Elena Flores Garcia	Lunes	09:00
163	A-PROG-4	Seguridad Informatica	Elena Flores Garcia	Martes	08:00
164	A-PROG-4	Seguridad Informatica	Elena Flores Garcia	Miercoles	08:00
165	A-PROG-4	Seguridad Informatica	Elena Flores Garcia	Jueves	08:00
166	B-PROG-4	Programacion Movil I	Sofia Torres Vega	Lunes	09:00
167	B-PROG-4	Programacion Movil I	Sofia Torres Vega	Martes	08:00
168	B-PROG-4	Programacion Movil I	Sofia Torres Vega	Miercoles	08:00
169	B-PROG-4	Programacion Movil I	Sofia Torres Vega	Jueves	08:00
170	B-PROG-4	Programacion Movil I	Sofia Torres Vega	Viernes	08:00
171	B-PROG-4	Programacion Movil I	Sofia Torres Vega	Lunes	10:00
172	B-PROG-4	Seguridad Informatica	Elena Flores Garcia	Lunes	07:00
173	B-PROG-4	Seguridad Informatica	Elena Flores Garcia	Martes	07:00
174	B-PROG-4	Seguridad Informatica	Elena Flores Garcia	Miercoles	07:00
175	B-PROG-4	Seguridad Informatica	Elena Flores Garcia	Jueves	07:00
176	C-PROG-4	Programacion Movil I	Sofia Torres Vega	Lunes	11:00
177	C-PROG-4	Programacion Movil I	Sofia Torres Vega	Martes	09:00
178	C-PROG-4	Programacion Movil I	Sofia Torres Vega	Miercoles	09:00
179	C-PROG-4	Programacion Movil I	Sofia Torres Vega	Jueves	09:00
180	C-PROG-4	Programacion Movil I	Sofia Torres Vega	Viernes	09:00
181	C-PROG-4	Programacion Movil I	Sofia Torres Vega	Lunes	12:00
182	C-PROG-4	Seguridad Informatica	Elena Flores Garcia	Lunes	08:00
183	C-PROG-4	Seguridad Informatica	Elena Flores Garcia	Martes	10:00
184	C-PROG-4	Seguridad Informatica	Elena Flores Garcia	Miercoles	10:00
185	C-PROG-4	Seguridad Informatica	Elena Flores Garcia	Jueves	10:00
186	D-PROG-4	Programacion Movil I	Sofia Torres Vega	Lunes	13:00
187	D-PROG-4	Programacion Movil I	Sofia Torres Vega	Martes	10:00
188	D-PROG-4	Programacion Movil I	Sofia Torres Vega	Miercoles	10:00
189	D-PROG-4	Programacion Movil I	Sofia Torres Vega	Jueves	10:00
190	D-PROG-4	Programacion Movil I	Sofia Torres Vega	Viernes	10:00
191	D-PROG-4	Programacion Movil I	Sofia Torres Vega	Martes	11:00
192	D-PROG-4	Seguridad Informatica	Elena Flores Garcia	Lunes	10:00
193	D-PROG-4	Seguridad Informatica	Elena Flores Garcia	Martes	09:00
194	D-PROG-4	Seguridad Informatica	Elena Flores Garcia	Miercoles	09:00
195	D-PROG-4	Seguridad Informatica	Elena Flores Garcia	Jueves	09:00
196	E-PROG-4	Programacion Movil I	Sofia Torres Vega	Martes	12:00
197	E-PROG-4	Programacion Movil I	Sofia Torres Vega	Miercoles	11:00
198	E-PROG-4	Programacion Movil I	Sofia Torres Vega	Jueves	11:00
199	E-PROG-4	Programacion Movil I	Sofia Torres Vega	Viernes	11:00
200	E-PROG-4	Programacion Movil I	Sofia Torres Vega	Martes	13:00
201	E-PROG-4	Programacion Movil I	Sofia Torres Vega	Miercoles	12:00
202	E-PROG-4	Seguridad Informatica	Elena Flores Garcia	Lunes	11:00
203	E-PROG-4	Seguridad Informatica	Elena Flores Garcia	Martes	11:00
204	E-PROG-4	Seguridad Informatica	Elena Flores Garcia	Miercoles	13:00
205	E-PROG-4	Seguridad Informatica	Elena Flores Garcia	Jueves	12:00
206	A-PROG-6	IA Aplicada	Diego Morales Ruiz	Lunes	07:00
207	A-PROG-6	IA Aplicada	Diego Morales Ruiz	Martes	07:00
208	A-PROG-6	IA Aplicada	Diego Morales Ruiz	Miercoles	07:00
209	A-PROG-6	IA Aplicada	Diego Morales Ruiz	Jueves	07:00
210	A-PROG-6	IA Aplicada	Diego Morales Ruiz	Viernes	07:00
211	A-PROG-6	IA Aplicada	Diego Morales Ruiz	Lunes	08:00
212	A-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Lunes	09:00
213	A-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Martes	08:00
214	A-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Miercoles	08:00
215	A-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Jueves	08:00
216	A-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Viernes	08:00
217	A-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Lunes	10:00
218	A-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Martes	09:00
219	A-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Miercoles	09:00
220	A-PROG-6	Emprendimiento TI	Diego Morales Ruiz	Lunes	11:00
221	A-PROG-6	Emprendimiento TI	Diego Morales Ruiz	Martes	10:00
222	A-PROG-6	Emprendimiento TI	Diego Morales Ruiz	Miercoles	10:00
223	A-PROG-6	Emprendimiento TI	Diego Morales Ruiz	Jueves	09:00
224	B-PROG-6	IA Aplicada	Diego Morales Ruiz	Lunes	12:00
225	B-PROG-6	IA Aplicada	Diego Morales Ruiz	Martes	11:00
226	B-PROG-6	IA Aplicada	Diego Morales Ruiz	Miercoles	11:00
227	B-PROG-6	IA Aplicada	Diego Morales Ruiz	Jueves	10:00
228	B-PROG-6	IA Aplicada	Diego Morales Ruiz	Viernes	09:00
229	B-PROG-6	IA Aplicada	Diego Morales Ruiz	Lunes	13:00
230	B-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Martes	12:00
231	B-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Miercoles	12:00
232	B-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Jueves	11:00
233	B-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Viernes	10:00
234	B-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Martes	13:00
235	B-PROG-6	Proyecto Integrador II	Diego Morales Ruiz	Miercoles	13:00
236	B-PROG-6	Emprendimiento TI	Miguel Santana Reyna	Lunes	07:00
237	B-PROG-6	Emprendimiento TI	Miguel Santana Reyna	Martes	07:00
238	B-PROG-6	Emprendimiento TI	Miguel Santana Reyna	Miercoles	07:00
239	B-PROG-6	Emprendimiento TI	Miguel Santana Reyna	Lunes	08:00
240	C-PROG-6	IA Aplicada	Miguel Santana Reyna	Lunes	09:00
241	C-PROG-6	IA Aplicada	Miguel Santana Reyna	Martes	08:00
242	C-PROG-6	IA Aplicada	Miguel Santana Reyna	Miercoles	08:00
243	C-PROG-6	IA Aplicada	Miguel Santana Reyna	Lunes	10:00
244	C-PROG-6	IA Aplicada	Miguel Santana Reyna	Martes	09:00
245	C-PROG-6	IA Aplicada	Miguel Santana Reyna	Miercoles	09:00
246	C-PROG-6	Proyecto Integrador II	Miguel Santana Reyna	Lunes	11:00
247	C-PROG-6	Proyecto Integrador II	Miguel Santana Reyna	Martes	10:00
248	C-PROG-6	Proyecto Integrador II	Miguel Santana Reyna	Miercoles	10:00
249	C-PROG-6	Proyecto Integrador II	Miguel Santana Reyna	Lunes	12:00
250	C-PROG-6	Proyecto Integrador II	Miguel Santana Reyna	Martes	11:00
251	A-MECA-1	Dibujo Tecnico	Marco Rivera Salinas	Lunes	07:00
252	A-MECA-1	Dibujo Tecnico	Marco Rivera Salinas	Martes	07:00
253	A-MECA-1	Dibujo Tecnico	Marco Rivera Salinas	Miercoles	07:00
254	A-MECA-1	Dibujo Tecnico	Marco Rivera Salinas	Jueves	07:00
255	A-MECA-1	Matematicas I Mec	Marco Rivera Salinas	Lunes	08:00
256	A-MECA-1	Matematicas I Mec	Marco Rivera Salinas	Martes	08:00
257	A-MECA-1	Matematicas I Mec	Marco Rivera Salinas	Miercoles	08:00
258	A-MECA-1	Matematicas I Mec	Marco Rivera Salinas	Jueves	08:00
259	A-MECA-1	Matematicas I Mec	Marco Rivera Salinas	Viernes	07:00
260	A-MECA-1	Seguridad Industrial	Marco Rivera Salinas	Lunes	09:00
261	A-MECA-1	Seguridad Industrial	Marco Rivera Salinas	Martes	09:00
262	A-MECA-1	Seguridad Industrial	Marco Rivera Salinas	Miercoles	09:00
263	B-MECA-1	Dibujo Tecnico	Marco Rivera Salinas	Lunes	10:00
264	B-MECA-1	Dibujo Tecnico	Marco Rivera Salinas	Martes	10:00
265	B-MECA-1	Dibujo Tecnico	Marco Rivera Salinas	Miercoles	10:00
266	B-MECA-1	Dibujo Tecnico	Marco Rivera Salinas	Jueves	09:00
267	B-MECA-1	Matematicas I Mec	Marco Rivera Salinas	Lunes	11:00
268	B-MECA-1	Matematicas I Mec	Marco Rivera Salinas	Martes	11:00
269	B-MECA-1	Matematicas I Mec	Marco Rivera Salinas	Miercoles	11:00
270	B-MECA-1	Matematicas I Mec	Marco Rivera Salinas	Jueves	10:00
271	B-MECA-1	Matematicas I Mec	Marco Rivera Salinas	Viernes	08:00
272	B-MECA-1	Seguridad Industrial	Marco Rivera Salinas	Lunes	12:00
273	B-MECA-1	Seguridad Industrial	Marco Rivera Salinas	Martes	12:00
274	B-MECA-1	Seguridad Industrial	Marco Rivera Salinas	Miercoles	12:00
275	C-MECA-1	Dibujo Tecnico	Marco Rivera Salinas	Lunes	13:00
276	A-MECA-2	Procesos de Manufactura	Pedro Ramirez Castro	Lunes	07:00
277	A-MECA-2	Procesos de Manufactura	Pedro Ramirez Castro	Martes	07:00
278	A-MECA-2	Procesos de Manufactura	Pedro Ramirez Castro	Miercoles	07:00
279	A-MECA-2	Procesos de Manufactura	Pedro Ramirez Castro	Jueves	07:00
280	A-MECA-2	Procesos de Manufactura	Pedro Ramirez Castro	Viernes	07:00
281	A-MECA-2	Procesos de Manufactura	Pedro Ramirez Castro	Lunes	08:00
282	A-MECA-2	Hidraulica y Neumatica	Pedro Ramirez Castro	Lunes	09:00
283	A-MECA-2	Hidraulica y Neumatica	Pedro Ramirez Castro	Martes	08:00
284	A-MECA-2	Hidraulica y Neumatica	Pedro Ramirez Castro	Miercoles	08:00
285	A-MECA-2	Hidraulica y Neumatica	Pedro Ramirez Castro	Jueves	08:00
286	A-MECA-2	Hidraulica y Neumatica	Pedro Ramirez Castro	Viernes	08:00
287	A-MECA-2	Matematicas II Mec	Pedro Ramirez Castro	Lunes	10:00
288	A-MECA-2	Matematicas II Mec	Pedro Ramirez Castro	Martes	09:00
289	A-MECA-2	Matematicas II Mec	Pedro Ramirez Castro	Miercoles	09:00
290	A-MECA-2	Matematicas II Mec	Pedro Ramirez Castro	Jueves	09:00
291	A-MECA-2	Matematicas II Mec	Pedro Ramirez Castro	Viernes	09:00
292	B-MECA-2	Procesos de Manufactura	Pedro Ramirez Castro	Lunes	11:00
293	B-MECA-2	Procesos de Manufactura	Pedro Ramirez Castro	Martes	10:00
294	B-MECA-2	Procesos de Manufactura	Pedro Ramirez Castro	Miercoles	10:00
295	B-MECA-2	Procesos de Manufactura	Pedro Ramirez Castro	Jueves	10:00
296	A-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Lunes	07:00
297	A-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Martes	07:00
298	A-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Miercoles	07:00
299	A-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Jueves	07:00
300	A-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Viernes	07:00
301	A-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Lunes	08:00
302	A-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Martes	08:00
303	A-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Miercoles	08:00
304	A-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Jueves	08:00
305	A-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Viernes	08:00
306	A-ELEC-1	Seguridad Electrica	Valeria Mendoza Torres	Lunes	09:00
307	A-ELEC-1	Seguridad Electrica	Valeria Mendoza Torres	Martes	09:00
308	A-ELEC-1	Seguridad Electrica	Valeria Mendoza Torres	Miercoles	09:00
309	B-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Lunes	10:00
310	B-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Martes	10:00
311	B-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Miercoles	10:00
312	B-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Jueves	09:00
313	B-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Viernes	09:00
314	B-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Lunes	11:00
315	B-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Martes	11:00
316	B-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Miercoles	11:00
317	B-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Jueves	10:00
318	B-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Viernes	10:00
319	B-ELEC-1	Seguridad Electrica	Valeria Mendoza Torres	Lunes	12:00
320	B-ELEC-1	Seguridad Electrica	Valeria Mendoza Torres	Martes	12:00
321	B-ELEC-1	Seguridad Electrica	Valeria Mendoza Torres	Miercoles	12:00
322	C-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Lunes	13:00
323	C-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Martes	13:00
324	C-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Miercoles	13:00
325	C-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Jueves	11:00
326	C-ELEC-1	Circuitos Electricos I	Valeria Mendoza Torres	Viernes	11:00
327	C-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Jueves	12:00
328	C-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Viernes	12:00
329	C-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Jueves	13:00
330	C-ELEC-1	Matematicas I Elec	Valeria Mendoza Torres	Viernes	13:00
331	C-ELEC-1	Seguridad Electrica	Rodrigo Vega Montoya	Jueves	07:00
332	C-ELEC-1	Seguridad Electrica	Rodrigo Vega Montoya	Viernes	07:00
333	C-ELEC-1	Seguridad Electrica	Rodrigo Vega Montoya	Jueves	08:00
334	D-ELEC-1	Circuitos Electricos I	Rodrigo Vega Montoya	Jueves	09:00
335	D-ELEC-1	Circuitos Electricos I	Rodrigo Vega Montoya	Viernes	08:00
336	D-ELEC-1	Circuitos Electricos I	Rodrigo Vega Montoya	Jueves	10:00
337	D-ELEC-1	Circuitos Electricos I	Rodrigo Vega Montoya	Viernes	09:00
338	D-ELEC-1	Matematicas I Elec	Rodrigo Vega Montoya	Jueves	11:00
339	D-ELEC-1	Matematicas I Elec	Rodrigo Vega Montoya	Viernes	10:00
340	D-ELEC-1	Matematicas I Elec	Rodrigo Vega Montoya	Jueves	12:00
341	D-ELEC-1	Matematicas I Elec	Rodrigo Vega Montoya	Viernes	11:00
342	D-ELEC-1	Seguridad Electrica	Rodrigo Vega Montoya	Jueves	13:00
343	D-ELEC-1	Seguridad Electrica	Rodrigo Vega Montoya	Viernes	12:00
344	D-ELEC-1	Seguridad Electrica	Rodrigo Vega Montoya	Viernes	13:00
345	A-MECAT-1	Fundamentos de Mecatronica	Adrian Castillo Nunez	Lunes	07:00
346	A-MECAT-1	Fundamentos de Mecatronica	Adrian Castillo Nunez	Martes	07:00
347	A-MECAT-1	Fundamentos de Mecatronica	Adrian Castillo Nunez	Miercoles	07:00
348	A-MECAT-1	Fundamentos de Mecatronica	Adrian Castillo Nunez	Jueves	07:00
349	A-MECAT-1	Fundamentos de Mecatronica	Adrian Castillo Nunez	Viernes	07:00
350	A-MECAT-1	Matematicas I Mecat	Adrian Castillo Nunez	Lunes	08:00
351	A-MECAT-1	Matematicas I Mecat	Adrian Castillo Nunez	Martes	08:00
352	A-MECAT-1	Matematicas I Mecat	Adrian Castillo Nunez	Miercoles	08:00
353	A-MECAT-1	Matematicas I Mecat	Adrian Castillo Nunez	Jueves	08:00
354	A-MECAT-1	Matematicas I Mecat	Adrian Castillo Nunez	Viernes	08:00
355	A-MECAT-1	Dibujo Industrial	Adrian Castillo Nunez	Lunes	09:00
356	A-MECAT-1	Dibujo Industrial	Adrian Castillo Nunez	Martes	09:00
357	A-MECAT-1	Dibujo Industrial	Adrian Castillo Nunez	Miercoles	09:00
358	A-MECAT-1	Dibujo Industrial	Adrian Castillo Nunez	Jueves	09:00
359	B-MECAT-1	Fundamentos de Mecatronica	Adrian Castillo Nunez	Lunes	10:00
360	B-MECAT-1	Fundamentos de Mecatronica	Adrian Castillo Nunez	Martes	10:00
361	B-MECAT-1	Fundamentos de Mecatronica	Adrian Castillo Nunez	Miercoles	10:00
362	B-MECAT-1	Fundamentos de Mecatronica	Adrian Castillo Nunez	Jueves	10:00
363	B-MECAT-1	Fundamentos de Mecatronica	Adrian Castillo Nunez	Viernes	09:00
364	B-MECAT-1	Matematicas I Mecat	Adrian Castillo Nunez	Lunes	11:00
365	B-MECAT-1	Matematicas I Mecat	Adrian Castillo Nunez	Martes	11:00
366	B-MECAT-1	Matematicas I Mecat	Adrian Castillo Nunez	Miercoles	11:00
367	B-MECAT-1	Matematicas I Mecat	Adrian Castillo Nunez	Jueves	11:00
368	B-MECAT-1	Matematicas I Mecat	Adrian Castillo Nunez	Viernes	10:00
369	B-MECAT-1	Dibujo Industrial	Adrian Castillo Nunez	Lunes	12:00
370	B-MECAT-1	Dibujo Industrial	Adrian Castillo Nunez	Martes	12:00
371	B-MECAT-1	Dibujo Industrial	Adrian Castillo Nunez	Miercoles	12:00
372	B-MECAT-1	Dibujo Industrial	Adrian Castillo Nunez	Jueves	12:00
373	C-MECAT-1	Fundamentos de Mecatronica	Adrian Castillo Nunez	Lunes	13:00
374	C-MECAT-1	Fundamentos de Mecatronica	Adrian Castillo Nunez	Martes	13:00
375	C-MECAT-1	Matematicas I Mecat	Oscar Ibarra Fuentes	Miercoles	07:00
376	C-MECAT-1	Matematicas I Mecat	Oscar Ibarra Fuentes	Jueves	07:00
377	C-MECAT-1	Matematicas I Mecat	Oscar Ibarra Fuentes	Viernes	07:00
378	C-MECAT-1	Matematicas I Mecat	Oscar Ibarra Fuentes	Miercoles	08:00
379	C-MECAT-1	Matematicas I Mecat	Oscar Ibarra Fuentes	Jueves	08:00
380	C-MECAT-1	Dibujo Industrial	Oscar Ibarra Fuentes	Miercoles	09:00
381	C-MECAT-1	Dibujo Industrial	Oscar Ibarra Fuentes	Jueves	09:00
382	C-MECAT-1	Dibujo Industrial	Oscar Ibarra Fuentes	Viernes	08:00
383	C-MECAT-1	Dibujo Industrial	Oscar Ibarra Fuentes	Miercoles	10:00
384	D-MECAT-1	Fundamentos de Mecatronica	Oscar Ibarra Fuentes	Miercoles	11:00
385	H-CIBER-1	Fundamentos de Redes	Hector Guzman Reyes	Lunes	14:00
386	H-CIBER-1	Fundamentos de Redes	Hector Guzman Reyes	Martes	14:00
387	H-CIBER-1	Fundamentos de Redes	Hector Guzman Reyes	Miercoles	14:00
388	H-CIBER-1	Fundamentos de Redes	Hector Guzman Reyes	Jueves	14:00
389	H-CIBER-1	Fundamentos de Redes	Hector Guzman Reyes	Viernes	14:00
390	H-CIBER-1	Intro a Ciberseguridad	Hector Guzman Reyes	Lunes	15:00
391	H-CIBER-1	Intro a Ciberseguridad	Hector Guzman Reyes	Martes	15:00
392	H-CIBER-1	Intro a Ciberseguridad	Hector Guzman Reyes	Miercoles	15:00
393	H-CIBER-1	Intro a Ciberseguridad	Hector Guzman Reyes	Jueves	15:00
394	H-CIBER-1	Intro a Ciberseguridad	Hector Guzman Reyes	Viernes	15:00
395	H-CIBER-1	Sistemas Operativos Linux	Hector Guzman Reyes	Lunes	16:00
396	H-CIBER-1	Sistemas Operativos Linux	Hector Guzman Reyes	Martes	16:00
397	H-CIBER-1	Sistemas Operativos Linux	Hector Guzman Reyes	Miercoles	16:00
398	H-CIBER-1	Sistemas Operativos Linux	Hector Guzman Reyes	Jueves	16:00
399	I-CIBER-1	Fundamentos de Redes	Hector Guzman Reyes	Lunes	17:00
400	I-CIBER-1	Fundamentos de Redes	Hector Guzman Reyes	Martes	17:00
401	I-CIBER-1	Fundamentos de Redes	Hector Guzman Reyes	Miercoles	17:00
402	I-CIBER-1	Fundamentos de Redes	Hector Guzman Reyes	Jueves	17:00
403	I-CIBER-1	Fundamentos de Redes	Hector Guzman Reyes	Viernes	16:00
404	I-CIBER-1	Intro a Ciberseguridad	Hector Guzman Reyes	Lunes	18:00
405	I-CIBER-1	Intro a Ciberseguridad	Hector Guzman Reyes	Martes	18:00
406	I-CIBER-1	Intro a Ciberseguridad	Hector Guzman Reyes	Miercoles	18:00
407	I-CIBER-1	Intro a Ciberseguridad	Hector Guzman Reyes	Jueves	18:00
408	I-CIBER-1	Intro a Ciberseguridad	Hector Guzman Reyes	Viernes	17:00
409	I-CIBER-1	Sistemas Operativos Linux	Hector Guzman Reyes	Lunes	19:00
410	H-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Lunes	14:00
411	H-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Martes	14:00
412	H-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Miercoles	14:00
413	H-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Jueves	14:00
414	H-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Viernes	14:00
415	H-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Lunes	15:00
416	H-CIBER-2	Criptografia	Laura Diaz Morales	Lunes	16:00
417	H-CIBER-2	Criptografia	Laura Diaz Morales	Martes	15:00
418	H-CIBER-2	Criptografia	Laura Diaz Morales	Miercoles	15:00
419	H-CIBER-2	Criptografia	Laura Diaz Morales	Jueves	15:00
420	H-CIBER-2	Criptografia	Laura Diaz Morales	Viernes	15:00
421	H-CIBER-2	Hacking Etico I	Laura Diaz Morales	Lunes	17:00
422	H-CIBER-2	Hacking Etico I	Laura Diaz Morales	Martes	16:00
423	H-CIBER-2	Hacking Etico I	Laura Diaz Morales	Miercoles	16:00
424	H-CIBER-2	Hacking Etico I	Laura Diaz Morales	Jueves	16:00
425	H-CIBER-2	Hacking Etico I	Laura Diaz Morales	Viernes	16:00
426	I-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Lunes	18:00
427	I-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Martes	17:00
428	I-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Miercoles	17:00
429	I-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Jueves	17:00
430	I-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Viernes	17:00
431	I-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Lunes	19:00
432	I-CIBER-2	Criptografia	Laura Diaz Morales	Lunes	20:00
433	I-CIBER-2	Criptografia	Laura Diaz Morales	Martes	18:00
434	I-CIBER-2	Criptografia	Laura Diaz Morales	Miercoles	18:00
435	I-CIBER-2	Criptografia	Laura Diaz Morales	Jueves	18:00
436	I-CIBER-2	Criptografia	Laura Diaz Morales	Viernes	18:00
437	I-CIBER-2	Hacking Etico I	Laura Diaz Morales	Martes	19:00
438	I-CIBER-2	Hacking Etico I	Laura Diaz Morales	Miercoles	19:00
439	I-CIBER-2	Hacking Etico I	Laura Diaz Morales	Jueves	19:00
440	I-CIBER-2	Hacking Etico I	Laura Diaz Morales	Viernes	19:00
441	I-CIBER-2	Hacking Etico I	Laura Diaz Morales	Martes	20:00
442	J-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Miercoles	20:00
443	J-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Jueves	20:00
444	J-CIBER-2	Redes Avanzadas	Laura Diaz Morales	Viernes	20:00
445	A-GIT-1	Fundamentos de Turismo	Patricia Ortega Mendez	Lunes	07:00
446	A-GIT-1	Fundamentos de Turismo	Patricia Ortega Mendez	Martes	07:00
447	A-GIT-1	Fundamentos de Turismo	Patricia Ortega Mendez	Miercoles	07:00
448	A-GIT-1	Fundamentos de Turismo	Patricia Ortega Mendez	Jueves	07:00
449	A-GIT-1	Geografia Turistica	Patricia Ortega Mendez	Lunes	08:00
450	A-GIT-1	Geografia Turistica	Patricia Ortega Mendez	Martes	08:00
451	A-GIT-1	Geografia Turistica	Patricia Ortega Mendez	Miercoles	08:00
452	A-GIT-1	Geografia Turistica	Patricia Ortega Mendez	Jueves	08:00
453	A-GIT-1	Ingles para Turismo I	Patricia Ortega Mendez	Lunes	09:00
454	A-GIT-1	Ingles para Turismo I	Patricia Ortega Mendez	Martes	09:00
455	A-GIT-1	Ingles para Turismo I	Patricia Ortega Mendez	Miercoles	09:00
456	A-GIT-1	Ingles para Turismo I	Patricia Ortega Mendez	Jueves	09:00
457	B-GIT-1	Fundamentos de Turismo	Patricia Ortega Mendez	Lunes	10:00
458	B-GIT-1	Fundamentos de Turismo	Patricia Ortega Mendez	Martes	10:00
459	B-GIT-1	Fundamentos de Turismo	Patricia Ortega Mendez	Miercoles	10:00
460	B-GIT-1	Fundamentos de Turismo	Patricia Ortega Mendez	Jueves	10:00
461	B-GIT-1	Geografia Turistica	Patricia Ortega Mendez	Lunes	11:00
462	B-GIT-1	Geografia Turistica	Patricia Ortega Mendez	Martes	11:00
463	B-GIT-1	Geografia Turistica	Patricia Ortega Mendez	Miercoles	11:00
464	B-GIT-1	Geografia Turistica	Patricia Ortega Mendez	Jueves	11:00
465	B-GIT-1	Ingles para Turismo I	Daniela Cruz Maldonado	Lunes	07:00
466	B-GIT-1	Ingles para Turismo I	Daniela Cruz Maldonado	Martes	07:00
467	B-GIT-1	Ingles para Turismo I	Daniela Cruz Maldonado	Lunes	08:00
468	B-GIT-1	Ingles para Turismo I	Daniela Cruz Maldonado	Martes	08:00
469	C-GIT-1	Fundamentos de Turismo	Daniela Cruz Maldonado	Lunes	09:00
470	C-GIT-1	Fundamentos de Turismo	Daniela Cruz Maldonado	Martes	09:00
471	C-GIT-1	Fundamentos de Turismo	Daniela Cruz Maldonado	Lunes	10:00
472	C-GIT-1	Fundamentos de Turismo	Daniela Cruz Maldonado	Martes	10:00
473	C-GIT-1	Geografia Turistica	Daniela Cruz Maldonado	Lunes	11:00
474	C-GIT-1	Geografia Turistica	Daniela Cruz Maldonado	Martes	11:00
475	A-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Lunes	07:00
476	A-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Martes	07:00
477	A-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Miercoles	07:00
478	A-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Jueves	07:00
479	B-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Lunes	08:00
480	B-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Martes	08:00
481	B-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Miercoles	08:00
482	B-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Jueves	08:00
483	C-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Lunes	09:00
484	C-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Martes	09:00
485	C-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Miercoles	09:00
486	C-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Jueves	09:00
487	D-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Lunes	10:00
488	D-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Martes	10:00
489	D-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Miercoles	10:00
490	D-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Jueves	10:00
491	E-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Lunes	11:00
492	E-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Martes	11:00
493	E-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Miercoles	11:00
494	E-GIT-2	Ingles para Turismo II	Roberto Sanchez Torres	Jueves	11:00
495	A-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Lunes	07:00
496	A-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Martes	07:00
497	A-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Miercoles	07:00
498	A-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Jueves	07:00
499	A-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Viernes	07:00
500	A-IA-1	Matematicas para IA	Lucia Vargas Lopez	Lunes	08:00
501	A-IA-1	Matematicas para IA	Lucia Vargas Lopez	Martes	08:00
502	A-IA-1	Matematicas para IA	Lucia Vargas Lopez	Miercoles	08:00
503	A-IA-1	Matematicas para IA	Lucia Vargas Lopez	Jueves	08:00
504	A-IA-1	Matematicas para IA	Lucia Vargas Lopez	Viernes	08:00
505	A-IA-1	Programacion Python	Lucia Vargas Lopez	Lunes	09:00
506	A-IA-1	Programacion Python	Lucia Vargas Lopez	Martes	09:00
507	A-IA-1	Programacion Python	Lucia Vargas Lopez	Miercoles	09:00
508	A-IA-1	Programacion Python	Lucia Vargas Lopez	Jueves	09:00
509	A-IA-1	Programacion Python	Lucia Vargas Lopez	Viernes	09:00
510	B-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Lunes	10:00
511	B-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Martes	10:00
512	B-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Miercoles	10:00
513	B-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Jueves	10:00
514	B-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Viernes	10:00
515	B-IA-1	Matematicas para IA	Lucia Vargas Lopez	Lunes	11:00
516	B-IA-1	Matematicas para IA	Lucia Vargas Lopez	Martes	11:00
517	B-IA-1	Matematicas para IA	Lucia Vargas Lopez	Miercoles	11:00
518	B-IA-1	Matematicas para IA	Lucia Vargas Lopez	Jueves	11:00
519	B-IA-1	Matematicas para IA	Lucia Vargas Lopez	Viernes	11:00
520	B-IA-1	Programacion Python	Lucia Vargas Lopez	Lunes	12:00
521	B-IA-1	Programacion Python	Lucia Vargas Lopez	Martes	12:00
522	B-IA-1	Programacion Python	Lucia Vargas Lopez	Miercoles	12:00
523	B-IA-1	Programacion Python	Lucia Vargas Lopez	Jueves	12:00
524	B-IA-1	Programacion Python	Lucia Vargas Lopez	Viernes	12:00
525	C-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Lunes	13:00
526	C-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Martes	13:00
527	C-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Miercoles	13:00
528	C-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Jueves	13:00
529	C-IA-1	Fundamentos de IA	Lucia Vargas Lopez	Viernes	13:00
530	H-IA-1	Fundamentos de IA	Claudia Pena Avila	Lunes	14:00
531	H-IA-1	Fundamentos de IA	Claudia Pena Avila	Martes	14:00
532	H-IA-1	Fundamentos de IA	Claudia Pena Avila	Miercoles	14:00
533	H-IA-1	Fundamentos de IA	Claudia Pena Avila	Lunes	15:00
534	H-IA-1	Fundamentos de IA	Claudia Pena Avila	Martes	15:00
535	H-IA-1	Matematicas para IA	Claudia Pena Avila	Lunes	16:00
536	H-IA-1	Matematicas para IA	Claudia Pena Avila	Martes	16:00
537	H-IA-1	Matematicas para IA	Claudia Pena Avila	Miercoles	15:00
538	H-IA-1	Matematicas para IA	Claudia Pena Avila	Lunes	17:00
539	H-IA-1	Matematicas para IA	Claudia Pena Avila	Martes	17:00
540	H-IA-1	Programacion Python	Claudia Pena Avila	Lunes	18:00
541	H-IA-1	Programacion Python	Claudia Pena Avila	Martes	18:00
542	H-IA-1	Programacion Python	Claudia Pena Avila	Miercoles	16:00
543	H-IA-1	Programacion Python	Claudia Pena Avila	Lunes	19:00
544	H-IA-1	Programacion Python	Claudia Pena Avila	Martes	19:00
\.


--
-- Data for Name: horario_profesores; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.horario_profesores (id, rfc_profesor, nombre_profesor, materia, grupo, dia, hora) FROM stdin;
1	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	A-PROG-1	Lunes	07:00
2	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	A-PROG-1	Martes	07:00
3	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	A-PROG-1	Miercoles	07:00
4	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	A-PROG-1	Jueves	07:00
5	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	A-PROG-1	Viernes	07:00
6	PELJ800101	Juan Perez Lopez	Matematicas I	A-PROG-1	Lunes	08:00
7	PELJ800101	Juan Perez Lopez	Matematicas I	A-PROG-1	Martes	08:00
8	PELJ800101	Juan Perez Lopez	Matematicas I	A-PROG-1	Miercoles	08:00
9	PELJ800101	Juan Perez Lopez	Matematicas I	A-PROG-1	Jueves	08:00
10	PELJ800101	Juan Perez Lopez	Matematicas I	A-PROG-1	Viernes	08:00
11	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	A-PROG-1	Lunes	09:00
12	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	A-PROG-1	Martes	09:00
13	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	A-PROG-1	Miercoles	09:00
14	LOMA850420	Ana Lopez Mendez	Introduccion a TI	A-PROG-1	Lunes	10:00
15	LOMA850420	Ana Lopez Mendez	Introduccion a TI	A-PROG-1	Martes	10:00
16	LOMA850420	Ana Lopez Mendez	Introduccion a TI	A-PROG-1	Miercoles	10:00
17	LOMA850420	Ana Lopez Mendez	Introduccion a TI	A-PROG-1	Jueves	09:00
18	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	B-PROG-1	Lunes	09:00
19	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	B-PROG-1	Martes	09:00
20	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	B-PROG-1	Miercoles	09:00
21	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	B-PROG-1	Jueves	09:00
22	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	B-PROG-1	Viernes	09:00
23	PELJ800101	Juan Perez Lopez	Matematicas I	B-PROG-1	Lunes	10:00
24	PELJ800101	Juan Perez Lopez	Matematicas I	B-PROG-1	Martes	10:00
25	PELJ800101	Juan Perez Lopez	Matematicas I	B-PROG-1	Miercoles	10:00
26	PELJ800101	Juan Perez Lopez	Matematicas I	B-PROG-1	Jueves	10:00
27	PELJ800101	Juan Perez Lopez	Matematicas I	B-PROG-1	Viernes	10:00
28	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	B-PROG-1	Lunes	07:00
29	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	B-PROG-1	Martes	07:00
30	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	B-PROG-1	Miercoles	07:00
31	LOMA850420	Ana Lopez Mendez	Introduccion a TI	B-PROG-1	Lunes	08:00
32	LOMA850420	Ana Lopez Mendez	Introduccion a TI	B-PROG-1	Martes	08:00
33	LOMA850420	Ana Lopez Mendez	Introduccion a TI	B-PROG-1	Miercoles	08:00
34	LOMA850420	Ana Lopez Mendez	Introduccion a TI	B-PROG-1	Jueves	07:00
35	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	C-PROG-1	Lunes	11:00
36	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	C-PROG-1	Martes	11:00
37	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	C-PROG-1	Miercoles	11:00
38	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	C-PROG-1	Jueves	11:00
39	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	C-PROG-1	Viernes	11:00
40	PELJ800101	Juan Perez Lopez	Matematicas I	C-PROG-1	Lunes	12:00
41	PELJ800101	Juan Perez Lopez	Matematicas I	C-PROG-1	Martes	12:00
42	PELJ800101	Juan Perez Lopez	Matematicas I	C-PROG-1	Miercoles	12:00
43	PELJ800101	Juan Perez Lopez	Matematicas I	C-PROG-1	Jueves	12:00
44	PELJ800101	Juan Perez Lopez	Matematicas I	C-PROG-1	Viernes	12:00
45	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	C-PROG-1	Lunes	08:00
46	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	C-PROG-1	Martes	08:00
47	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	C-PROG-1	Miercoles	08:00
48	LOMA850420	Ana Lopez Mendez	Introduccion a TI	C-PROG-1	Lunes	07:00
49	LOMA850420	Ana Lopez Mendez	Introduccion a TI	C-PROG-1	Martes	07:00
50	LOMA850420	Ana Lopez Mendez	Introduccion a TI	C-PROG-1	Miercoles	07:00
51	LOMA850420	Ana Lopez Mendez	Introduccion a TI	C-PROG-1	Jueves	08:00
52	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	D-PROG-1	Lunes	13:00
53	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	D-PROG-1	Martes	13:00
54	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	D-PROG-1	Miercoles	13:00
55	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	D-PROG-1	Jueves	13:00
56	PELJ800101	Juan Perez Lopez	Fundamentos de Programacion	D-PROG-1	Viernes	13:00
57	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	D-PROG-1	Lunes	10:00
58	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	D-PROG-1	Martes	10:00
59	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	D-PROG-1	Miercoles	10:00
60	LOMA850420	Ana Lopez Mendez	Introduccion a TI	D-PROG-1	Lunes	09:00
61	LOMA850420	Ana Lopez Mendez	Introduccion a TI	D-PROG-1	Martes	09:00
62	LOMA850420	Ana Lopez Mendez	Introduccion a TI	D-PROG-1	Miercoles	09:00
63	LOMA850420	Ana Lopez Mendez	Introduccion a TI	D-PROG-1	Jueves	10:00
64	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	E-PROG-1	Lunes	11:00
65	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	E-PROG-1	Martes	11:00
66	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	E-PROG-1	Miercoles	11:00
67	LOMA850420	Ana Lopez Mendez	Introduccion a TI	E-PROG-1	Lunes	12:00
68	LOMA850420	Ana Lopez Mendez	Introduccion a TI	E-PROG-1	Martes	12:00
69	LOMA850420	Ana Lopez Mendez	Introduccion a TI	E-PROG-1	Miercoles	12:00
70	LOMA850420	Ana Lopez Mendez	Introduccion a TI	E-PROG-1	Jueves	11:00
71	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	F-PROG-1	Lunes	12:00
72	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	F-PROG-1	Martes	12:00
73	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	F-PROG-1	Miercoles	12:00
74	LOMA850420	Ana Lopez Mendez	Introduccion a TI	F-PROG-1	Lunes	11:00
75	LOMA850420	Ana Lopez Mendez	Introduccion a TI	F-PROG-1	Martes	11:00
76	LOMA850420	Ana Lopez Mendez	Introduccion a TI	F-PROG-1	Miercoles	11:00
77	LOMA850420	Ana Lopez Mendez	Introduccion a TI	F-PROG-1	Jueves	12:00
78	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	G-PROG-1	Lunes	13:00
79	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	G-PROG-1	Martes	13:00
80	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	G-PROG-1	Miercoles	13:00
81	LOMA850420	Ana Lopez Mendez	Introduccion a TI	G-PROG-1	Jueves	13:00
82	LOMA850420	Ana Lopez Mendez	Introduccion a TI	G-PROG-1	Viernes	07:00
83	LOMA850420	Ana Lopez Mendez	Introduccion a TI	G-PROG-1	Viernes	08:00
84	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	H-PROG-1	Lunes	14:00
85	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	H-PROG-1	Martes	14:00
86	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	H-PROG-1	Miercoles	14:00
87	LOMA850420	Ana Lopez Mendez	Introduccion a TI	H-PROG-1	Lunes	15:00
88	LOMA850420	Ana Lopez Mendez	Introduccion a TI	H-PROG-1	Martes	15:00
89	LOMA850420	Ana Lopez Mendez	Introduccion a TI	H-PROG-1	Miercoles	15:00
90	LOMA850420	Ana Lopez Mendez	Introduccion a TI	H-PROG-1	Jueves	14:00
91	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	I-PROG-1	Lunes	15:00
92	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	I-PROG-1	Martes	15:00
93	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	I-PROG-1	Miercoles	15:00
94	LOMA850420	Ana Lopez Mendez	Introduccion a TI	I-PROG-1	Lunes	14:00
95	LOMA850420	Ana Lopez Mendez	Introduccion a TI	I-PROG-1	Martes	14:00
96	LOMA850420	Ana Lopez Mendez	Introduccion a TI	I-PROG-1	Miercoles	14:00
97	LOMA850420	Ana Lopez Mendez	Introduccion a TI	I-PROG-1	Jueves	15:00
98	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	J-PROG-1	Lunes	16:00
99	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	J-PROG-1	Martes	16:00
100	HESC820310	Carlos Hernandez Soto	Ingles Tecnico I	J-PROG-1	Miercoles	16:00
101	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	A-PROG-3	Lunes	07:00
102	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	A-PROG-3	Martes	07:00
103	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	A-PROG-3	Miercoles	07:00
104	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	A-PROG-3	Jueves	07:00
105	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	A-PROG-3	Viernes	07:00
106	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	A-PROG-3	Lunes	08:00
107	GORM790215	Maria Gomez Ruiz	Base de Datos II	A-PROG-3	Lunes	09:00
108	GORM790215	Maria Gomez Ruiz	Base de Datos II	A-PROG-3	Martes	08:00
109	GORM790215	Maria Gomez Ruiz	Base de Datos II	A-PROG-3	Miercoles	08:00
110	GORM790215	Maria Gomez Ruiz	Base de Datos II	A-PROG-3	Jueves	08:00
111	GORM790215	Maria Gomez Ruiz	Base de Datos II	A-PROG-3	Viernes	08:00
112	RICF820750	Fernanda Rios Castellanos	Redes I	A-PROG-3	Lunes	10:00
113	RICF820750	Fernanda Rios Castellanos	Redes I	A-PROG-3	Martes	09:00
114	RICF820750	Fernanda Rios Castellanos	Redes I	A-PROG-3	Miercoles	09:00
115	RICF820750	Fernanda Rios Castellanos	Redes I	A-PROG-3	Jueves	09:00
116	RICF820750	Fernanda Rios Castellanos	Ingles Tecnico III	A-PROG-3	Lunes	11:00
117	RICF820750	Fernanda Rios Castellanos	Ingles Tecnico III	A-PROG-3	Martes	10:00
118	RICF820750	Fernanda Rios Castellanos	Ingles Tecnico III	A-PROG-3	Miercoles	10:00
119	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	B-PROG-3	Lunes	10:00
120	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	B-PROG-3	Martes	09:00
121	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	B-PROG-3	Miercoles	09:00
122	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	B-PROG-3	Jueves	09:00
123	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	B-PROG-3	Viernes	09:00
124	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	B-PROG-3	Lunes	11:00
125	GORM790215	Maria Gomez Ruiz	Base de Datos II	B-PROG-3	Lunes	12:00
126	GORM790215	Maria Gomez Ruiz	Base de Datos II	B-PROG-3	Martes	10:00
127	GORM790215	Maria Gomez Ruiz	Base de Datos II	B-PROG-3	Miercoles	10:00
128	GORM790215	Maria Gomez Ruiz	Base de Datos II	B-PROG-3	Jueves	10:00
129	GORM790215	Maria Gomez Ruiz	Base de Datos II	B-PROG-3	Viernes	10:00
130	RICF820750	Fernanda Rios Castellanos	Redes I	B-PROG-3	Lunes	07:00
131	RICF820750	Fernanda Rios Castellanos	Redes I	B-PROG-3	Martes	07:00
132	RICF820750	Fernanda Rios Castellanos	Redes I	B-PROG-3	Miercoles	07:00
133	RICF820750	Fernanda Rios Castellanos	Redes I	B-PROG-3	Jueves	07:00
134	RICF820750	Fernanda Rios Castellanos	Ingles Tecnico III	B-PROG-3	Lunes	08:00
135	RICF820750	Fernanda Rios Castellanos	Ingles Tecnico III	B-PROG-3	Martes	08:00
136	RICF820750	Fernanda Rios Castellanos	Ingles Tecnico III	B-PROG-3	Miercoles	08:00
137	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	C-PROG-3	Lunes	13:00
138	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	C-PROG-3	Martes	11:00
139	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	C-PROG-3	Miercoles	11:00
140	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	C-PROG-3	Jueves	11:00
141	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	C-PROG-3	Viernes	11:00
142	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	C-PROG-3	Martes	12:00
143	GORM790215	Maria Gomez Ruiz	Base de Datos II	C-PROG-3	Martes	13:00
144	GORM790215	Maria Gomez Ruiz	Base de Datos II	C-PROG-3	Miercoles	12:00
145	GORM790215	Maria Gomez Ruiz	Base de Datos II	C-PROG-3	Jueves	12:00
146	GORM790215	Maria Gomez Ruiz	Base de Datos II	C-PROG-3	Viernes	12:00
147	GORM790215	Maria Gomez Ruiz	Base de Datos II	C-PROG-3	Miercoles	13:00
148	RICF820750	Fernanda Rios Castellanos	Redes I	C-PROG-3	Lunes	09:00
149	RICF820750	Fernanda Rios Castellanos	Redes I	C-PROG-3	Jueves	08:00
150	RICF820750	Fernanda Rios Castellanos	Redes I	C-PROG-3	Viernes	07:00
151	RICF820750	Fernanda Rios Castellanos	Redes I	C-PROG-3	Lunes	12:00
152	RICF820750	Fernanda Rios Castellanos	Ingles Tecnico III	C-PROG-3	Jueves	10:00
228	MORD880750	Diego Morales Ruiz	IA Aplicada	B-PROG-6	Viernes	09:00
153	RICF820750	Fernanda Rios Castellanos	Ingles Tecnico III	C-PROG-3	Viernes	08:00
154	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	D-PROG-3	Jueves	13:00
155	GORM790215	Maria Gomez Ruiz	Desarrollo Web Front-End	D-PROG-3	Viernes	13:00
156	TOVS860640	Sofia Torres Vega	Programacion Movil I	A-PROG-4	Lunes	07:00
157	TOVS860640	Sofia Torres Vega	Programacion Movil I	A-PROG-4	Martes	07:00
158	TOVS860640	Sofia Torres Vega	Programacion Movil I	A-PROG-4	Miercoles	07:00
159	TOVS860640	Sofia Torres Vega	Programacion Movil I	A-PROG-4	Jueves	07:00
160	TOVS860640	Sofia Torres Vega	Programacion Movil I	A-PROG-4	Viernes	07:00
161	TOVS860640	Sofia Torres Vega	Programacion Movil I	A-PROG-4	Lunes	08:00
162	FLGE780310	Elena Flores Garcia	Seguridad Informatica	A-PROG-4	Lunes	09:00
163	FLGE780310	Elena Flores Garcia	Seguridad Informatica	A-PROG-4	Martes	08:00
164	FLGE780310	Elena Flores Garcia	Seguridad Informatica	A-PROG-4	Miercoles	08:00
165	FLGE780310	Elena Flores Garcia	Seguridad Informatica	A-PROG-4	Jueves	08:00
166	TOVS860640	Sofia Torres Vega	Programacion Movil I	B-PROG-4	Lunes	09:00
167	TOVS860640	Sofia Torres Vega	Programacion Movil I	B-PROG-4	Martes	08:00
168	TOVS860640	Sofia Torres Vega	Programacion Movil I	B-PROG-4	Miercoles	08:00
169	TOVS860640	Sofia Torres Vega	Programacion Movil I	B-PROG-4	Jueves	08:00
170	TOVS860640	Sofia Torres Vega	Programacion Movil I	B-PROG-4	Viernes	08:00
171	TOVS860640	Sofia Torres Vega	Programacion Movil I	B-PROG-4	Lunes	10:00
172	FLGE780310	Elena Flores Garcia	Seguridad Informatica	B-PROG-4	Lunes	07:00
173	FLGE780310	Elena Flores Garcia	Seguridad Informatica	B-PROG-4	Martes	07:00
174	FLGE780310	Elena Flores Garcia	Seguridad Informatica	B-PROG-4	Miercoles	07:00
175	FLGE780310	Elena Flores Garcia	Seguridad Informatica	B-PROG-4	Jueves	07:00
176	TOVS860640	Sofia Torres Vega	Programacion Movil I	C-PROG-4	Lunes	11:00
177	TOVS860640	Sofia Torres Vega	Programacion Movil I	C-PROG-4	Martes	09:00
178	TOVS860640	Sofia Torres Vega	Programacion Movil I	C-PROG-4	Miercoles	09:00
179	TOVS860640	Sofia Torres Vega	Programacion Movil I	C-PROG-4	Jueves	09:00
180	TOVS860640	Sofia Torres Vega	Programacion Movil I	C-PROG-4	Viernes	09:00
181	TOVS860640	Sofia Torres Vega	Programacion Movil I	C-PROG-4	Lunes	12:00
182	FLGE780310	Elena Flores Garcia	Seguridad Informatica	C-PROG-4	Lunes	08:00
183	FLGE780310	Elena Flores Garcia	Seguridad Informatica	C-PROG-4	Martes	10:00
184	FLGE780310	Elena Flores Garcia	Seguridad Informatica	C-PROG-4	Miercoles	10:00
185	FLGE780310	Elena Flores Garcia	Seguridad Informatica	C-PROG-4	Jueves	10:00
186	TOVS860640	Sofia Torres Vega	Programacion Movil I	D-PROG-4	Lunes	13:00
187	TOVS860640	Sofia Torres Vega	Programacion Movil I	D-PROG-4	Martes	10:00
188	TOVS860640	Sofia Torres Vega	Programacion Movil I	D-PROG-4	Miercoles	10:00
189	TOVS860640	Sofia Torres Vega	Programacion Movil I	D-PROG-4	Jueves	10:00
190	TOVS860640	Sofia Torres Vega	Programacion Movil I	D-PROG-4	Viernes	10:00
191	TOVS860640	Sofia Torres Vega	Programacion Movil I	D-PROG-4	Martes	11:00
192	FLGE780310	Elena Flores Garcia	Seguridad Informatica	D-PROG-4	Lunes	10:00
193	FLGE780310	Elena Flores Garcia	Seguridad Informatica	D-PROG-4	Martes	09:00
194	FLGE780310	Elena Flores Garcia	Seguridad Informatica	D-PROG-4	Miercoles	09:00
195	FLGE780310	Elena Flores Garcia	Seguridad Informatica	D-PROG-4	Jueves	09:00
196	TOVS860640	Sofia Torres Vega	Programacion Movil I	E-PROG-4	Martes	12:00
197	TOVS860640	Sofia Torres Vega	Programacion Movil I	E-PROG-4	Miercoles	11:00
198	TOVS860640	Sofia Torres Vega	Programacion Movil I	E-PROG-4	Jueves	11:00
199	TOVS860640	Sofia Torres Vega	Programacion Movil I	E-PROG-4	Viernes	11:00
200	TOVS860640	Sofia Torres Vega	Programacion Movil I	E-PROG-4	Martes	13:00
201	TOVS860640	Sofia Torres Vega	Programacion Movil I	E-PROG-4	Miercoles	12:00
202	FLGE780310	Elena Flores Garcia	Seguridad Informatica	E-PROG-4	Lunes	11:00
203	FLGE780310	Elena Flores Garcia	Seguridad Informatica	E-PROG-4	Martes	11:00
204	FLGE780310	Elena Flores Garcia	Seguridad Informatica	E-PROG-4	Miercoles	13:00
205	FLGE780310	Elena Flores Garcia	Seguridad Informatica	E-PROG-4	Jueves	12:00
206	MORD880750	Diego Morales Ruiz	IA Aplicada	A-PROG-6	Lunes	07:00
207	MORD880750	Diego Morales Ruiz	IA Aplicada	A-PROG-6	Martes	07:00
208	MORD880750	Diego Morales Ruiz	IA Aplicada	A-PROG-6	Miercoles	07:00
209	MORD880750	Diego Morales Ruiz	IA Aplicada	A-PROG-6	Jueves	07:00
210	MORD880750	Diego Morales Ruiz	IA Aplicada	A-PROG-6	Viernes	07:00
211	MORD880750	Diego Morales Ruiz	IA Aplicada	A-PROG-6	Lunes	08:00
212	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	A-PROG-6	Lunes	09:00
213	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	A-PROG-6	Martes	08:00
214	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	A-PROG-6	Miercoles	08:00
215	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	A-PROG-6	Jueves	08:00
216	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	A-PROG-6	Viernes	08:00
217	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	A-PROG-6	Lunes	10:00
218	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	A-PROG-6	Martes	09:00
219	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	A-PROG-6	Miercoles	09:00
220	MORD880750	Diego Morales Ruiz	Emprendimiento TI	A-PROG-6	Lunes	11:00
221	MORD880750	Diego Morales Ruiz	Emprendimiento TI	A-PROG-6	Martes	10:00
222	MORD880750	Diego Morales Ruiz	Emprendimiento TI	A-PROG-6	Miercoles	10:00
223	MORD880750	Diego Morales Ruiz	Emprendimiento TI	A-PROG-6	Jueves	09:00
224	MORD880750	Diego Morales Ruiz	IA Aplicada	B-PROG-6	Lunes	12:00
225	MORD880750	Diego Morales Ruiz	IA Aplicada	B-PROG-6	Martes	11:00
226	MORD880750	Diego Morales Ruiz	IA Aplicada	B-PROG-6	Miercoles	11:00
227	MORD880750	Diego Morales Ruiz	IA Aplicada	B-PROG-6	Jueves	10:00
229	MORD880750	Diego Morales Ruiz	IA Aplicada	B-PROG-6	Lunes	13:00
230	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	B-PROG-6	Martes	12:00
231	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	B-PROG-6	Miercoles	12:00
232	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	B-PROG-6	Jueves	11:00
233	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	B-PROG-6	Viernes	10:00
234	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	B-PROG-6	Martes	13:00
235	MORD880750	Diego Morales Ruiz	Proyecto Integrador II	B-PROG-6	Miercoles	13:00
236	SARM850860	Miguel Santana Reyna	Emprendimiento TI	B-PROG-6	Lunes	07:00
237	SARM850860	Miguel Santana Reyna	Emprendimiento TI	B-PROG-6	Martes	07:00
238	SARM850860	Miguel Santana Reyna	Emprendimiento TI	B-PROG-6	Miercoles	07:00
239	SARM850860	Miguel Santana Reyna	Emprendimiento TI	B-PROG-6	Lunes	08:00
240	SARM850860	Miguel Santana Reyna	IA Aplicada	C-PROG-6	Lunes	09:00
241	SARM850860	Miguel Santana Reyna	IA Aplicada	C-PROG-6	Martes	08:00
242	SARM850860	Miguel Santana Reyna	IA Aplicada	C-PROG-6	Miercoles	08:00
243	SARM850860	Miguel Santana Reyna	IA Aplicada	C-PROG-6	Lunes	10:00
244	SARM850860	Miguel Santana Reyna	IA Aplicada	C-PROG-6	Martes	09:00
245	SARM850860	Miguel Santana Reyna	IA Aplicada	C-PROG-6	Miercoles	09:00
246	SARM850860	Miguel Santana Reyna	Proyecto Integrador II	C-PROG-6	Lunes	11:00
247	SARM850860	Miguel Santana Reyna	Proyecto Integrador II	C-PROG-6	Martes	10:00
248	SARM850860	Miguel Santana Reyna	Proyecto Integrador II	C-PROG-6	Miercoles	10:00
249	SARM850860	Miguel Santana Reyna	Proyecto Integrador II	C-PROG-6	Lunes	12:00
250	SARM850860	Miguel Santana Reyna	Proyecto Integrador II	C-PROG-6	Martes	11:00
251	RISM790420	Marco Rivera Salinas	Dibujo Tecnico	A-MECA-1	Lunes	07:00
252	RISM790420	Marco Rivera Salinas	Dibujo Tecnico	A-MECA-1	Martes	07:00
253	RISM790420	Marco Rivera Salinas	Dibujo Tecnico	A-MECA-1	Miercoles	07:00
254	RISM790420	Marco Rivera Salinas	Dibujo Tecnico	A-MECA-1	Jueves	07:00
255	RISM790420	Marco Rivera Salinas	Matematicas I Mec	A-MECA-1	Lunes	08:00
256	RISM790420	Marco Rivera Salinas	Matematicas I Mec	A-MECA-1	Martes	08:00
257	RISM790420	Marco Rivera Salinas	Matematicas I Mec	A-MECA-1	Miercoles	08:00
258	RISM790420	Marco Rivera Salinas	Matematicas I Mec	A-MECA-1	Jueves	08:00
259	RISM790420	Marco Rivera Salinas	Matematicas I Mec	A-MECA-1	Viernes	07:00
260	RISM790420	Marco Rivera Salinas	Seguridad Industrial	A-MECA-1	Lunes	09:00
261	RISM790420	Marco Rivera Salinas	Seguridad Industrial	A-MECA-1	Martes	09:00
262	RISM790420	Marco Rivera Salinas	Seguridad Industrial	A-MECA-1	Miercoles	09:00
263	RISM790420	Marco Rivera Salinas	Dibujo Tecnico	B-MECA-1	Lunes	10:00
264	RISM790420	Marco Rivera Salinas	Dibujo Tecnico	B-MECA-1	Martes	10:00
265	RISM790420	Marco Rivera Salinas	Dibujo Tecnico	B-MECA-1	Miercoles	10:00
266	RISM790420	Marco Rivera Salinas	Dibujo Tecnico	B-MECA-1	Jueves	09:00
267	RISM790420	Marco Rivera Salinas	Matematicas I Mec	B-MECA-1	Lunes	11:00
268	RISM790420	Marco Rivera Salinas	Matematicas I Mec	B-MECA-1	Martes	11:00
269	RISM790420	Marco Rivera Salinas	Matematicas I Mec	B-MECA-1	Miercoles	11:00
270	RISM790420	Marco Rivera Salinas	Matematicas I Mec	B-MECA-1	Jueves	10:00
271	RISM790420	Marco Rivera Salinas	Matematicas I Mec	B-MECA-1	Viernes	08:00
272	RISM790420	Marco Rivera Salinas	Seguridad Industrial	B-MECA-1	Lunes	12:00
273	RISM790420	Marco Rivera Salinas	Seguridad Industrial	B-MECA-1	Martes	12:00
274	RISM790420	Marco Rivera Salinas	Seguridad Industrial	B-MECA-1	Miercoles	12:00
275	RISM790420	Marco Rivera Salinas	Dibujo Tecnico	C-MECA-1	Lunes	13:00
276	RACP750101	Pedro Ramirez Castro	Procesos de Manufactura	A-MECA-2	Lunes	07:00
277	RACP750101	Pedro Ramirez Castro	Procesos de Manufactura	A-MECA-2	Martes	07:00
278	RACP750101	Pedro Ramirez Castro	Procesos de Manufactura	A-MECA-2	Miercoles	07:00
279	RACP750101	Pedro Ramirez Castro	Procesos de Manufactura	A-MECA-2	Jueves	07:00
280	RACP750101	Pedro Ramirez Castro	Procesos de Manufactura	A-MECA-2	Viernes	07:00
281	RACP750101	Pedro Ramirez Castro	Procesos de Manufactura	A-MECA-2	Lunes	08:00
282	RACP750101	Pedro Ramirez Castro	Hidraulica y Neumatica	A-MECA-2	Lunes	09:00
283	RACP750101	Pedro Ramirez Castro	Hidraulica y Neumatica	A-MECA-2	Martes	08:00
284	RACP750101	Pedro Ramirez Castro	Hidraulica y Neumatica	A-MECA-2	Miercoles	08:00
285	RACP750101	Pedro Ramirez Castro	Hidraulica y Neumatica	A-MECA-2	Jueves	08:00
286	RACP750101	Pedro Ramirez Castro	Hidraulica y Neumatica	A-MECA-2	Viernes	08:00
287	RACP750101	Pedro Ramirez Castro	Matematicas II Mec	A-MECA-2	Lunes	10:00
288	RACP750101	Pedro Ramirez Castro	Matematicas II Mec	A-MECA-2	Martes	09:00
289	RACP750101	Pedro Ramirez Castro	Matematicas II Mec	A-MECA-2	Miercoles	09:00
290	RACP750101	Pedro Ramirez Castro	Matematicas II Mec	A-MECA-2	Jueves	09:00
291	RACP750101	Pedro Ramirez Castro	Matematicas II Mec	A-MECA-2	Viernes	09:00
292	RACP750101	Pedro Ramirez Castro	Procesos de Manufactura	B-MECA-2	Lunes	11:00
293	RACP750101	Pedro Ramirez Castro	Procesos de Manufactura	B-MECA-2	Martes	10:00
294	RACP750101	Pedro Ramirez Castro	Procesos de Manufactura	B-MECA-2	Miercoles	10:00
295	RACP750101	Pedro Ramirez Castro	Procesos de Manufactura	B-MECA-2	Jueves	10:00
296	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	A-ELEC-1	Lunes	07:00
297	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	A-ELEC-1	Martes	07:00
298	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	A-ELEC-1	Miercoles	07:00
299	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	A-ELEC-1	Jueves	07:00
300	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	A-ELEC-1	Viernes	07:00
301	METV910920	Valeria Mendoza Torres	Matematicas I Elec	A-ELEC-1	Lunes	08:00
302	METV910920	Valeria Mendoza Torres	Matematicas I Elec	A-ELEC-1	Martes	08:00
303	METV910920	Valeria Mendoza Torres	Matematicas I Elec	A-ELEC-1	Miercoles	08:00
304	METV910920	Valeria Mendoza Torres	Matematicas I Elec	A-ELEC-1	Jueves	08:00
305	METV910920	Valeria Mendoza Torres	Matematicas I Elec	A-ELEC-1	Viernes	08:00
306	METV910920	Valeria Mendoza Torres	Seguridad Electrica	A-ELEC-1	Lunes	09:00
307	METV910920	Valeria Mendoza Torres	Seguridad Electrica	A-ELEC-1	Martes	09:00
308	METV910920	Valeria Mendoza Torres	Seguridad Electrica	A-ELEC-1	Miercoles	09:00
309	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	B-ELEC-1	Lunes	10:00
310	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	B-ELEC-1	Martes	10:00
311	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	B-ELEC-1	Miercoles	10:00
312	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	B-ELEC-1	Jueves	09:00
313	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	B-ELEC-1	Viernes	09:00
314	METV910920	Valeria Mendoza Torres	Matematicas I Elec	B-ELEC-1	Lunes	11:00
315	METV910920	Valeria Mendoza Torres	Matematicas I Elec	B-ELEC-1	Martes	11:00
316	METV910920	Valeria Mendoza Torres	Matematicas I Elec	B-ELEC-1	Miercoles	11:00
317	METV910920	Valeria Mendoza Torres	Matematicas I Elec	B-ELEC-1	Jueves	10:00
318	METV910920	Valeria Mendoza Torres	Matematicas I Elec	B-ELEC-1	Viernes	10:00
319	METV910920	Valeria Mendoza Torres	Seguridad Electrica	B-ELEC-1	Lunes	12:00
320	METV910920	Valeria Mendoza Torres	Seguridad Electrica	B-ELEC-1	Martes	12:00
321	METV910920	Valeria Mendoza Torres	Seguridad Electrica	B-ELEC-1	Miercoles	12:00
322	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	C-ELEC-1	Lunes	13:00
323	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	C-ELEC-1	Martes	13:00
324	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	C-ELEC-1	Miercoles	13:00
325	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	C-ELEC-1	Jueves	11:00
326	METV910920	Valeria Mendoza Torres	Circuitos Electricos I	C-ELEC-1	Viernes	11:00
327	METV910920	Valeria Mendoza Torres	Matematicas I Elec	C-ELEC-1	Jueves	12:00
328	METV910920	Valeria Mendoza Torres	Matematicas I Elec	C-ELEC-1	Viernes	12:00
329	METV910920	Valeria Mendoza Torres	Matematicas I Elec	C-ELEC-1	Jueves	13:00
330	METV910920	Valeria Mendoza Torres	Matematicas I Elec	C-ELEC-1	Viernes	13:00
331	VEMR870920	Rodrigo Vega Montoya	Seguridad Electrica	C-ELEC-1	Jueves	07:00
332	VEMR870920	Rodrigo Vega Montoya	Seguridad Electrica	C-ELEC-1	Viernes	07:00
333	VEMR870920	Rodrigo Vega Montoya	Seguridad Electrica	C-ELEC-1	Jueves	08:00
334	VEMR870920	Rodrigo Vega Montoya	Circuitos Electricos I	D-ELEC-1	Jueves	09:00
335	VEMR870920	Rodrigo Vega Montoya	Circuitos Electricos I	D-ELEC-1	Viernes	08:00
336	VEMR870920	Rodrigo Vega Montoya	Circuitos Electricos I	D-ELEC-1	Jueves	10:00
337	VEMR870920	Rodrigo Vega Montoya	Circuitos Electricos I	D-ELEC-1	Viernes	09:00
338	VEMR870920	Rodrigo Vega Montoya	Matematicas I Elec	D-ELEC-1	Jueves	11:00
339	VEMR870920	Rodrigo Vega Montoya	Matematicas I Elec	D-ELEC-1	Viernes	10:00
340	VEMR870920	Rodrigo Vega Montoya	Matematicas I Elec	D-ELEC-1	Jueves	12:00
341	VEMR870920	Rodrigo Vega Montoya	Matematicas I Elec	D-ELEC-1	Viernes	11:00
342	VEMR870920	Rodrigo Vega Montoya	Seguridad Electrica	D-ELEC-1	Jueves	13:00
343	VEMR870920	Rodrigo Vega Montoya	Seguridad Electrica	D-ELEC-1	Viernes	12:00
344	VEMR870920	Rodrigo Vega Montoya	Seguridad Electrica	D-ELEC-1	Viernes	13:00
345	CANA900910	Adrian Castillo Nunez	Fundamentos de Mecatronica	A-MECAT-1	Lunes	07:00
346	CANA900910	Adrian Castillo Nunez	Fundamentos de Mecatronica	A-MECAT-1	Martes	07:00
347	CANA900910	Adrian Castillo Nunez	Fundamentos de Mecatronica	A-MECAT-1	Miercoles	07:00
348	CANA900910	Adrian Castillo Nunez	Fundamentos de Mecatronica	A-MECAT-1	Jueves	07:00
349	CANA900910	Adrian Castillo Nunez	Fundamentos de Mecatronica	A-MECAT-1	Viernes	07:00
350	CANA900910	Adrian Castillo Nunez	Matematicas I Mecat	A-MECAT-1	Lunes	08:00
351	CANA900910	Adrian Castillo Nunez	Matematicas I Mecat	A-MECAT-1	Martes	08:00
352	CANA900910	Adrian Castillo Nunez	Matematicas I Mecat	A-MECAT-1	Miercoles	08:00
353	CANA900910	Adrian Castillo Nunez	Matematicas I Mecat	A-MECAT-1	Jueves	08:00
354	CANA900910	Adrian Castillo Nunez	Matematicas I Mecat	A-MECAT-1	Viernes	08:00
355	CANA900910	Adrian Castillo Nunez	Dibujo Industrial	A-MECAT-1	Lunes	09:00
356	CANA900910	Adrian Castillo Nunez	Dibujo Industrial	A-MECAT-1	Martes	09:00
357	CANA900910	Adrian Castillo Nunez	Dibujo Industrial	A-MECAT-1	Miercoles	09:00
358	CANA900910	Adrian Castillo Nunez	Dibujo Industrial	A-MECAT-1	Jueves	09:00
359	CANA900910	Adrian Castillo Nunez	Fundamentos de Mecatronica	B-MECAT-1	Lunes	10:00
360	CANA900910	Adrian Castillo Nunez	Fundamentos de Mecatronica	B-MECAT-1	Martes	10:00
361	CANA900910	Adrian Castillo Nunez	Fundamentos de Mecatronica	B-MECAT-1	Miercoles	10:00
362	CANA900910	Adrian Castillo Nunez	Fundamentos de Mecatronica	B-MECAT-1	Jueves	10:00
363	CANA900910	Adrian Castillo Nunez	Fundamentos de Mecatronica	B-MECAT-1	Viernes	09:00
364	CANA900910	Adrian Castillo Nunez	Matematicas I Mecat	B-MECAT-1	Lunes	11:00
365	CANA900910	Adrian Castillo Nunez	Matematicas I Mecat	B-MECAT-1	Martes	11:00
366	CANA900910	Adrian Castillo Nunez	Matematicas I Mecat	B-MECAT-1	Miercoles	11:00
367	CANA900910	Adrian Castillo Nunez	Matematicas I Mecat	B-MECAT-1	Jueves	11:00
368	CANA900910	Adrian Castillo Nunez	Matematicas I Mecat	B-MECAT-1	Viernes	10:00
369	CANA900910	Adrian Castillo Nunez	Dibujo Industrial	B-MECAT-1	Lunes	12:00
370	CANA900910	Adrian Castillo Nunez	Dibujo Industrial	B-MECAT-1	Martes	12:00
371	CANA900910	Adrian Castillo Nunez	Dibujo Industrial	B-MECAT-1	Miercoles	12:00
372	CANA900910	Adrian Castillo Nunez	Dibujo Industrial	B-MECAT-1	Jueves	12:00
373	CANA900910	Adrian Castillo Nunez	Fundamentos de Mecatronica	C-MECAT-1	Lunes	13:00
374	CANA900910	Adrian Castillo Nunez	Fundamentos de Mecatronica	C-MECAT-1	Martes	13:00
375	IBFO890215	Oscar Ibarra Fuentes	Matematicas I Mecat	C-MECAT-1	Miercoles	07:00
376	IBFO890215	Oscar Ibarra Fuentes	Matematicas I Mecat	C-MECAT-1	Jueves	07:00
377	IBFO890215	Oscar Ibarra Fuentes	Matematicas I Mecat	C-MECAT-1	Viernes	07:00
378	IBFO890215	Oscar Ibarra Fuentes	Matematicas I Mecat	C-MECAT-1	Miercoles	08:00
379	IBFO890215	Oscar Ibarra Fuentes	Matematicas I Mecat	C-MECAT-1	Jueves	08:00
380	IBFO890215	Oscar Ibarra Fuentes	Dibujo Industrial	C-MECAT-1	Miercoles	09:00
381	IBFO890215	Oscar Ibarra Fuentes	Dibujo Industrial	C-MECAT-1	Jueves	09:00
382	IBFO890215	Oscar Ibarra Fuentes	Dibujo Industrial	C-MECAT-1	Viernes	08:00
383	IBFO890215	Oscar Ibarra Fuentes	Dibujo Industrial	C-MECAT-1	Miercoles	10:00
384	IBFO890215	Oscar Ibarra Fuentes	Fundamentos de Mecatronica	D-MECAT-1	Miercoles	11:00
385	GURH810640	Hector Guzman Reyes	Fundamentos de Redes	H-CIBER-1	Lunes	14:00
386	GURH810640	Hector Guzman Reyes	Fundamentos de Redes	H-CIBER-1	Martes	14:00
387	GURH810640	Hector Guzman Reyes	Fundamentos de Redes	H-CIBER-1	Miercoles	14:00
388	GURH810640	Hector Guzman Reyes	Fundamentos de Redes	H-CIBER-1	Jueves	14:00
389	GURH810640	Hector Guzman Reyes	Fundamentos de Redes	H-CIBER-1	Viernes	14:00
390	GURH810640	Hector Guzman Reyes	Intro a Ciberseguridad	H-CIBER-1	Lunes	15:00
391	GURH810640	Hector Guzman Reyes	Intro a Ciberseguridad	H-CIBER-1	Martes	15:00
392	GURH810640	Hector Guzman Reyes	Intro a Ciberseguridad	H-CIBER-1	Miercoles	15:00
393	GURH810640	Hector Guzman Reyes	Intro a Ciberseguridad	H-CIBER-1	Jueves	15:00
394	GURH810640	Hector Guzman Reyes	Intro a Ciberseguridad	H-CIBER-1	Viernes	15:00
395	GURH810640	Hector Guzman Reyes	Sistemas Operativos Linux	H-CIBER-1	Lunes	16:00
396	GURH810640	Hector Guzman Reyes	Sistemas Operativos Linux	H-CIBER-1	Martes	16:00
397	GURH810640	Hector Guzman Reyes	Sistemas Operativos Linux	H-CIBER-1	Miercoles	16:00
398	GURH810640	Hector Guzman Reyes	Sistemas Operativos Linux	H-CIBER-1	Jueves	16:00
399	GURH810640	Hector Guzman Reyes	Fundamentos de Redes	I-CIBER-1	Lunes	17:00
400	GURH810640	Hector Guzman Reyes	Fundamentos de Redes	I-CIBER-1	Martes	17:00
401	GURH810640	Hector Guzman Reyes	Fundamentos de Redes	I-CIBER-1	Miercoles	17:00
402	GURH810640	Hector Guzman Reyes	Fundamentos de Redes	I-CIBER-1	Jueves	17:00
403	GURH810640	Hector Guzman Reyes	Fundamentos de Redes	I-CIBER-1	Viernes	16:00
404	GURH810640	Hector Guzman Reyes	Intro a Ciberseguridad	I-CIBER-1	Lunes	18:00
405	GURH810640	Hector Guzman Reyes	Intro a Ciberseguridad	I-CIBER-1	Martes	18:00
406	GURH810640	Hector Guzman Reyes	Intro a Ciberseguridad	I-CIBER-1	Miercoles	18:00
407	GURH810640	Hector Guzman Reyes	Intro a Ciberseguridad	I-CIBER-1	Jueves	18:00
408	GURH810640	Hector Guzman Reyes	Intro a Ciberseguridad	I-CIBER-1	Viernes	17:00
409	GURH810640	Hector Guzman Reyes	Sistemas Operativos Linux	I-CIBER-1	Lunes	19:00
410	DIML870530	Laura Diaz Morales	Redes Avanzadas	H-CIBER-2	Lunes	14:00
411	DIML870530	Laura Diaz Morales	Redes Avanzadas	H-CIBER-2	Martes	14:00
412	DIML870530	Laura Diaz Morales	Redes Avanzadas	H-CIBER-2	Miercoles	14:00
413	DIML870530	Laura Diaz Morales	Redes Avanzadas	H-CIBER-2	Jueves	14:00
414	DIML870530	Laura Diaz Morales	Redes Avanzadas	H-CIBER-2	Viernes	14:00
415	DIML870530	Laura Diaz Morales	Redes Avanzadas	H-CIBER-2	Lunes	15:00
416	DIML870530	Laura Diaz Morales	Criptografia	H-CIBER-2	Lunes	16:00
417	DIML870530	Laura Diaz Morales	Criptografia	H-CIBER-2	Martes	15:00
418	DIML870530	Laura Diaz Morales	Criptografia	H-CIBER-2	Miercoles	15:00
419	DIML870530	Laura Diaz Morales	Criptografia	H-CIBER-2	Jueves	15:00
420	DIML870530	Laura Diaz Morales	Criptografia	H-CIBER-2	Viernes	15:00
421	DIML870530	Laura Diaz Morales	Hacking Etico I	H-CIBER-2	Lunes	17:00
422	DIML870530	Laura Diaz Morales	Hacking Etico I	H-CIBER-2	Martes	16:00
423	DIML870530	Laura Diaz Morales	Hacking Etico I	H-CIBER-2	Miercoles	16:00
424	DIML870530	Laura Diaz Morales	Hacking Etico I	H-CIBER-2	Jueves	16:00
425	DIML870530	Laura Diaz Morales	Hacking Etico I	H-CIBER-2	Viernes	16:00
426	DIML870530	Laura Diaz Morales	Redes Avanzadas	I-CIBER-2	Lunes	18:00
427	DIML870530	Laura Diaz Morales	Redes Avanzadas	I-CIBER-2	Martes	17:00
428	DIML870530	Laura Diaz Morales	Redes Avanzadas	I-CIBER-2	Miercoles	17:00
429	DIML870530	Laura Diaz Morales	Redes Avanzadas	I-CIBER-2	Jueves	17:00
430	DIML870530	Laura Diaz Morales	Redes Avanzadas	I-CIBER-2	Viernes	17:00
431	DIML870530	Laura Diaz Morales	Redes Avanzadas	I-CIBER-2	Lunes	19:00
432	DIML870530	Laura Diaz Morales	Criptografia	I-CIBER-2	Lunes	20:00
433	DIML870530	Laura Diaz Morales	Criptografia	I-CIBER-2	Martes	18:00
434	DIML870530	Laura Diaz Morales	Criptografia	I-CIBER-2	Miercoles	18:00
435	DIML870530	Laura Diaz Morales	Criptografia	I-CIBER-2	Jueves	18:00
436	DIML870530	Laura Diaz Morales	Criptografia	I-CIBER-2	Viernes	18:00
437	DIML870530	Laura Diaz Morales	Hacking Etico I	I-CIBER-2	Martes	19:00
438	DIML870530	Laura Diaz Morales	Hacking Etico I	I-CIBER-2	Miercoles	19:00
439	DIML870530	Laura Diaz Morales	Hacking Etico I	I-CIBER-2	Jueves	19:00
440	DIML870530	Laura Diaz Morales	Hacking Etico I	I-CIBER-2	Viernes	19:00
441	DIML870530	Laura Diaz Morales	Hacking Etico I	I-CIBER-2	Martes	20:00
442	DIML870530	Laura Diaz Morales	Redes Avanzadas	J-CIBER-2	Miercoles	20:00
443	DIML870530	Laura Diaz Morales	Redes Avanzadas	J-CIBER-2	Jueves	20:00
444	DIML870530	Laura Diaz Morales	Redes Avanzadas	J-CIBER-2	Viernes	20:00
445	ORMP800530	Patricia Ortega Mendez	Fundamentos de Turismo	A-GIT-1	Lunes	07:00
446	ORMP800530	Patricia Ortega Mendez	Fundamentos de Turismo	A-GIT-1	Martes	07:00
447	ORMP800530	Patricia Ortega Mendez	Fundamentos de Turismo	A-GIT-1	Miercoles	07:00
448	ORMP800530	Patricia Ortega Mendez	Fundamentos de Turismo	A-GIT-1	Jueves	07:00
449	ORMP800530	Patricia Ortega Mendez	Geografia Turistica	A-GIT-1	Lunes	08:00
450	ORMP800530	Patricia Ortega Mendez	Geografia Turistica	A-GIT-1	Martes	08:00
451	ORMP800530	Patricia Ortega Mendez	Geografia Turistica	A-GIT-1	Miercoles	08:00
452	ORMP800530	Patricia Ortega Mendez	Geografia Turistica	A-GIT-1	Jueves	08:00
453	ORMP800530	Patricia Ortega Mendez	Ingles para Turismo I	A-GIT-1	Lunes	09:00
454	ORMP800530	Patricia Ortega Mendez	Ingles para Turismo I	A-GIT-1	Martes	09:00
455	ORMP800530	Patricia Ortega Mendez	Ingles para Turismo I	A-GIT-1	Miercoles	09:00
456	ORMP800530	Patricia Ortega Mendez	Ingles para Turismo I	A-GIT-1	Jueves	09:00
457	ORMP800530	Patricia Ortega Mendez	Fundamentos de Turismo	B-GIT-1	Lunes	10:00
458	ORMP800530	Patricia Ortega Mendez	Fundamentos de Turismo	B-GIT-1	Martes	10:00
459	ORMP800530	Patricia Ortega Mendez	Fundamentos de Turismo	B-GIT-1	Miercoles	10:00
460	ORMP800530	Patricia Ortega Mendez	Fundamentos de Turismo	B-GIT-1	Jueves	10:00
461	ORMP800530	Patricia Ortega Mendez	Geografia Turistica	B-GIT-1	Lunes	11:00
462	ORMP800530	Patricia Ortega Mendez	Geografia Turistica	B-GIT-1	Martes	11:00
463	ORMP800530	Patricia Ortega Mendez	Geografia Turistica	B-GIT-1	Miercoles	11:00
464	ORMP800530	Patricia Ortega Mendez	Geografia Turistica	B-GIT-1	Jueves	11:00
465	CRMD880101	Daniela Cruz Maldonado	Ingles para Turismo I	B-GIT-1	Lunes	07:00
466	CRMD880101	Daniela Cruz Maldonado	Ingles para Turismo I	B-GIT-1	Martes	07:00
467	CRMD880101	Daniela Cruz Maldonado	Ingles para Turismo I	B-GIT-1	Lunes	08:00
468	CRMD880101	Daniela Cruz Maldonado	Ingles para Turismo I	B-GIT-1	Martes	08:00
469	CRMD880101	Daniela Cruz Maldonado	Fundamentos de Turismo	C-GIT-1	Lunes	09:00
470	CRMD880101	Daniela Cruz Maldonado	Fundamentos de Turismo	C-GIT-1	Martes	09:00
471	CRMD880101	Daniela Cruz Maldonado	Fundamentos de Turismo	C-GIT-1	Lunes	10:00
472	CRMD880101	Daniela Cruz Maldonado	Fundamentos de Turismo	C-GIT-1	Martes	10:00
473	CRMD880101	Daniela Cruz Maldonado	Geografia Turistica	C-GIT-1	Lunes	11:00
474	CRMD880101	Daniela Cruz Maldonado	Geografia Turistica	C-GIT-1	Martes	11:00
475	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	A-GIT-2	Lunes	07:00
476	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	A-GIT-2	Martes	07:00
477	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	A-GIT-2	Miercoles	07:00
478	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	A-GIT-2	Jueves	07:00
479	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	B-GIT-2	Lunes	08:00
480	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	B-GIT-2	Martes	08:00
481	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	B-GIT-2	Miercoles	08:00
482	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	B-GIT-2	Jueves	08:00
483	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	C-GIT-2	Lunes	09:00
484	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	C-GIT-2	Martes	09:00
485	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	C-GIT-2	Miercoles	09:00
486	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	C-GIT-2	Jueves	09:00
487	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	D-GIT-2	Lunes	10:00
488	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	D-GIT-2	Martes	10:00
489	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	D-GIT-2	Miercoles	10:00
490	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	D-GIT-2	Jueves	10:00
491	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	E-GIT-2	Lunes	11:00
492	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	E-GIT-2	Martes	11:00
493	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	E-GIT-2	Miercoles	11:00
494	SATR760215	Roberto Sanchez Torres	Ingles para Turismo II	E-GIT-2	Jueves	11:00
495	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	A-IA-1	Lunes	07:00
496	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	A-IA-1	Martes	07:00
497	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	A-IA-1	Miercoles	07:00
498	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	A-IA-1	Jueves	07:00
499	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	A-IA-1	Viernes	07:00
500	VALL890860	Lucia Vargas Lopez	Matematicas para IA	A-IA-1	Lunes	08:00
501	VALL890860	Lucia Vargas Lopez	Matematicas para IA	A-IA-1	Martes	08:00
502	VALL890860	Lucia Vargas Lopez	Matematicas para IA	A-IA-1	Miercoles	08:00
503	VALL890860	Lucia Vargas Lopez	Matematicas para IA	A-IA-1	Jueves	08:00
504	VALL890860	Lucia Vargas Lopez	Matematicas para IA	A-IA-1	Viernes	08:00
505	VALL890860	Lucia Vargas Lopez	Programacion Python	A-IA-1	Lunes	09:00
506	VALL890860	Lucia Vargas Lopez	Programacion Python	A-IA-1	Martes	09:00
507	VALL890860	Lucia Vargas Lopez	Programacion Python	A-IA-1	Miercoles	09:00
508	VALL890860	Lucia Vargas Lopez	Programacion Python	A-IA-1	Jueves	09:00
509	VALL890860	Lucia Vargas Lopez	Programacion Python	A-IA-1	Viernes	09:00
510	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	B-IA-1	Lunes	10:00
511	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	B-IA-1	Martes	10:00
512	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	B-IA-1	Miercoles	10:00
513	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	B-IA-1	Jueves	10:00
514	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	B-IA-1	Viernes	10:00
515	VALL890860	Lucia Vargas Lopez	Matematicas para IA	B-IA-1	Lunes	11:00
516	VALL890860	Lucia Vargas Lopez	Matematicas para IA	B-IA-1	Martes	11:00
517	VALL890860	Lucia Vargas Lopez	Matematicas para IA	B-IA-1	Miercoles	11:00
518	VALL890860	Lucia Vargas Lopez	Matematicas para IA	B-IA-1	Jueves	11:00
519	VALL890860	Lucia Vargas Lopez	Matematicas para IA	B-IA-1	Viernes	11:00
520	VALL890860	Lucia Vargas Lopez	Programacion Python	B-IA-1	Lunes	12:00
521	VALL890860	Lucia Vargas Lopez	Programacion Python	B-IA-1	Martes	12:00
522	VALL890860	Lucia Vargas Lopez	Programacion Python	B-IA-1	Miercoles	12:00
523	VALL890860	Lucia Vargas Lopez	Programacion Python	B-IA-1	Jueves	12:00
524	VALL890860	Lucia Vargas Lopez	Programacion Python	B-IA-1	Viernes	12:00
525	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	C-IA-1	Lunes	13:00
526	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	C-IA-1	Martes	13:00
527	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	C-IA-1	Miercoles	13:00
528	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	C-IA-1	Jueves	13:00
529	VALL890860	Lucia Vargas Lopez	Fundamentos de IA	C-IA-1	Viernes	13:00
530	PEAC860910	Claudia Pena Avila	Fundamentos de IA	H-IA-1	Lunes	14:00
531	PEAC860910	Claudia Pena Avila	Fundamentos de IA	H-IA-1	Martes	14:00
532	PEAC860910	Claudia Pena Avila	Fundamentos de IA	H-IA-1	Miercoles	14:00
533	PEAC860910	Claudia Pena Avila	Fundamentos de IA	H-IA-1	Lunes	15:00
534	PEAC860910	Claudia Pena Avila	Fundamentos de IA	H-IA-1	Martes	15:00
535	PEAC860910	Claudia Pena Avila	Matematicas para IA	H-IA-1	Lunes	16:00
536	PEAC860910	Claudia Pena Avila	Matematicas para IA	H-IA-1	Martes	16:00
537	PEAC860910	Claudia Pena Avila	Matematicas para IA	H-IA-1	Miercoles	15:00
538	PEAC860910	Claudia Pena Avila	Matematicas para IA	H-IA-1	Lunes	17:00
539	PEAC860910	Claudia Pena Avila	Matematicas para IA	H-IA-1	Martes	17:00
540	PEAC860910	Claudia Pena Avila	Programacion Python	H-IA-1	Lunes	18:00
541	PEAC860910	Claudia Pena Avila	Programacion Python	H-IA-1	Martes	18:00
542	PEAC860910	Claudia Pena Avila	Programacion Python	H-IA-1	Miercoles	16:00
543	PEAC860910	Claudia Pena Avila	Programacion Python	H-IA-1	Lunes	19:00
544	PEAC860910	Claudia Pena Avila	Programacion Python	H-IA-1	Martes	19:00
\.


--
-- Data for Name: materia_grupos; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.materia_grupos (materia_id, grupo_id) FROM stdin;
\.


--
-- Data for Name: materias; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.materias (id_materia, id_especialidad, id_semestre, nombre, clave, horas_semanales) FROM stdin;
1	1	1	Fundamentos de Programacion	PROG101	5
2	1	1	Matematicas I	PROG102	5
3	1	1	Ingles Tecnico I	PROG103	3
4	1	1	Introduccion a TI	PROG104	4
5	1	2	Programacion Orientada a Objetos	PROG201	6
6	1	2	Matematicas II	PROG202	5
7	1	2	Base de Datos I	PROG203	5
8	1	2	Ingles Tecnico II	PROG204	3
9	1	3	Desarrollo Web Front-End	PROG301	6
10	1	3	Base de Datos II	PROG302	5
11	1	3	Redes I	PROG303	4
12	1	3	Ingles Tecnico III	PROG304	3
13	1	4	Desarrollo Web Back-End	PROG401	6
14	1	4	Programacion Movil I	PROG402	6
15	1	4	Seguridad Informatica	PROG403	4
16	1	5	Desarrollo de Software	PROG501	6
17	1	5	Programacion Movil II	PROG502	6
18	1	5	Proyecto Integrador I	PROG503	5
19	1	6	IA Aplicada	PROG601	6
20	1	6	Proyecto Integrador II	PROG602	8
21	1	6	Emprendimiento TI	PROG603	4
22	2	1	Dibujo Tecnico	MECA101	4
23	2	1	Matematicas I Mec	MECA102	5
24	2	1	Seguridad Industrial	MECA103	3
25	2	2	Procesos de Manufactura	MECA201	6
26	2	2	Hidraulica y Neumatica	MECA202	5
27	2	2	Matematicas II Mec	MECA203	5
28	2	3	Control de Calidad	MECA301	5
29	2	3	Maquinas y Herramientas	MECA302	6
30	2	3	Automatizacion Industrial	MECA303	5
31	3	1	Circuitos Electricos I	ELEC101	5
32	3	1	Matematicas I Elec	ELEC102	5
33	3	1	Seguridad Electrica	ELEC103	3
34	3	2	Circuitos Electricos II	ELEC201	6
35	3	2	Instalaciones Electricas	ELEC202	5
36	3	2	Electronica Basica	ELEC203	5
37	3	3	Maquinas Electricas	ELEC301	6
38	3	3	Control Electrico	ELEC302	5
39	3	3	Sistemas de Potencia	ELEC303	5
40	4	1	Fundamentos de Mecatronica	MECAT101	5
41	4	1	Matematicas I Mecat	MECAT102	5
42	4	1	Dibujo Industrial	MECAT103	4
43	4	2	Electronica Digital	MECAT201	6
44	4	2	Programacion de PLC	MECAT202	5
45	4	2	Neumatica	MECAT203	5
46	4	3	Robotica Industrial	MECAT301	6
47	4	3	Automatizacion Mecat	MECAT302	5
48	4	3	Control de Sistemas	MECAT303	5
49	5	1	Fundamentos de Redes	CIBER101	5
50	5	1	Intro a Ciberseguridad	CIBER102	5
51	5	1	Sistemas Operativos Linux	CIBER103	4
52	5	2	Redes Avanzadas	CIBER201	6
53	5	2	Criptografia	CIBER202	5
54	5	2	Hacking Etico I	CIBER203	5
55	5	3	Hacking Etico II	CIBER301	6
56	5	3	Forense Digital	CIBER302	5
57	5	3	Seguridad en Aplicaciones	CIBER303	5
58	6	1	Fundamentos de Turismo	GIT101	4
59	6	1	Geografia Turistica	GIT102	4
60	6	1	Ingles para Turismo I	GIT103	4
61	6	2	Administracion Turistica	GIT201	5
62	6	2	Patrimonio Cultural	GIT202	4
63	6	2	Ingles para Turismo II	GIT203	4
64	6	3	Marketing Turistico	GIT301	5
65	6	3	Gestion Hotelera	GIT302	5
66	6	3	Proyecto Turistico	GIT303	5
67	7	1	Fundamentos de IA	IA101	5
68	7	1	Matematicas para IA	IA102	5
69	7	1	Programacion Python	IA103	5
70	7	2	Machine Learning I	IA201	6
71	7	2	Estadistica Aplicada	IA202	5
72	7	2	Vision Computacional	IA203	5
73	7	3	Machine Learning II	IA301	6
74	7	3	Procesamiento de Lenguaje Natural	IA302	5
75	7	3	Proyecto de IA	IA303	6
\.


--
-- Data for Name: plan_estudio; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.plan_estudio (id_plan, id_especialidad, nombre, horas_asignadas) FROM stdin;
1	1	Plan de Estudios Ingenieria en Software	0
2	2	Plan de Estudios Inteligencia Artificial	0
3	3	Plan de Estudios Redes y Ciberseguridad	0
4	4	Plan de Estudios Desarrollo Movil	0
5	5	Plan de Estudios Ciencia de Datos	0
\.


--
-- Data for Name: plan_estudio_materia; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.plan_estudio_materia (id, id_plan, id_materia) FROM stdin;
\.


--
-- Data for Name: profesor; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.profesor (id_profesor, nombre, apellidos, email, telefono, pregrado, fecha_ingreso, prioridad, horas_academicas, rfc) FROM stdin;
1	Juan	Perez Lopez	juan.perez@cbtis22.edu.mx	\N	\N	2015-01-15	1	35	PELJ800101
2	Maria	Gomez Ruiz	maria.gomez@cbtis22.edu.mx	\N	\N	2016-08-01	1	35	GORM790215
3	Carlos	Hernandez Soto	carlos.hernandez@cbtis22.edu.mx	\N	\N	2017-03-10	1	30	HESC820310
4	Ana	Lopez Mendez	ana.lopez@cbtis22.edu.mx	\N	\N	2018-01-20	1	35	LOMA850420
5	Laura	Diaz Morales	laura.diaz@cbtis22.edu.mx	\N	\N	2019-06-15	1	35	DIML870530
6	Sofia	Torres Vega	sofia.torres@cbtis22.edu.mx	\N	\N	2020-11-10	1	30	TOVS860640
7	Diego	Morales Ruiz	diego.morales@cbtis22.edu.mx	\N	\N	2021-04-05	1	30	MORD880750
8	Lucia	Vargas Lopez	lucia.vargas@cbtis22.edu.mx	\N	\N	2021-01-12	1	35	VALL890860
9	Adrian	Castillo Nunez	adrian.castillo@cbtis22.edu.mx	\N	\N	2022-03-01	1	30	CANA900910
10	Valeria	Mendoza Torres	valeria.mendoza@cbtis22.edu.mx	\N	\N	2022-09-01	1	35	METV910920
11	Pedro	Ramirez Castro	pedro.ramirez@cbtis22.edu.mx	\N	\N	2020-02-05	2	20	RACP750101
12	Roberto	Sanchez Torres	roberto.sanchez@cbtis22.edu.mx	\N	\N	2021-08-01	2	20	SATR760215
13	Elena	Flores Garcia	elena.flores@cbtis22.edu.mx	\N	\N	2022-03-10	2	20	FLGE780310
14	Marco	Rivera Salinas	marco.rivera@cbtis22.edu.mx	\N	\N	2022-01-20	2	25	RISM790420
15	Patricia	Ortega Mendez	patricia.ortega@cbtis22.edu.mx	\N	\N	2023-06-15	2	20	ORMP800530
16	Hector	Guzman Reyes	hector.guzman@cbtis22.edu.mx	\N	\N	2023-11-10	2	25	GURH810640
17	Fernanda	Rios Castellanos	fernanda.rios@cbtis22.edu.mx	\N	\N	2023-04-05	2	20	RICF820750
18	Miguel	Santana Reyna	miguel.santana@cbtis22.edu.mx	\N	\N	2024-01-12	3	15	SARM850860
19	Claudia	Pena Avila	claudia.pena@cbtis22.edu.mx	\N	\N	2024-03-01	3	15	PEAC860910
20	Rodrigo	Vega Montoya	rodrigo.vega@cbtis22.edu.mx	\N	\N	2024-09-01	3	15	VEMR870920
21	Daniela	Cruz Maldonado	daniela.cruz@cbtis22.edu.mx	\N	\N	2025-01-15	3	10	CRMD880101
22	Oscar	Ibarra Fuentes	oscar.ibarra@cbtis22.edu.mx	\N	\N	2025-08-01	3	10	IBFO890215
\.


--
-- Data for Name: profesor_disponibilidad; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.profesor_disponibilidad (id, profesor_id, dia, hora_inicio, hora_fin) FROM stdin;
1	1	Lunes	07:00	14:00
2	1	Martes	07:00	14:00
3	1	Miercoles	07:00	14:00
4	1	Jueves	07:00	14:00
5	1	Viernes	07:00	14:00
6	1	Lunes	14:00	21:00
7	1	Martes	14:00	21:00
8	1	Miercoles	14:00	21:00
9	1	Jueves	14:00	21:00
10	1	Viernes	14:00	21:00
11	2	Lunes	07:00	14:00
12	2	Martes	07:00	14:00
13	2	Miercoles	07:00	14:00
14	2	Jueves	07:00	14:00
15	2	Viernes	07:00	14:00
16	2	Lunes	14:00	21:00
17	2	Martes	14:00	21:00
18	2	Miercoles	14:00	21:00
19	2	Jueves	14:00	21:00
20	2	Viernes	14:00	21:00
21	3	Lunes	07:00	14:00
22	3	Martes	07:00	14:00
23	3	Miercoles	07:00	14:00
24	3	Jueves	07:00	14:00
25	3	Viernes	07:00	14:00
26	3	Lunes	14:00	21:00
27	3	Martes	14:00	21:00
28	3	Miercoles	14:00	21:00
29	3	Jueves	14:00	21:00
30	3	Viernes	14:00	21:00
31	4	Lunes	07:00	14:00
32	4	Martes	07:00	14:00
33	4	Miercoles	07:00	14:00
34	4	Jueves	07:00	14:00
35	4	Viernes	07:00	14:00
36	4	Lunes	14:00	21:00
37	4	Martes	14:00	21:00
38	4	Miercoles	14:00	21:00
39	4	Jueves	14:00	21:00
40	4	Viernes	14:00	21:00
41	5	Lunes	14:00	21:00
42	5	Martes	14:00	21:00
43	5	Miercoles	14:00	21:00
44	5	Jueves	14:00	21:00
45	5	Viernes	14:00	21:00
46	6	Lunes	07:00	14:00
47	6	Martes	07:00	14:00
48	6	Miercoles	07:00	14:00
49	6	Jueves	07:00	14:00
50	6	Viernes	07:00	14:00
51	6	Lunes	14:00	21:00
52	6	Martes	14:00	21:00
53	6	Miercoles	14:00	21:00
54	6	Jueves	14:00	21:00
55	6	Viernes	14:00	21:00
56	7	Lunes	07:00	14:00
57	7	Martes	07:00	14:00
58	7	Miercoles	07:00	14:00
59	7	Jueves	07:00	14:00
60	7	Viernes	07:00	14:00
61	7	Lunes	14:00	21:00
62	7	Martes	14:00	21:00
63	7	Miercoles	14:00	21:00
64	7	Jueves	14:00	21:00
65	7	Viernes	14:00	21:00
66	8	Lunes	07:00	14:00
67	8	Martes	07:00	14:00
68	8	Miercoles	07:00	14:00
69	8	Jueves	07:00	14:00
70	8	Viernes	07:00	14:00
71	8	Lunes	14:00	21:00
72	8	Martes	14:00	21:00
73	8	Miercoles	14:00	21:00
74	8	Jueves	14:00	21:00
75	8	Viernes	14:00	21:00
76	9	Lunes	07:00	14:00
77	9	Martes	07:00	14:00
78	9	Miercoles	07:00	14:00
79	9	Jueves	07:00	14:00
80	9	Viernes	07:00	14:00
81	10	Lunes	07:00	14:00
82	10	Martes	07:00	14:00
83	10	Miercoles	07:00	14:00
84	10	Jueves	07:00	14:00
85	10	Viernes	07:00	14:00
86	10	Lunes	14:00	21:00
87	10	Martes	14:00	21:00
88	10	Miercoles	14:00	21:00
89	10	Jueves	14:00	21:00
90	10	Viernes	14:00	21:00
91	11	Lunes	07:00	14:00
92	11	Martes	07:00	14:00
93	11	Miercoles	07:00	14:00
94	11	Jueves	07:00	14:00
95	11	Viernes	07:00	14:00
96	12	Lunes	07:00	14:00
97	12	Martes	07:00	14:00
98	12	Miercoles	07:00	14:00
99	12	Jueves	07:00	14:00
100	12	Viernes	07:00	14:00
101	12	Lunes	14:00	21:00
102	12	Martes	14:00	21:00
103	12	Miercoles	14:00	21:00
104	12	Jueves	14:00	21:00
105	12	Viernes	14:00	21:00
106	13	Lunes	07:00	14:00
107	13	Martes	07:00	14:00
108	13	Miercoles	07:00	14:00
109	13	Jueves	07:00	14:00
110	13	Viernes	07:00	14:00
111	13	Lunes	14:00	21:00
112	13	Martes	14:00	21:00
113	13	Miercoles	14:00	21:00
114	13	Jueves	14:00	21:00
115	13	Viernes	14:00	21:00
116	14	Lunes	07:00	14:00
117	14	Martes	07:00	14:00
118	14	Miercoles	07:00	14:00
119	14	Jueves	07:00	14:00
120	14	Viernes	07:00	14:00
121	15	Lunes	07:00	14:00
122	15	Martes	07:00	14:00
123	15	Miercoles	07:00	14:00
124	15	Jueves	07:00	14:00
125	15	Viernes	07:00	14:00
126	15	Lunes	14:00	21:00
127	15	Martes	14:00	21:00
128	15	Miercoles	14:00	21:00
129	15	Jueves	14:00	21:00
130	15	Viernes	14:00	21:00
131	16	Lunes	14:00	21:00
132	16	Martes	14:00	21:00
133	16	Miercoles	14:00	21:00
134	16	Jueves	14:00	21:00
135	16	Viernes	14:00	21:00
136	17	Lunes	07:00	14:00
137	17	Martes	07:00	14:00
138	17	Miercoles	07:00	14:00
139	17	Jueves	07:00	14:00
140	17	Viernes	07:00	14:00
141	17	Lunes	14:00	21:00
142	17	Martes	14:00	21:00
143	17	Miercoles	14:00	21:00
144	17	Jueves	14:00	21:00
145	17	Viernes	14:00	21:00
146	18	Lunes	07:00	14:00
147	18	Martes	07:00	14:00
148	18	Miercoles	07:00	14:00
149	19	Lunes	14:00	21:00
150	19	Martes	14:00	21:00
151	19	Miercoles	14:00	21:00
152	20	Jueves	07:00	14:00
153	20	Viernes	07:00	14:00
154	21	Lunes	07:00	14:00
155	21	Martes	07:00	14:00
156	21	Lunes	14:00	21:00
157	21	Martes	14:00	21:00
158	22	Miercoles	07:00	14:00
159	22	Jueves	07:00	14:00
160	22	Viernes	07:00	14:00
\.


--
-- Data for Name: profesor_materia; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.profesor_materia (id, profesor_id, materia_id, especialidad_id, fecha_asignacion) FROM stdin;
1	1	1	\N	2026-04-26
2	1	2	\N	2026-04-26
3	1	5	\N	2026-04-26
4	1	6	\N	2026-04-26
5	2	9	\N	2026-04-26
6	2	10	\N	2026-04-26
7	2	13	\N	2026-04-26
8	2	14	\N	2026-04-26
9	3	3	\N	2026-04-26
10	3	8	\N	2026-04-26
11	3	12	\N	2026-04-26
12	3	58	\N	2026-04-26
13	3	59	\N	2026-04-26
14	3	61	\N	2026-04-26
15	3	62	\N	2026-04-26
16	4	7	\N	2026-04-26
17	4	11	\N	2026-04-26
18	4	4	\N	2026-04-26
19	4	49	\N	2026-04-26
20	4	50	\N	2026-04-26
21	5	52	\N	2026-04-26
22	5	53	\N	2026-04-26
23	5	54	\N	2026-04-26
24	5	55	\N	2026-04-26
25	5	56	\N	2026-04-26
26	5	57	\N	2026-04-26
27	6	14	\N	2026-04-26
28	6	17	\N	2026-04-26
29	6	18	\N	2026-04-26
30	6	16	\N	2026-04-26
31	7	19	\N	2026-04-26
32	7	20	\N	2026-04-26
33	7	21	\N	2026-04-26
34	8	67	\N	2026-04-26
35	8	68	\N	2026-04-26
36	8	69	\N	2026-04-26
37	8	70	\N	2026-04-26
38	8	71	\N	2026-04-26
39	8	72	\N	2026-04-26
40	8	73	\N	2026-04-26
41	8	74	\N	2026-04-26
42	8	75	\N	2026-04-26
43	9	40	\N	2026-04-26
44	9	41	\N	2026-04-26
45	9	42	\N	2026-04-26
46	9	43	\N	2026-04-26
47	9	44	\N	2026-04-26
48	9	45	\N	2026-04-26
49	9	46	\N	2026-04-26
50	9	47	\N	2026-04-26
51	9	48	\N	2026-04-26
52	10	31	\N	2026-04-26
53	10	32	\N	2026-04-26
54	10	33	\N	2026-04-26
55	10	34	\N	2026-04-26
56	10	35	\N	2026-04-26
57	10	36	\N	2026-04-26
58	10	37	\N	2026-04-26
59	10	38	\N	2026-04-26
60	10	39	\N	2026-04-26
61	11	25	\N	2026-04-26
62	11	26	\N	2026-04-26
63	11	27	\N	2026-04-26
64	11	28	\N	2026-04-26
65	11	29	\N	2026-04-26
66	11	30	\N	2026-04-26
67	12	63	\N	2026-04-26
68	12	64	\N	2026-04-26
69	12	65	\N	2026-04-26
70	12	66	\N	2026-04-26
71	13	15	\N	2026-04-26
72	13	16	\N	2026-04-26
73	13	17	\N	2026-04-26
74	13	18	\N	2026-04-26
75	14	22	\N	2026-04-26
76	14	23	\N	2026-04-26
77	14	24	\N	2026-04-26
78	15	58	\N	2026-04-26
79	15	59	\N	2026-04-26
80	15	60	\N	2026-04-26
81	16	49	\N	2026-04-26
82	16	50	\N	2026-04-26
83	16	51	\N	2026-04-26
84	17	9	\N	2026-04-26
85	17	11	\N	2026-04-26
86	17	12	\N	2026-04-26
87	18	19	\N	2026-04-26
88	18	20	\N	2026-04-26
89	18	21	\N	2026-04-26
90	19	67	\N	2026-04-26
91	19	68	\N	2026-04-26
92	19	69	\N	2026-04-26
93	20	31	\N	2026-04-26
94	20	32	\N	2026-04-26
95	20	33	\N	2026-04-26
96	21	58	\N	2026-04-26
97	21	59	\N	2026-04-26
98	21	60	\N	2026-04-26
99	22	40	\N	2026-04-26
100	22	41	\N	2026-04-26
101	22	42	\N	2026-04-26
\.


--
-- Data for Name: semestre; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.semestre (id_semestre, id_plan, numero, descripcion) FROM stdin;
1	1	1	Primer Semestre
2	1	2	Segundo Semestre
3	1	3	Tercer Semestre
4	1	4	Cuarto Semestre
5	1	5	Quinto Semestre
6	1	6	Sexto Semestre
\.


--
-- Data for Name: usuarios; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.usuarios (id, username, password, email, fecha_registro) FROM stdin;
1	Director1	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	direct@gmail.com	2026-04-07 06:17:02.087944
2	admin1	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	admin1@gmail.com	2026-04-07 06:38:39.993998
3	admin2	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	admin2@gmail.com	2026-04-07 08:43:24.749879
4	Mendez	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	mendez@gmail.com	2026-04-08 18:51:42.735745
5	admin3	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	adm3@gmail.com	2026-04-09 08:58:45.265908
6	admin4	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	ad4@gmail.com	2026-04-09 10:17:04.76333
7	admin5	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	admin5@edu.mx	2026-04-09 10:25:00.571348
8	admin6	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	admin6@edu.mx	2026-04-09 10:32:42.267211
9	admin7	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	admin7@edu.mx	2026-04-09 10:40:32.766751
10	admin8	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	admin8@edu.mx	2026-04-09 10:47:20.175519
11	ad2uy	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	wjr@hf.mx	2026-04-09 10:50:38.435056
12	Dani	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	dani7@edu.mx	2026-04-09 10:54:54.270746
13	foxS	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	fox@gmail.mx	2026-04-09 12:26:55.292975
14	brian	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	brian@gmail.com	2026-04-09 12:51:52.753079
15	admin9	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	admin9@gmail.mx	2026-04-10 17:18:28.180635
16	angel	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	ang@edu.mx	2026-04-10 18:44:42.577324
17	aleje	jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=	ale@edu.mx	2026-04-10 21:09:28.083032
18	manuel	jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=	manu@edu.mx	2026-04-10 21:13:54.213267
19	daniel	jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=	dani@edu.mx	2026-04-10 21:17:04.921335
20	Axel	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	ax@edu.mx	2026-04-10 21:22:53.508186
21	Rmirez	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	ram@edu.mx	2026-04-10 21:31:57.186911
22	Alex	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	aa@edu.mx	2026-04-11 18:54:37.364184
23	mauri	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	mau@edu.mx	2026-04-11 21:23:52.318545
24	Angel	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	angel@edu.mx	2026-04-12 12:09:56.14858
25	Enrique	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	enrique@edu.mx	2026-04-12 12:18:37.220795
26	Mauricio	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	mau.her@edu.mx	2026-04-14 16:39:23.840433
27	Ricardo	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	ricardo.56@edu.mx	2026-04-14 21:58:51.854742
28		47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=		2026-04-15 11:18:57.013673
29	Manuel	eI/oqJHiFTOr0aOIy87vffW5BhsRk+FX/9RkUeY4iNA=	manuelaqedu.mx	2026-04-16 13:25:18.469628
\.


--
-- Name: aula_id_aula_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.aula_id_aula_seq', 4, true);


--
-- Name: bitacora_id_bitacora_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.bitacora_id_bitacora_seq', 1, false);


--
-- Name: ciclo_id_ciclo_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ciclo_id_ciclo_seq', 1, true);


--
-- Name: clave_sistema_id_clave_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.clave_sistema_id_clave_seq', 30, true);


--
-- Name: detalle_horario_id_detalle_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.detalle_horario_id_detalle_seq', 1, false);


--
-- Name: especialidades_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.especialidades_id_seq1', 7, true);


--
-- Name: grupos_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.grupos_id_seq', 237, true);


--
-- Name: horario_general_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.horario_general_id_seq', 14, true);


--
-- Name: horario_grupos_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.horario_grupos_id_seq', 544, true);


--
-- Name: horario_id_horario_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.horario_id_horario_seq', 1, true);


--
-- Name: horario_profesores_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.horario_profesores_id_seq', 544, true);


--
-- Name: materias_id_materia_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.materias_id_materia_seq', 75, true);


--
-- Name: plan_estudio_id_plan_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.plan_estudio_id_plan_seq', 5, true);


--
-- Name: plan_estudio_materia_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.plan_estudio_materia_id_seq', 1, false);


--
-- Name: profesor_disponibilidad_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.profesor_disponibilidad_id_seq', 160, true);


--
-- Name: profesor_materia_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.profesor_materia_id_seq', 101, true);


--
-- Name: profesores_id_profesor_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.profesores_id_profesor_seq', 22, true);


--
-- Name: semestre_id_semestre_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.semestre_id_semestre_seq', 5, true);


--
-- Name: usuarios_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.usuarios_id_seq', 29, true);


--
-- Name: aula aula_numero_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.aula
    ADD CONSTRAINT aula_numero_key UNIQUE (numero);


--
-- Name: aula aula_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.aula
    ADD CONSTRAINT aula_pkey PRIMARY KEY (id_aula);


--
-- Name: bitacora bitacora_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bitacora
    ADD CONSTRAINT bitacora_pkey PRIMARY KEY (id_bitacora);


--
-- Name: ciclo ciclo_nombre_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ciclo
    ADD CONSTRAINT ciclo_nombre_key UNIQUE (nombre);


--
-- Name: ciclo ciclo_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ciclo
    ADD CONSTRAINT ciclo_pkey PRIMARY KEY (id_ciclo);


--
-- Name: clave_sistema clave_sistema_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clave_sistema
    ADD CONSTRAINT clave_sistema_pkey PRIMARY KEY (id_clave);


--
-- Name: detalle_horario detalle_horario_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.detalle_horario
    ADD CONSTRAINT detalle_horario_pkey PRIMARY KEY (id_detalle);


--
-- Name: especialidades especialidades_pkey1; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.especialidades
    ADD CONSTRAINT especialidades_pkey1 PRIMARY KEY (id);


--
-- Name: grupos grupos_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.grupos
    ADD CONSTRAINT grupos_pkey PRIMARY KEY (id);


--
-- Name: horario_general horario_general_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.horario_general
    ADD CONSTRAINT horario_general_pkey PRIMARY KEY (id);


--
-- Name: horario_grupos horario_grupos_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.horario_grupos
    ADD CONSTRAINT horario_grupos_pkey PRIMARY KEY (id);


--
-- Name: horario horario_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.horario
    ADD CONSTRAINT horario_pkey PRIMARY KEY (id_horario);


--
-- Name: horario_profesores horario_profesores_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.horario_profesores
    ADD CONSTRAINT horario_profesores_pkey PRIMARY KEY (id);


--
-- Name: materia_grupos materia_grupos_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.materia_grupos
    ADD CONSTRAINT materia_grupos_pkey PRIMARY KEY (materia_id, grupo_id);


--
-- Name: materias materias_clave_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.materias
    ADD CONSTRAINT materias_clave_key UNIQUE (clave);


--
-- Name: materias materias_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.materias
    ADD CONSTRAINT materias_pkey PRIMARY KEY (id_materia);


--
-- Name: plan_estudio_materia plan_estudio_materia_id_plan_id_materia_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plan_estudio_materia
    ADD CONSTRAINT plan_estudio_materia_id_plan_id_materia_key UNIQUE (id_plan, id_materia);


--
-- Name: plan_estudio_materia plan_estudio_materia_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plan_estudio_materia
    ADD CONSTRAINT plan_estudio_materia_pkey PRIMARY KEY (id);


--
-- Name: plan_estudio plan_estudio_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plan_estudio
    ADD CONSTRAINT plan_estudio_pkey PRIMARY KEY (id_plan);


--
-- Name: profesor_disponibilidad profesor_disponibilidad_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profesor_disponibilidad
    ADD CONSTRAINT profesor_disponibilidad_pkey PRIMARY KEY (id);


--
-- Name: profesor_materia profesor_materia_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profesor_materia
    ADD CONSTRAINT profesor_materia_pkey PRIMARY KEY (id);


--
-- Name: profesor_materia profesor_materia_profesor_id_materia_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profesor_materia
    ADD CONSTRAINT profesor_materia_profesor_id_materia_id_key UNIQUE (profesor_id, materia_id);


--
-- Name: profesor profesores_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profesor
    ADD CONSTRAINT profesores_email_key UNIQUE (email);


--
-- Name: profesor profesores_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profesor
    ADD CONSTRAINT profesores_pkey PRIMARY KEY (id_profesor);


--
-- Name: semestre semestre_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.semestre
    ADD CONSTRAINT semestre_pkey PRIMARY KEY (id_semestre);


--
-- Name: detalle_horario uq_dh_aula_hora; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.detalle_horario
    ADD CONSTRAINT uq_dh_aula_hora UNIQUE (id_aula, dia_semana, hora_inicio);


--
-- Name: detalle_horario uq_dh_prof_hora; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.detalle_horario
    ADD CONSTRAINT uq_dh_prof_hora UNIQUE (id_profesor, dia_semana, hora_inicio);


--
-- Name: horario uq_hor; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.horario
    ADD CONSTRAINT uq_hor UNIQUE (id_ciclo, id_grupo, tipo);


--
-- Name: semestre uq_sem_plan_num; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.semestre
    ADD CONSTRAINT uq_sem_plan_num UNIQUE (id_plan, numero);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: usuarios usuarios_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_username_key UNIQUE (username);


--
-- Name: idx_bit_usr_fecha; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bit_usr_fecha ON public.bitacora USING btree (id_usuario, fecha);


--
-- Name: idx_dh_dia_hora; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_dh_dia_hora ON public.detalle_horario USING btree (dia_semana, hora_inicio);


--
-- Name: idx_grupos_capacidad; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_grupos_capacidad ON public.grupos USING btree (capacidad);


--
-- Name: idx_grupos_codigo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_grupos_codigo ON public.grupos USING btree (codigo);


--
-- Name: idx_mat_esp_sem; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_mat_esp_sem ON public.materias USING btree (id_especialidad, id_semestre);


--
-- Name: idx_plan_materia; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_plan_materia ON public.plan_estudio_materia USING btree (id_plan, id_materia);


--
-- Name: idx_prof_prior; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_prof_prior ON public.profesor USING btree (prioridad, fecha_ingreso);


--
-- Name: idx_profesor_disponibilidad; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_profesor_disponibilidad ON public.profesor_disponibilidad USING btree (profesor_id, dia);


-- FIX: fk_detalle_horario_aula eliminada (duplicado de fk_dh_aula que ya tiene RESTRICT).


-- FIX: fk_detalle_horario_horario eliminada (duplicado de fk_dh_hor que ya tiene CASCADE).


-- FIX: fk_detalle_horario_materia eliminada (duplicado de fk_dh_mat con comportamiento contradictorio CASCADE vs RESTRICT)
-- Se conserva fk_dh_mat (ON DELETE RESTRICT).


-- FIX: fk_detalle_horario_profesor eliminada (duplicado de fk_dh_prof con comportamiento contradictorio CASCADE vs RESTRICT)
-- Se conserva fk_dh_prof (ON DELETE RESTRICT) para bloquear borrado de profesor con clases asignadas.


--
-- Name: detalle_horario fk_dh_aula; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.detalle_horario
    ADD CONSTRAINT fk_dh_aula FOREIGN KEY (id_aula) REFERENCES public.aula(id_aula) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: detalle_horario fk_dh_hor; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.detalle_horario
    ADD CONSTRAINT fk_dh_hor FOREIGN KEY (id_horario) REFERENCES public.horario(id_horario) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: detalle_horario fk_dh_mat; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.detalle_horario
    ADD CONSTRAINT fk_dh_mat FOREIGN KEY (id_materia) REFERENCES public.materias(id_materia) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: detalle_horario fk_dh_prof; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.detalle_horario
    ADD CONSTRAINT fk_dh_prof FOREIGN KEY (id_profesor) REFERENCES public.profesor(id_profesor) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: horario fk_hor_cic; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.horario
    ADD CONSTRAINT fk_hor_cic FOREIGN KEY (id_ciclo) REFERENCES public.ciclo(id_ciclo) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: materias fk_mat_sem; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.materias
    ADD CONSTRAINT fk_mat_sem FOREIGN KEY (id_semestre) REFERENCES public.semestre(id_semestre) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: semestre fk_sem_plan; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.semestre
    ADD CONSTRAINT fk_sem_plan FOREIGN KEY (id_plan) REFERENCES public.plan_estudio(id_plan) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: materia_grupos materia_grupos_grupo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.materia_grupos
    ADD CONSTRAINT materia_grupos_grupo_id_fkey FOREIGN KEY (grupo_id) REFERENCES public.grupos(id) ON DELETE CASCADE;


--
-- Name: plan_estudio_materia plan_estudio_materia_id_materia_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plan_estudio_materia
    ADD CONSTRAINT plan_estudio_materia_id_materia_fkey FOREIGN KEY (id_materia) REFERENCES public.materias(id_materia) ON DELETE CASCADE;


--
-- Name: plan_estudio_materia plan_estudio_materia_id_plan_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plan_estudio_materia
    ADD CONSTRAINT plan_estudio_materia_id_plan_fkey FOREIGN KEY (id_plan) REFERENCES public.plan_estudio(id_plan) ON DELETE CASCADE;


--
-- Name: profesor_materia profesor_materia_especialidad_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profesor_materia
    ADD CONSTRAINT profesor_materia_especialidad_id_fkey FOREIGN KEY (especialidad_id) REFERENCES public.especialidades(id);


--
-- Name: profesor_materia profesor_materia_materia_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profesor_materia
    ADD CONSTRAINT profesor_materia_materia_id_fkey FOREIGN KEY (materia_id) REFERENCES public.materias(id_materia) ON DELETE CASCADE;


--
-- Name: profesor_materia profesor_materia_profesor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profesor_materia
    ADD CONSTRAINT profesor_materia_profesor_id_fkey FOREIGN KEY (profesor_id) REFERENCES public.profesor(id_profesor) ON DELETE CASCADE;



--
-- FIX: Llaves foráneas faltantes y trigger de limpieza
--

-- FIX: FK faltante en profesor_disponibilidad -> profesor (CASCADE)
ALTER TABLE ONLY public.profesor_disponibilidad
    ADD CONSTRAINT fk_disp_profesor
    FOREIGN KEY (profesor_id)
    REFERENCES public.profesor(id_profesor)
    ON UPDATE CASCADE
    ON DELETE CASCADE;

-- FIX: FK faltante en clave_sistema -> usuarios (CASCADE)
ALTER TABLE ONLY public.clave_sistema
    ADD CONSTRAINT fk_clave_director
    FOREIGN KEY (id_director)
    REFERENCES public.usuarios(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE;

-- FIX: FK faltante en bitacora -> usuarios (SET NULL - conserva historial)
ALTER TABLE ONLY public.bitacora
    ADD CONSTRAINT fk_bitacora_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES public.usuarios(id)
    ON UPDATE CASCADE
    ON DELETE SET NULL;

-- FIX: Trigger para limpiar horario_profesores y horario_grupos al borrar un profesor.
-- Estas tablas guardan nombre/RFC como texto plano y no pueden tener FK normal.
CREATE OR REPLACE FUNCTION public.limpiar_horario_al_borrar_profesor()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM public.horario_profesores
    WHERE rfc_profesor = OLD.rfc;

    DELETE FROM public.horario_grupos
    WHERE profesor = OLD.nombre || ' ' || OLD.apellidos;

    RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS trg_limpiar_horario_profesor ON public.profesor;

CREATE TRIGGER trg_limpiar_horario_profesor
    AFTER DELETE ON public.profesor
    FOR EACH ROW
    EXECUTE FUNCTION public.limpiar_horario_al_borrar_profesor();

--
-- PostgreSQL database dump complete
--


-- ================================================================
-- FIX #1/#2: Tabla para guardar metadatos del horario generado.
-- Sustituye la fila 'META' que antes se insertaba en horario_general
-- y aparecía visible en la vista del horario.
-- ================================================================
CREATE TABLE IF NOT EXISTS public.horario_meta (
    id              integer PRIMARY KEY DEFAULT 1,  -- siempre 1 fila (upsert)
    usuario_id      integer NOT NULL,
    fecha_generacion timestamp NOT NULL DEFAULT NOW(),
    CONSTRAINT horario_meta_single_row CHECK (id = 1)
);

ALTER TABLE public.horario_meta OWNER TO postgres;


-- ============================================================
--  PARCHE SQL para HorarioDAO v7
--  Ejecutar UNA SOLA VEZ después de aplicar el nuevo HorarioDAO.java
-- ============================================================

-- 1. Unique en horario_general.hora (necesario para ON CONFLICT (hora) DO UPDATE)
--    Si ya existe, la sentencia no falla gracias al IF NOT EXISTS.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'horario_general_hora_unique'
          AND conrelid = 'public.horario_general'::regclass
    ) THEN
        ALTER TABLE public.horario_general
            ADD CONSTRAINT horario_general_hora_unique UNIQUE (hora);
    END IF;
END;
$$;

-- 2. Índices de rendimiento para las consultas de validación y vista
--    (creados solo si no existen)
CREATE INDEX IF NOT EXISTS idx_hg_grupo_dia_hora
    ON public.horario_grupos (grupo, dia, hora);

CREATE INDEX IF NOT EXISTS idx_hg_grupo
    ON public.horario_grupos (grupo);

CREATE INDEX IF NOT EXISTS idx_hp_rfc
    ON public.horario_profesores (rfc_profesor);

CREATE INDEX IF NOT EXISTS idx_hp_prof_dia_hora
    ON public.horario_profesores (nombre_profesor, dia, hora);

CREATE INDEX IF NOT EXISTS idx_mat_esp_sem
    ON public.materias (id_especialidad, id_semestre);

CREATE INDEX IF NOT EXISTS idx_disp_prof
    ON public.profesor_disponibilidad (profesor_id);

-- 3. Tabla horario_meta (si aún no existe — la versión anterior la creaba con ON CONFLICT)
CREATE TABLE IF NOT EXISTS public.horario_meta (
    id              serial PRIMARY KEY,
    usuario_id      integer NOT NULL,
    fecha_generacion timestamp NOT NULL DEFAULT NOW()
);

-- ============================================================
--  Verificación rápida: cuántas filas tienen las tablas clave
-- ============================================================
SELECT 'grupos'                  AS tabla, COUNT(*) FROM public.grupos
UNION ALL
SELECT 'materias',                          COUNT(*) FROM public.materias
UNION ALL
SELECT 'profesor',                          COUNT(*) FROM public.profesor
UNION ALL
SELECT 'profesor_disponibilidad',           COUNT(*) FROM public.profesor_disponibilidad
UNION ALL
SELECT 'profesor_materia',                  COUNT(*) FROM public.profesor_materia
UNION ALL
SELECT 'horario_grupos (actual)',            COUNT(*) FROM public.horario_grupos
UNION ALL
SELECT 'horario_profesores (actual)',        COUNT(*) FROM public.horario_profesores;


-- ============================================================
-- Script para insertar la clave maestra del director
-- Ejecutar UNA SOLA VEZ al configurar el sistema
-- ============================================================

-- Primero limpia cualquier clave anterior (solo debe existir una)
DELETE FROM clave_sistema;

-- Inserta la clave del director (cámbia 'CLAVE_SECRETA_AQUI' por la clave real)
-- El hash se genera con SHA-256 + Base64, igual que en Java.
-- Para generarlo desde PostgreSQL puedes usar la extensión pgcrypto:
--   SELECT encode(digest('tu_clave_aqui', 'sha256'), 'base64');

INSERT INTO clave_sistema (clave_hash, activa, id_director)
VALUES (
    encode(digest('Direct77', 'sha256'), 'base64'),
    true,
    1  -- id del usuario director (ajusta según tu tabla usuarios)
);

-- Verifica que quedó bien:
SELECT id_clave, activa, fecha_cambio FROM clave_sistema;

-- 1. Agrega la columna necesaria
ALTER TABLE public.grupos ADD COLUMN id_tutor integer DEFAULT NULL;

-- 2. Crea la relación con la tabla de profesores
ALTER TABLE public.grupos ADD CONSTRAINT fk_grupos_tutor 
FOREIGN KEY (id_tutor) REFERENCES public.profesor(id_profesor) ON DELETE SET NULL;

-- ============================================================
--  PARCHE: correccion de funciones con usuario_id hardcodeado
--  Aplica sobre la BD existente sin tocar los datos.
--  Ejecutar en psql: \i parche_db.sql
--                 o: psql -U postgres -d horarios_db -f parche_db.sql
-- ============================================================

-- 1. Reemplazar horario_grupo_tabla
--    Antes tenia WHERE g.usuario_id = 5 fijo.
--    Ahora acepta p_usuario_id opcional (NULL = sin filtro).
CREATE OR REPLACE FUNCTION public.horario_grupo_tabla(
    codigo_grupo    text,
    p_usuario_id    integer DEFAULT NULL
)
RETURNS TABLE(
    "HORA"      text,
    "LUNES"     text,
    "MARTES"    text,
    "MIERCOLES" text,
    "JUEVES"    text,
    "VIERNES"   text
)
LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY
    SELECT
        TO_CHAR(h.hora_inicio, 'HH24:MI')::TEXT AS "HORA",
        MAX(CASE WHEN h.dia_semana = 'Lunes'     THEN m.codigo || ' (' || INITCAP(LEFT(p.nombre,1)) || '. ' || p.apellido || ')' END)::TEXT AS "LUNES",
        MAX(CASE WHEN h.dia_semana = 'Martes'    THEN m.codigo || ' (' || INITCAP(LEFT(p.nombre,1)) || '. ' || p.apellido || ')' END)::TEXT AS "MARTES",
        MAX(CASE WHEN h.dia_semana = 'Miercoles' THEN m.codigo || ' (' || INITCAP(LEFT(p.nombre,1)) || '. ' || p.apellido || ')' END)::TEXT AS "MIERCOLES",
        MAX(CASE WHEN h.dia_semana = 'Jueves'    THEN m.codigo || ' (' || INITCAP(LEFT(p.nombre,1)) || '. ' || p.apellido || ')' END)::TEXT AS "JUEVES",
        MAX(CASE WHEN h.dia_semana = 'Viernes'   THEN m.codigo || ' (' || INITCAP(LEFT(p.nombre,1)) || '. ' || p.apellido || ')' END)::TEXT AS "VIERNES"
    FROM horario_generado h
    JOIN materias   m ON h.materia_id  = m.id
    JOIN profesores p ON h.profesor_id = p.id
    JOIN grupos     g ON h.grupo_id    = g.id
    WHERE g.codigo = codigo_grupo
      AND (p_usuario_id IS NULL OR g.usuario_id = p_usuario_id)
    GROUP BY h.hora_inicio
    ORDER BY h.hora_inicio;
END;
$$;

-- 2. Reemplazar horario_por_grupo
CREATE OR REPLACE FUNCTION public.horario_por_grupo(
    codigo_grupo    text,
    p_usuario_id    integer DEFAULT NULL
)
RETURNS TABLE(
    grupo       text,
    hora        text,
    dia_semana  text,
    materia     text,
    profesor    text
)
LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY
    SELECT
        g.codigo::TEXT AS grupo,
        TO_CHAR(h.hora_inicio, 'HH24:MI') AS hora,
        h.dia_semana::TEXT,
        m.codigo::TEXT AS materia,
        INITCAP(LEFT(p.nombre,1)) || '. ' || p.apellido AS profesor
    FROM horario_generado h
    JOIN grupos     g ON h.grupo_id    = g.id
    JOIN materias   m ON h.materia_id  = m.id
    JOIN profesores p ON h.profesor_id = p.id
    WHERE g.codigo = codigo_grupo
      AND (p_usuario_id IS NULL OR g.usuario_id = p_usuario_id)
    ORDER BY
        CASE h.dia_semana
            WHEN 'Lunes'     THEN 1
            WHEN 'Martes'    THEN 2
            WHEN 'Miercoles' THEN 3
            WHEN 'Jueves'    THEN 4
            WHEN 'Viernes'   THEN 5
        END,
        h.hora_inicio;
END;
$$;

-- Verificacion rapida
DO $$
BEGIN
    RAISE NOTICE 'Parche aplicado correctamente.';
    RAISE NOTICE 'Funciones actualizadas: horario_grupo_tabla, horario_por_grupo';
END;
$$;


-- ============================================================
--  PARCHE: Agregar columna grupo_id a profesor_materia
--  Permite registrar a que grupo especifico imparte cada materia un profesor.
--  La columna es nullable: NULL = sin grupo especifico asignado.
-- ============================================================
ALTER TABLE public.profesor_materia
    ADD COLUMN IF NOT EXISTS grupo_id integer;

ALTER TABLE public.profesor_materia
    ADD CONSTRAINT IF NOT EXISTS profesor_materia_grupo_id_fkey
    FOREIGN KEY (grupo_id) REFERENCES public.grupos(id) ON DELETE SET NULL;

DO $$
BEGIN
    RAISE NOTICE 'Columna grupo_id agregada a profesor_materia correctamente.';
END;
$$;
