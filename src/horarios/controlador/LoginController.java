/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.controlador;
import horarios.dao.UserDAO;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  CONTROLADOR: LoginController
 *  CAPA:        Controlador (MVC)
 * ============================================================
 *  Intermediario entre LoginWindow/RegisterWindow (vista)
 *  y UserDAO (capa de datos).
 *
 *  Responsabilidades:
 *    1. Validar campos del formulario ANTES de llamar al DAO.
 *    2. Delegar operaciones reales (login, registro, existencia)
 *       al UserDAO.
 *
 *  Razon de existir: la vista NO debe saber como se valida
 *  o como se conecta a la BD. El controlador desacopla ambas capas.
 * ============================================================
 */
public class LoginController {

    // Instancia del DAO que hace el trabajo real con la BD
    private final UserDAO userDAO = new UserDAO();

    /**
     * Intenta iniciar sesion con las credenciales dadas.
     * Verifica primero que los campos no esten vacios para
     * no hacer un viaje innecesario a la BD.
     *
     * @param username nombre de usuario ingresado en el campo de texto
     * @param password contrasena ingresada en el campo de contrasena
     * @return true si las credenciales son correctas, false en caso contrario
     */
    public boolean iniciarSesion(String username, String password) {
        // Validacion rapida: si alguno esta vacio, rechazar sin ir a la BD
        if (username.isBlank() || password.isBlank()) return false;

        // Delegar la verificacion real al UserDAO (que compara hash SHA-256)
        return userDAO.login(username, password);
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * Valida el formulario primero; si hay algun error de validacion,
     * retorna false sin insertar nada en la BD.
     *
     * @param username     nombre de usuario deseado
     * @param password     contrasena elegida
     * @param email        correo electronico
     * @param claveSistema clave maestra del director (requerida para registrarse)
     * @return true si el registro fue exitoso
     */
    public boolean registrar(String username, String password, String email, String claveSistema) {
        // Reutiliza validarFormularioRegistro; si retorna un mensaje de error
        // (distinto de null), el formulario tiene datos invalidos -> no registrar
        if (validarFormularioRegistro(username, email, password, password, claveSistema) != null)
            return false;

        // Datos validos: delegar al DAO para insertar en BD
        return userDAO.registrar(username, password, email, claveSistema);
    }

    /**
     * Verifica si un nombre de usuario ya esta registrado en el sistema.
     * Usado en RegisterWindow para dar feedback inmediato al usuario.
     *
     * @param username nombre a verificar
     * @return true si ya existe (no esta disponible)
     */
    public boolean usuarioExiste(String username) {
        return userDAO.existe(username);
    }

    /**
     * Valida todos los campos del formulario de registro.
     *
     * Reglas:
     *   - username: minimo 4 caracteres
     *   - email: debe contener "@"
     *   - password: minimo 6 caracteres
     *   - confirm: debe ser identica a password
     *   - clave del sistema: minimo 4 caracteres
     */
    public String validarFormularioRegistro(String username, String email,
                                            String password, String confirm, String clave) {
        // Cada condicion retorna el primer error encontrado (fail-fast)
        if (username.length() < 4)     return "El usuario debe tener al menos 4 caracteres";
        if (!email.contains("@"))      return "Ingrese un correo electronico valido";
        if (password.length() < 6)     return "La contrasena debe tener al menos 6 caracteres";
        if (!password.equals(confirm)) return "Las contrasenas no coinciden";
        if (clave.length() < 4)        return "La clave del sistema debe tener al menos 4 caracteres";
        return null; //sin errores, todo correcto
    }
}
