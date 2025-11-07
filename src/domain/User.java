package domain; // clase de modelo para representar un usuario

/**
 * Envoltorio ligero que representa un usuario mediante su handle (por ejemplo
 * {@code @pepe}). Proporciona operaciones básicas como acceso al handle y
 * métodos {@code equals/hashCode}.
 */
public final class User { // envoltorio ligero para un handle de usuario

    private final String handle; // identificador del usuario (ej. @usuario)

    /**
     * Construye un {@code User} a partir de su handle.
     *
     * @param handle handle válido del usuario (ej. "@pepe")
     */
    public User(final String handle) { // constructor valida formato
        GraphUtils.validateHandle(handle); // valida que no sea nulo, no vacío y empiece con '@'
        this.handle = handle; // asigna handle
    }

    /**
     * Obtiene el handle del usuario.
     *
     * @return handle del usuario
     */
    public String getHandle() { // getter del handle
        return handle; // retorna el identificador
    }

    @Override
    public boolean equals(final Object obj) { // igualdad basada en el handle
        if (this == obj) { // misma referencia
            return true; // son iguales
        }
        if (!(obj instanceof User other)) { // si no es instancia de User
            return false; // no iguales
        }
        return handle.equals(other.handle); // compara handles
    }

    @Override
    public int hashCode() { // hash basado en el handle
        return handle.hashCode(); // delega en String.hashCode
    }

    @Override
    public String toString() { // representación en texto
        return handle; // devuelve el handle
    }
}
