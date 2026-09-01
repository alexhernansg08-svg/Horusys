# Sistema de Horarios — CBTIS 22

Aplicación de escritorio en **Java Swing** para la generación y gestión de horarios escolares.

---

## Arquitectura MVC + DAO

El proyecto sigue el patrón **Modelo – Vista – Controlador** con una capa separada de **DAO** para el acceso a datos.

```
┌─────────────────────────────────────────────────────────────┐
│                         VISTA                               │
│   LoginWindow  RegisterWindow  MainWindow                   │
│   ProfesoresWindow  MateriasWindow  GruposWindow            │
│   EspecialidadesWindow  HorariosWindow                      │
└────────────────────┬────────────────────────────────────────┘
                     │ llama a
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      CONTROLADOR                            │
│   LoginController     ProfesorController                    │
│   MateriaController   GrupoController                       │
│   EspecialidadController  HorarioController                 │
└────────────────────┬────────────────────────────────────────┘
                     │ llama a
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                         DAO                                 │
│   UserDAO         ProfesorDAO     MateriaDAO                │
│   GrupoDAO        EspecialidadDAO HorarioDAO                │
│   DatabaseConnection  (gestiona la conexión JDBC)           │
└────────────────────┬────────────────────────────────────────┘
                     │ lee/escribe
                     ▼
            ┌────────────────┐
            │  PostgreSQL DB  │
            │  horarios_db   │
            └────────────────┘
```

> **Regla de oro:** cada capa solo habla con la de abajo.
> La Vista nunca toca el DAO. El DAO nunca toca la Vista.

---

## Estructura de paquetes

```
src/
└── horarios/
    ├── Main.java                   ← Punto de entrada (lanza LoginWindow)
    │
    ├── modelo/                     ← POJOs (datos, sin lógica de negocio)
    │   ├── User.java               ← Usuario administrador del sistema
    │   ├── Especialidad.java       ← Carrera técnica (ej: Informática)
    │   ├── Grupo.java              ← Grupo escolar (ej: 1A-INF)
    │   ├── Materia.java            ← Asignatura (ej: Algoritmos)
    │   └── Profesor.java           ← Docente + disponibilidad horaria
    │
    ├── vista/                      ← Ventanas Swing (JFrames)
    │   ├── LoginWindow             ← Autenticación
    │   ├── RegisterWindow          ← Alta de nuevo director
    │   ├── MainWindow              ← Menú principal
    │   ├── ProfesoresWindow        ← CRUD de profesores y disponibilidad
    │   ├── MateriasWindow          ← CRUD de materias
    │   ├── GruposWindow            ← CRUD de grupos
    │   ├── EspecialidadesWindow    ← CRUD de especialidades
    │   └── HorariosWindow          ← Generación y consulta de horarios
    │
    ├── controlador/                ← Lógica de validación y orquestación
    │   ├── LoginController         ← Valida credenciales y registro
    │   ├── ProfesorController      ← Valida y orquesta CRUD de profesores
    │   ├── MateriaController       ← Valida y orquesta CRUD de materias
    │   ├── GrupoController         ← Valida y orquesta CRUD de grupos
    │   ├── EspecialidadController  ← Valida y orquesta CRUD de especialidades
    │   └── HorarioController       ← Expone generación y consulta de horarios
    │
    ├── dao/                        ← Acceso a base de datos (SQL puro)
    │   ├── DatabaseConnection      ← Única fuente de conexiones JDBC
    │   ├── UserDAO                 ← Login, registro, hash SHA-256
    │   ├── ProfesorDAO             ← CRUD + disponibilidad + materias asignadas
    │   ├── MateriaDAO              ← CRUD de materias
    │   ├── GrupoDAO                ← CRUD de grupos
    │   ├── EspecialidadDAO         ← CRUD de especialidades
    │   └── HorarioDAO              ← Algoritmo greedy de generación + consultas
    │
    └── util/                       ← Herramientas reutilizables (sin estado)
        ├── Colores.java            ← Paleta de colores de la UI
        ├── SoundManager.java       ← Reproducción de sonidos de feedback
        ├── SwingUtils.java         ← Helpers de Swing (tablas, formato)
        └── ExcelExporter.java      ← Exportación de horarios a Excel
```

---

## Flujo principal del sistema

```
Inicio
  └─► LoginWindow
        ├─ Login exitoso ──► MainWindow
        │                       ├─► ProfesoresWindow
        │                       ├─► MateriasWindow
        │                       ├─► GruposWindow
        │                       ├─► EspecialidadesWindow
        │                       └─► HorariosWindow
        │                             ├─ Generar horario (algoritmo greedy)
        │                             ├─ Ver por grupo
        │                             ├─ Ver por profesor
        │                             └─ Exportar a Excel
        └─ Registro ──────► RegisterWindow
```

---

## Configuración de la base de datos

Edita `src/db.properties` con tus credenciales:

```properties
db.url=jdbc:postgresql://127.0.0.1/horarios_db
db.user=postgres
db.password=tu_password
```

El script SQL para crear las tablas está en:
```
horarios_db_script_sql_fixed.sql
```

---

## Dependencias

| Librería | Uso |
|---|---|
| `postgresql-xx.jar` | Driver JDBC para PostgreSQL |
| `AbsoluteLayout-RELEASE140.jar` | Layout de NetBeans para los formularios `.form` |

---

## Convenciones del código

| Capa | Qué hace | Qué NO hace |
|---|---|---|
| **Modelo** | Guarda datos (atributos + getters/setters) | Lógica de negocio ni SQL |
| **Vista** | Muestra UI, captura eventos | Llama directamente al DAO |
| **Controlador** | Valida datos, coordina Vista↔DAO | Ejecuta SQL ni dibuja UI |
| **DAO** | Ejecuta SQL, mapea ResultSet → Modelo | Valida reglas de negocio |
| **Util** | Herramientas reutilizables | Estado persistente |
