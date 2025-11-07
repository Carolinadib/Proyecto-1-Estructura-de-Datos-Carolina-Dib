package domain; // algoritmo Kosaraju para componentes fuertemente conectadas

/**
 * Implementación del algoritmo de Kosaraju para detectar componentes
 * fuertemente conectadas en un {@link DirectedGraph}. Devuelve las componentes
 * como un arreglo de arreglos de handles de usuario.
 */
public class KosarajuSCC {

    /**
     * Ejecuta el algoritmo de Kosaraju sobre el grafo dado y retorna las
     * componentes fuertemente conectadas como un arreglo de arreglos de
     * handles.
     *
     * @param graph grafo sobre el cual calcular las CFC
     * @return arreglo con las componentes detectadas (cada componente es un
     * arreglo de handles)
     */
    public String[][] compute(final DirectedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph cannot be null");
        }

        final DirectedGraph.AdjacencyView view = graph.getAdjacencyView();
        final String[] users = view.users();
        final String[][] neighbors = view.neighbors();
        final int n = users.length;

        // estructuras de control por índice
        final boolean[] visited = new boolean[n];
        final boolean[] finished = new boolean[n];
        final int[] finishingOrder = new int[n];
        int foSize = 0;

        // helper para obtener índice de un usuario (lineal)
        final IndexOf indexOf = new IndexOf(users);

        // primer DFS (iterativo), registrando orden de finalización
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                // pila de índices
                final int[] stack = new int[n];
                int sp = 0;
                stack[sp++] = i;
                while (sp > 0) {
                    final int current = stack[sp - 1];
                    if (!visited[current]) {
                        visited[current] = true;
                        final String[] neigh = neighbors.length > current ? neighbors[current] : new String[0];
                        boolean pushed = false;
                        for (int j = 0; j < neigh.length; j++) {
                            final int idx = indexOf.of(neigh[j]);
                            if (idx >= 0 && !visited[idx]) {
                                stack[sp++] = idx;
                                pushed = true;
                            }
                        }
                        if (pushed) {
                            continue;
                        }
                    }
                    final int finishedVertex = stack[--sp];
                    if (!finished[finishedVertex]) {
                        finished[finishedVertex] = true;
                        finishingOrder[foSize++] = finishedVertex;
                    }
                }
            }
        }

        // segundo paso: recorrer grafo transpuesto
        final DirectedGraph transpose = GraphUtils.transpose(graph);
        final DirectedGraph.AdjacencyView tview = transpose.getAdjacencyView();
        final String[] tusers = tview.users();
        final String[][] tneighbors = tview.neighbors();
        final IndexOf tIndexOf = new IndexOf(tusers);
        final boolean[] explored = new boolean[n];

        // lista dinámica de componentes
        final String[][] componentsTemp = new String[n][];
        int componentsCount = 0;

        for (int k = foSize - 1; k >= 0; k--) {
            final int userIdx = finishingOrder[k];
            if (explored[userIdx]) {
                continue;
            }
            // recolectar componente
            final int[] stack = new int[n];
            int sp = 0;
            stack[sp++] = userIdx;
            // dynamic collector
            final String[] collected = new String[n];
            int collectedSize = 0;
            while (sp > 0) {
                final int current = stack[--sp];
                if (explored[current]) {
                    continue;
                }
                explored[current] = true;
                collected[collectedSize++] = tusers[current];
                final String[] neigh = tneighbors.length > current ? tneighbors[current] : new String[0];
                for (int j = 0; j < neigh.length; j++) {
                    final int idx = tIndexOf.of(neigh[j]);
                    if (idx >= 0 && !explored[idx]) {
                        stack[sp++] = idx;
                    }
                }
            }
            // copy collected into tight array
            final String[] comp = new String[collectedSize];
            for (int c = 0; c < collectedSize; c++) {
                comp[c] = collected[c];
            }
            componentsTemp[componentsCount++] = comp;
        }

        // compact components array
        final String[][] result = new String[componentsCount][];
        for (int i = 0; i < componentsCount; i++) {
            result[i] = componentsTemp[i];
        }
        return result;
    }

    // helper simple para indexOf en un arreglo de strings
    private static final class IndexOf {

        private final String[] arr;

        IndexOf(final String[] arr) {
            this.arr = arr == null ? new String[0] : arr;
        }

        int of(final String s) {
            if (s == null) {
                return -1;
            }
            for (int i = 0; i < arr.length; i++) {
                if (s.equals(arr[i])) {
                    return i;
                }
            }
            return -1;
        }
    }
}
