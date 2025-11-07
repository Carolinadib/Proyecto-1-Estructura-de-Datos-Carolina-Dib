package domain; // paquete con clases del modelo de dominio

/**
 * Grafo dirigido simple que representa usuarios como nodos y relaciones
 * "seguimiento" como aristas dirigidas. Implementa operaciones básicas para
 * agregar/eliminar usuarios y relaciones y ofrece una vista inmutable de la
 * adyacencia.
 */
public class DirectedGraph {

    // Representación interna: arreglo de nodos (orden de inserción)
    private Node[] nodes;
    private int size; // número de nodos presentes

    /**
     * Crea un grafo dirigido vacío.
     */
    public DirectedGraph() {
        this.nodes = new Node[8];
        this.size = 0;
    }

    /**
     * Crea una copia profunda del grafo dado.
     *
     * @param other grafo a copiar
     */
    public DirectedGraph(final DirectedGraph other) {
        if (other == null) {
            throw new IllegalArgumentException("other graph cannot be null");
        }
        this.nodes = new Node[Math.max(8, other.size)];
        for (int i = 0; i < other.size; i++) {
            Node n = other.nodes[i];
            this.nodes[i] = new Node(n.handle, n.neighbors == null ? new String[0] : n.neighbors.clone());
        }
        this.size = other.size;
    }

    /**
     * Añade un usuario identificado por su handle si no existe.
     *
     * @param userHandle handle del usuario (ej. "@pepe")
     * @return {@code true} si el usuario fue añadido, {@code false} si ya
     * existía
     */
    public boolean addUser(final String userHandle) {
        final String normalized = normalizeHandle(userHandle);
        if (indexOf(normalized) >= 0) {
            return false;
        }
        ensureCapacity();
        nodes[size++] = new Node(normalized, new String[0]);
        return true;
    }

    /**
     * Elimina un usuario y todas sus referencias entrantes.
     *
     * @param userHandle handle del usuario a eliminar
     * @return {@code true} si el usuario existía y fue eliminado
     */
    public boolean removeUser(final String userHandle) {
        final String normalized = normalizeHandle(userHandle);
        final int idx = indexOf(normalized);
        if (idx < 0) {
            return false;
        }
        // eliminar nodo
        for (int i = idx; i < size - 1; i++) {
            nodes[i] = nodes[i + 1];
        }
        nodes[size - 1] = null;
        size--;
        // eliminar referencias entrantes
        for (int i = 0; i < size; i++) {
            nodes[i].removeNeighbor(normalized);
        }
        return true;
    }

    /**
     * Añade una relación dirigida desde {@code fromHandle} hacia
     * {@code toHandle}.
     *
     * @param fromHandle handle del usuario origen
     * @param toHandle handle del usuario destino
     * @return {@code true} si la relación fue añadida; {@code false} si ya
     * existía
     */
    public boolean addRelation(final String fromHandle, final String toHandle) {
        final String from = normalizeHandle(fromHandle);
        final String to = normalizeHandle(toHandle);
        final int iFrom = indexOf(from);
        final int iTo = indexOf(to);
        ensureUserExistsIndex(iFrom, from);
        ensureUserExistsIndex(iTo, to);
        return nodes[iFrom].addNeighborIfAbsent(to);
    }

    /**
     * Elimina la relación dirigida especificada.
     *
     * @param fromHandle handle del usuario origen
     * @param toHandle handle del usuario destino
     * @return {@code true} si la relación existía y fue eliminada
     */
    public boolean removeRelation(final String fromHandle, final String toHandle) {
        final String from = normalizeHandle(fromHandle);
        final String to = normalizeHandle(toHandle);
        final int iFrom = indexOf(from);
        if (iFrom < 0) {
            return false;
        }
        return nodes[iFrom].removeNeighbor(to);
    }

    /**
     * Devuelve los vecinos como copia de un arreglo (inmutable para el
     * llamador).
     */
    /**
     * Obtiene una copia del arreglo de vecinos (destinos) del usuario dado.
     *
     * @param userHandle handle del usuario
     * @return arreglo (copia) de handles vecinos; arreglo vacío si no existe
     */
    public String[] getNeighbors(final String userHandle) {
        final String normalized = normalizeHandle(userHandle);
        final int idx = indexOf(normalized);
        if (idx < 0) {
            return new String[0];
        }
        return nodes[idx].neighbors.clone();
    }

    /**
     * Devuelve los usuarios en orden de inserción.
     */
    /**
     * Devuelve los usuarios en orden de inserción.
     *
     * @return arreglo con los handles de los usuarios
     */
    public String[] getUsers() {
        final String[] result = new String[size];
        for (int i = 0; i < size; i++) {
            result[i] = nodes[i].handle;
        }
        return result;
    }

