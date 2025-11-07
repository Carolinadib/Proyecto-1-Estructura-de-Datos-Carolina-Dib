package domain; // utilidades para manipular grafos

/**
 * Utilidades auxiliares para operar sobre grafos dirigidos. Contiene métodos
 * para copiar, transponer y validar estructuras de {@link DirectedGraph} así
 * como otras funciones de ayuda.
 */
public final class GraphUtils {

    private static final String GRAPH_CANNOT_BE_NULL = "graph cannot be null";

    private GraphUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static DirectedGraph copyOf(final DirectedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException(GRAPH_CANNOT_BE_NULL);
        }
        return new DirectedGraph(graph);
    }

    public static DirectedGraph transpose(final DirectedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException(GRAPH_CANNOT_BE_NULL);
        }
        final DirectedGraph transpose = new DirectedGraph();
        // copiar usuarios
        transpose.ensureUsersPresent(graph.getUsers());
        // invertir aristas usando la vista de adyacencia
        final DirectedGraph.AdjacencyView view = graph.getAdjacencyView();
        final String[] users = view.users();
        final String[][] neighbors = view.neighbors();
        for (int i = 0; i < users.length; i++) {
            final String from = users[i];
            final String[] neigh = neighbors[i];
            for (int j = 0; j < neigh.length; j++) {
                transpose.addRelation(neigh[j], from);
            }
        }
        return transpose;
    }

    /**
     * Asegura que todas las referencias (pares origen->vecinos) existan como
     * usuarios. La representación se pasa como arrays paralelos.
     */
    public static void ensureAllReferencesExist(final DirectedGraph graph, final String[] froms, final String[][] neighbors) {
        if (graph == null) {
            throw new IllegalArgumentException(GRAPH_CANNOT_BE_NULL);
        }
        if (froms == null) {
            throw new IllegalArgumentException("references cannot be null");
        }
        // recoger todos los handles en un arreglo dinámico simple
        // conteo aproximado
        int count = 0;
        for (int i = 0; i < froms.length; i++) {
            count++;
            final String[] n = neighbors == null || neighbors.length <= i ? new String[0] : neighbors[i];
            count += n.length;
        }
        final String[] all = new String[count];
        int pos = 0;
        for (int i = 0; i < froms.length; i++) {
            all[pos++] = froms[i];
            final String[] n = neighbors == null || neighbors.length <= i ? new String[0] : neighbors[i];
            for (int j = 0; j < n.length; j++) {
                all[pos++] = n[j];
            }
        }
        graph.ensureUsersPresent(all);
    }

    public static void validateHandle(final String handle) {
        if (handle == null) {
            throw new IllegalArgumentException("El handle no puede ser nulo");
        }
        final String trimmed = handle.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("El handle no puede estar vacío");
        }
        if (!trimmed.startsWith("@")) {
            throw new IllegalArgumentException("El handle debe iniciar con '@'");
        }
    }

    public static DirectedGraph.AdjacencyView snapshotAdjacency(final DirectedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException(GRAPH_CANNOT_BE_NULL);
        }
        return graph.getAdjacencyView();
    }

    public static int countOutgoingEdges(final DirectedGraph graph, final String[] users) {
        if (graph == null) {
            throw new IllegalArgumentException(GRAPH_CANNOT_BE_NULL);
        }
        if (users == null) {
            throw new IllegalArgumentException("users cannot be null");
        }
        int sum = 0;
        for (int i = 0; i < users.length; i++) {
            sum += graph.getNeighbors(users[i]).length;
        }
        return sum;
    }
}
