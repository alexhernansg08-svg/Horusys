/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  CLASE: DatabaseConnection
 *  CAPA:  DAO (Data Access Object)
 * ============================================================
 *  Centraliza TODA la logica de conexion a PostgreSQL.
 *  Ningun otro archivo deberia abrir una conexion directamente;
 *  todos llaman a DatabaseConnection.getConnection().
 *
 *  Patron de diseno: Utility Class (clase de utilidades estaticas).
 *    - Constructor privado: impide crear instancias con "new".
 *    - Metodo estatico getConnection(): unico punto de acceso.
 *    - Bloque static{}: carga la configuracion una sola vez al arrancar.
 * ============================================================
 */
public final class DatabaseConnection {

    private static final Logger LOG = Logger.getLogger(DatabaseConnection.class.getName());

    // ---------------------------------------------------------------
    // Constantes de conexion (cargadas desde db.properties al inicio)
    // ---------------------------------------------------------------
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    /**
     * Bloque estatico: se ejecuta UNA SOLA VEZ cuando la JVM
     * carga esta clase por primera vez. Lee db.properties del
     * classpath y extrae las tres propiedades necesarias.
     * Si el archivo no existe, usa valores por defecto razonables.
     */
    static {
        Properties props = cargarPropiedades(); // lee src/db.properties
        URL      = props.getProperty("db.url",      "jdbc:postgresql://127.0.0.1/horarios_db2");
        USER     = props.getProperty("db.user",     "postgres");
        PASSWORD = props.getProperty("db.password", "unach2024");
    }

    /**
     * Constructor privado: nadie puede hacer "new DatabaseConnection()".
     * Asi nos aseguramos de que la clase solo se use de forma estatica.
     */
    private DatabaseConnection() {}

    // ---------------------------------------------------------------
    //  Metodo privado: carga db.properties desde el classpath
    // ---------------------------------------------------------------

    /**
     * Busca db.properties en el classpath (carpeta src/) y carga
     * sus pares clave=valor en un objeto Properties.
     *
     * El archivo tipico tiene:
     *   db.url=jdbc:postgresql://127.0.0.1/horarios_db
     *   db.user=postgres
     *   db.password=mi_password
     *
     * @return Properties con los valores leidos (o vacio si no se encontro)
     */
    private static Properties cargarPropiedades() {
        Properties props = new Properties();
        // getClassLoader().getResourceAsStream() busca el archivo
        // dentro del JAR o del classpath compilado (no en el disco duro directamente)
        try (InputStream is = DatabaseConnection.class
                .getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is); // parsea el archivo clave=valor
            } else {
                // El archivo no existe en el classpath -> advertencia, se usaran defaults
                LOG.warning("db.properties no encontrado en classpath. "
                    + "Crea src/db.properties con las credenciales.");
            }
        } catch (IOException e) {
            // Error de lectura del archivo -> log de error critico
            LOG.severe("Error al leer db.properties: " + e.getMessage());
        }
        return props;
    }

    // ---------------------------------------------------------------
    //  Metodo publico: obtiene una conexion activa a la BD
    // ---------------------------------------------------------------

    /**
     * Abre y retorna una conexion JDBC a PostgreSQL.
     *
     * IMPORTANTE: el llamador es responsable de cerrar la conexion
     * (se recomienda usar try-with-resources para garantizarlo).
     *
     * Flujo interno:
     *   1. Registra el driver JDBC de PostgreSQL en la JVM.
     *   2. Llama a DriverManager con URL, USER y PASSWORD.
     *   3. Si algo falla, lanza SQLException con mensaje claro.
     *
     * @return Connection lista para ejecutar PreparedStatements
     * @throws SQLException si el driver no existe o la BD rechaza la conexion
     */
  public static Connection getConnection() throws SQLException {
    try {
        Class.forName("org.postgresql.Driver");

        // Quemamos las credenciales directamente aquí para probar:
        return DriverManager.getConnection("jdbc:postgresql://127.0.0.1/horarios_db2", "postgres", "unach2024");

    } catch (ClassNotFoundException e) {
        throw new SQLException("Driver PostgreSQL no encontrado. Verifica el JAR.", e);
    } catch (SQLException e) {
        LOG.severe("ERROR DE CONEXION - URL: " + URL + " | Usuario: " + USER);
        throw e;
    }
}
}