    /**
     * Cuenta el número total de aristas salientes en el grafo.
     *
     * @return número total de relaciones (aristas)
     */
    public int getEdgeCount() {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += nodes[i].neighbors.length;
        }
        return sum;
    }

    /**
     * Retorna una vista inmutable simple de la adyacencia: un arreglo de
     * usuarios y un arreglo paralelo de arreglos de vecinos.
     */
    /**
     * Retorna una vista inmutable simple de la adyacencia.
     *
     * @return {@link AdjacencyView} que contiene usuarios y vecinos
     */
    public AdjacencyView getAdjacencyView() {
        final String[] users = getUsers();
        final String[][] neighbors = new String[size][];
        for (int i = 0; i < size; i++) {
            neighbors[i] = nodes[i].neighbors.clone();
        }
        return new AdjacencyView(users, neighbors);
    }

    /**
     * Indica si un usuario con el handle dado existe en el grafo.
     *
     * @param userHandle handle a comprobar
     * @return {@code true} si existe, {@code false} en caso contrario
     */
    public boolean containsUser(final String userHandle) {
        return indexOf(normalizeHandle(userHandle)) >= 0;
    }

    /**
     * Asegura que los handles pasados existan; acepta un arreglo de strings.
     */
    /**
     * Asegura que los handles pasados existan en el grafo (los crea si no).
     *
     * @param userHandles arreglo de handles a asegurar
     */
    public void ensureUsersPresent(final String[] userHandles) {
        if (userHandles == null) {
            throw new IllegalArgumentException("userHandles cannot be null");
        }
        for (int i = 0; i < userHandles.length; i++) {
            final String normalized = normalizeHandle(userHandles[i]);
            addUser(normalized);
        }
    }

    private void ensureUserExistsIndex(final int idx, final String handle) {
        if (idx < 0) {
            throw new IllegalArgumentException("Usuario no registrado: " + handle);
        }
    }

    private int indexOf(final String handle) {
        for (int i = 0; i < size; i++) {
            if (nodes[i].handle.equals(handle)) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeHandle(final String handle) {
        if (handle == null) {
            throw new IllegalArgumentException("El handle no puede ser nulo");
        }
        final String normalized = handle.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("El handle no puede estar vacío");
        }
        return normalized;
    }

    private void ensureCapacity() {
        if (size >= nodes.length) {
            final Node[] n = new Node[nodes.length * 2];
            for (int i = 0; i < nodes.length; i++) {
                n[i] = nodes[i];
            }
            nodes = n;
        }
    }

    // Nodo simple que contiene handle y vecinos como arreglo dinámico
    private static final class Node {

        final String handle;
        String[] neighbors;

        Node(final String handle, final String[] neighbors) {
            this.handle = handle;
            this.neighbors = neighbors == null ? new String[0] : neighbors;
        }

        boolean addNeighborIfAbsent(final String to) {
            for (int i = 0; i < neighbors.length; i++) {
                if (neighbors[i].equals(to)) {
                    return false;
                }
            }
            // agregar
            final String[] n = new String[neighbors.length + 1];
            for (int i = 0; i < neighbors.length; i++) {
                n[i] = neighbors[i];
            }
            n[neighbors.length] = to;
            neighbors = n;
            return true;
        }

        boolean removeNeighbor(final String to) {
            int idx = -1;
            for (int i = 0; i < neighbors.length; i++) {
                if (neighbors[i].equals(to)) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                return false;
            }
            final String[] n = new String[neighbors.length - 1];
            for (int i = 0, j = 0; i < neighbors.length; i++) {
                if (i == idx) {
                    continue;
                }
                n[j++] = neighbors[i];
            }
            neighbors = n;
            return true;
        }
    }

    /**
     * Vista simple e inmutable de la adyacencia.
     */
    public static final class AdjacencyView {

        private final String[] users;
        private final String[][] neighbors;

        AdjacencyView(final String[] users, final String[][] neighbors) {
            this.users = users == null ? new String[0] : users.clone();
            this.neighbors = neighbors == null ? new String[0][] : deepClone(neighbors);
        }

        public String[] users() {
            return users.clone();
        }

        public String[][] neighbors() {
            return deepClone(neighbors);
        }

        public String[] neighborsOf(final String user) {
            if (user == null) {
                return new String[0];
            }
            for (int i = 0; i < users.length; i++) {
                if (users[i].equals(user)) {
                    return neighbors[i].clone();
                }
            }
            return new String[0];
        }

        private static String[][] deepClone(final String[][] src) {
            final String[][] out = new String[src.length][];
            for (int i = 0; i < src.length; i++) {
                out[i] = src[i] == null ? new String[0] : src[i].clone();
            }
            return out;
        }
    }

}
