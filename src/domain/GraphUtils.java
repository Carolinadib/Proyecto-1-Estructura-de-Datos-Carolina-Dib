/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package domain;
import java.util.ArrayDeque; // deque para acumulación
import java.util.ArrayList; // listas mutables
import java.util.Deque; // interfaz deque
import java.util.LinkedHashMap; // mapa con orden de inserción
import java.util.List; // interfaz lista
import java.util.Map; // interfaz mapa
import java.util.Objects; // utilidades para validar nulos
import java.util.Set; // interfaz set

public final class GraphUtils { // clase de utilidades estáticas

    private static final String GRAPH_CANNOT_BE_NULL = "graph cannot be null"; // mensaje reutilizable

    private GraphUtils() { // previene instanciación
        throw new UnsupportedOperationException("Utility class");
    }

    public static DirectedGraph copyOf(final DirectedGraph graph) { // copia profunda del grafo
        Objects.requireNonNull(graph, GRAPH_CANNOT_BE_NULL); // valida no nulo
        return new DirectedGraph(graph); // delega al constructor copia
    }

    public static DirectedGraph transpose(final DirectedGraph graph) { // calcula el grafo transpuesto (aristas invertidas)
        Objects.requireNonNull(graph, GRAPH_CANNOT_BE_NULL); // valida
        final DirectedGraph transpose = new DirectedGraph(); // nuevo grafo para el transpuesto
        transpose.ensureUsersPresent(graph.getUsers()); // añade los mismos nodos
        graph.getAdjacencyView().forEach((from, neighbors)
                -> neighbors.forEach(to -> transpose.addRelation(to, from))); // invierte cada arista
        return transpose; // retorna el grafo transpuesto
    }

    public static void ensureAllReferencesExist(final DirectedGraph graph, final Map<String, List<String>> references) { // asegura que todas las referencias existan como usuarios
        Objects.requireNonNull(graph, GRAPH_CANNOT_BE_NULL); // valida
        Objects.requireNonNull(references, "references cannot be null"); // valida referencias
        final Deque<String> pending = new ArrayDeque<>(); // cola temporal para normalizar
        references.forEach((from, neighbors) -> {
            pending.add(from); // añade origen
            pending.addAll(neighbors); // añade destinos
        });
        graph.ensureUsersPresent(pending); // crea usuarios faltantes
    }

    public static void validateHandle(final String handle) { // valida formato básico de handle
        final String trimmed = Objects.requireNonNull(handle, "El handle no puede ser nulo").trim(); // valida no nulo y recorta
        if (trimmed.isEmpty()) { // si quedó vacío
            throw new IllegalArgumentException("El handle no puede estar vacío"); // lanza
        }
        if (!trimmed.startsWith("@")) { // debe empezar con @
            throw new IllegalArgumentException("El handle debe iniciar con '@'"); // lanza
        }
    }

    public static Map<String, List<String>> snapshotAdjacency(final DirectedGraph graph) { // snapshot seguro de la adyacencia
        Objects.requireNonNull(graph, GRAPH_CANNOT_BE_NULL); // valida
        final Map<String, List<String>> snapshot = new LinkedHashMap<>(); // mapa de copia
        graph.getAdjacencyView().forEach((user, neighbors) -> snapshot.put(user, new ArrayList<>(neighbors))); // copia listas
        return snapshot; // retorna copia mutable
    }

    public static int countOutgoingEdges(final DirectedGraph graph, final Set<String> users) { // cuenta aristas salientes para un conjunto de usuarios
        Objects.requireNonNull(graph, GRAPH_CANNOT_BE_NULL); // valida
        Objects.requireNonNull(users, "users cannot be null"); // valida
        return users.stream()
                .map(graph::getNeighbors) // obtiene vecinos por usuario
                .mapToInt(List::size) // cuenta por usuario
                .sum(); // suma total
    }
}
