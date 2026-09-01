/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.dao;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Base64;
import java.util.logging.Logger;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  DAO: UserDAO
 *  CAPA: Data Access Object
 * ============================================================
 *  Maneja el acceso a la tabla "usuarios" y la tabla "clave_sistema".
 *
 *  Seguridad implementada:
 *    - Las contrasenas NUNCA se guardan en texto plano.
 *    - La clave del director tambien se valida contra su hash en BD.
 *
 *  Flujo de registro:
 *    1. Verificar que la clave del sistema (director) sea valida en BD.
 *    2. Si es valida, insertar el nuevo usuario con password hasheado.
 *    3. Todo ocurre en una transaccion (commit/rollback) para atomicidad.
 *
 *  Tablas en BD:
 *    - usuarios       (username, password, email)
 *    - clave_sistema  (clave_hash, activa)
 * ============================================================
 */
public class UserDAO {

    private static final Logger LOG = Logger.getLogger(UserDAO.class.getName());

    // ---------------------------------------------------------------
    //  Constantes SQL (definidas aqui para no repetirlas en el codigo)
    // ---------------------------------------------------------------

    // Verifica si la clave del sistema coincide con alguna clave activa en BD
    private static final String SQL_CLAVE_VALIDA =
        "SELECT 1 FROM clave_sistema WHERE clave_hash = ? AND activa = true";

    // Inserta un nuevo usuario
    private static final String SQL_INSERT_USUARIO =
        "INSERT INTO usuarios (username, password, email) VALUES (?, ?, ?)";

    // Obtiene el password hasheado de un usuario por su username
    private static final String SQL_LOGIN =
        "SELECT password FROM usuarios WHERE username = ?";

    // Verifica si un username ya existe en la tabla
    private static final String SQL_EXISTS =
        "SELECT 1 FROM usuarios WHERE username = ?";

    // ---------------------------------------------------------------
    //  API publica
    // ---------------------------------------------------------------

    /**
     * Registra un nuevo usuario con la proteccion de la clave del director.
     *
     * para garantizar que ambas operaciones (verificar clave + insertar usuario)
     * sean atomicas: si falla cualquiera, no queda nada a medias.
     *
     * username     nombre de usuario
     * password     contrasena en texto plano (se hashea internamente)
     * email        correo electronico
     * claveSistema clave del director en texto plano (se hashea para comparar)
     * @return true si el registro fue exitoso
     */
    public boolean registrar(String username, String password, String email, String claveSistema) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // iniciar transaccion manual

            try {
                // Paso 1: verificar la clave del director contra su hash en BD
                if (!claveValida(conn, claveSistema)) {
                    conn.rollback(); // clave incorrecta: deshacer todo
                    return false;
                }

                // Paso 2: insertar el nuevo usuario con la contrasena hasheada
                try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_USUARIO)) {
                    ps.setString(1, username);
                    ps.setString(2, encriptar(password)); // nunca guardar texto plano
                    ps.setString(3, email);
                    ps.executeUpdate();
                }

                conn.commit(); // exito: confirmar la transaccion
                return true;

            } catch (SQLException ex) {
                conn.rollback(); // error: revertir todo (ej: username duplicado)
                LOG.severe("Error al registrar usuario: " + ex.getMessage());
                return false;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica las credenciales de un usuario para iniciar sesion.
     *
     * Flujo:
     *   1. Busca el usuario por username.
     *   2. Hashea la contrasena ingresada.
     *   3. Compara el hash con el almacenado en BD.
     *
     * username nombre de usuario
     * password contrasena en texto plano
     * @return true si las credenciales son correctas
     */
    public boolean login(String username, String password) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LOGIN)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                // rs.next() retorna false si el usuario no existe
                // Si existe, comparamos el hash de la contrasena ingresada
                // con el hash almacenado en BD
                return rs.next() && rs.getString("password").equals(encriptar(password));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si un nombre de usuario ya esta registrado.
     * Usado en el formulario de registro para alertar antes de enviar.
     *
     * En caso de error SQL, retorna true por seguridad
     * (asumir que existe es mas seguro que asumir que no existe).
     *
     * username nombre a verificar
     * @return true si el username ya existe en BD
     */
    public boolean existe(String username) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_EXISTS)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // si tiene fila -> username existe
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return true; // fallo seguro: asumir que existe
        }
    }

    // ---------------------------------------------------------------
    //  Helpers privados
    // ---------------------------------------------------------------

    /**
     * Verifica si la clave del sistema (director) es valida.
     * Hashea la clave recibida y la compara con los hashes en BD.
     *
     * Reutiliza la misma conexion de la transaccion activa
     * para que la verificacion sea parte de la misma transaccion.
     *
     * conn  conexion activa (con transaccion en progreso)
     * clave clave del director en texto plano
     * @return true si hay una fila activa con ese hash en clave_sistema
     */
    private boolean claveValida(Connection conn, String clave) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_CLAVE_VALIDA)) {
            ps.setString(1, encriptar(clave)); // hashear antes de comparar
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // hay fila = clave correcta
            }
        }
    }

    private String encriptar(String texto) {
        try {
            // Obtener una instancia del algoritmo SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Calcular el hash: convierte el texto a bytes UTF-8 y genera los 32 bytes del hash
            byte[] hash = digest.digest(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Codificar los bytes del hash en Base64 para obtener una cadena legible/almacenable
            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre existe en Java estandar; este catch es solo por la firma del metodo
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}


