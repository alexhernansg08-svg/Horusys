/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package horarios.modelo;
/**
 *
 * @author axelp
 */
/**
 * ============================================================
 *  MODELO: User
 *  CAPA:   Modelo (POJO)
 * ============================================================
 *  Representa a un usuario administrador del sistema (director).
 *
 *  Los usuarios se autentican con username + password (SHA-256).
 *  El registro requiere la "clave del sistema" del director,
 *  que se valida en la tabla clave_sistema antes de insertar.
 *
 *  Esta clase es un POJO (Plain Old Java Object):
 *    - Solo tiene atributos, constructor y getters/setters.
 *    - No contiene logica de negocio ni acceso a BD.
 *    - UserDAO es quien la lee/escribe en la base de datos.
 *    - LoginController valida sus datos antes de persistirla.
 *
 *  Tabla en BD: usuarios (username, password, email)
 * ============================================================
 */
public class User {

    // Atributos que mapean con las columnas de la tabla "usuarios"
    private String username;  // nombre de usuario (PRIMARY KEY)
    private String password;  // hash SHA-256 de la contrasena (NUNCA texto plano)
    private String email;     // correo electronico del administrador

    /** Constructor vacio: requerido por el DAO para instanciar al leer ResultSet. */
    public User() {}

    /**
     * Constructor completo: util al crear un objeto para login o registro.
     *
     * @param username nombre de usuario
     * @param password contrasena (se hashea en UserDAO antes de persistir)
     * @param email    correo electronico
     */
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email    = email;
    }

    // ---------------------------------------------------------------
    //  Getters y Setters (acceso controlado a los atributos privados)
    // ---------------------------------------------------------------

    public String getUsername()                  { return username; }
    public void   setUsername(String username)   { this.username = username; }

    public String getPassword()                  { return password; }
    public void   setPassword(String password)   { this.password = password; }

    public String getEmail()               { return email; }
    public void   setEmail(String email)   { this.email = email; }

    /**
     * Representacion legible para logs o mensajes de bienvenida.
     * Ejemplo: "admin (admin@cbtis22.edu.mx)"
     * NOTA: nunca incluir password en toString().
     */
    @Override
    public String toString() {
        return username + " (" + email + ")";
    }
}
