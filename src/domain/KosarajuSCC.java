/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package domain;

import java.util.ArrayDeque; // pila/cola eficiente
import java.util.ArrayList; // listas dinámicas
import java.util.Deque; // estructura de pila/cola
import java.util.LinkedHashSet; // conjunto con orden de inserción
import java.util.List; // interfaz de lista
import java.util.Map; // interfaz de mapa
import java.util.Objects; // utilidades para validar nulos
import java.util.Set; // interfaz de conjunto

public class KosarajuSCC { // implementa el algoritmo iterativo de Kosaraju

    public List<Set<String>> compute(final DirectedGraph graph) { // calcula las SCC del grafo
        Objects.requireNonNull(graph, "graph cannot be null"); // valida
        final List<String> finishingOrder = new ArrayList<>(); // orden de finalización del primer DFS
        final Set<String> visited = new LinkedHashSet<>(); // nodos visitados en primer DFS
        final Set<String> finished = new LinkedHashSet<>(); // nodos ya finalizados

        graph.getUsers().forEach(user -> { // recorre todos los nodos
            if (!visited.contains(user)) { // si no fue visitado
                depthFirstSearch(graph.getAdjacencyView(), user, visited, finished, finishingOrder); // DFS iterativo para generar orden de finalización
            }
        });

        final DirectedGraph transpose = GraphUtils.transpose(graph); // invierte el grafo
        final Map<String, List<String>> transposeAdjacency = transpose.getAdjacencyView(); // obtiene adyacencia del transpuesto
        final Set<String> explored = new LinkedHashSet<>(); // nodos ya explorados en segundo paso
        final List<Set<String>> stronglyConnectedComponents = new ArrayList<>(); // lista resultado de SCCs

        for (int i = finishingOrder.size() - 1; i >= 0; i--) { // recorre en orden inverso de finalización
            final String user = finishingOrder.get(i); // nodo actual
            if (!explored.contains(user)) { // si no se exploró aún
                final Set<String> component = new LinkedHashSet<>(); // conjunto para la componente actual
                depthFirstCollect(transposeAdjacency, user, explored, component); // colecta todos los nodos de la SCC
                stronglyConnectedComponents.add(component); // añade componente al resultado
            }
        }

        return stronglyConnectedComponents; // devuelve todas las SCCs encontradas
    }

    private void depthFirstSearch(final Map<String, List<String>> adjacency,
            final String vertex,
            final Set<String> visited,
            final Set<String> finished,
            final List<String> finishingOrder) { // DFS iterativo que produce orden de finalización
        final Deque<String> stack = new ArrayDeque<>(); // pila explícita para evitar recursión
        stack.push(vertex); // empuja vértice inicial
        while (!stack.isEmpty()) { // mientras haya vértice en la pila
            final String current = stack.peek(); // mira el tope sin sacar
            if (!visited.contains(current)) { // si no ha sido visitado
                visited.add(current); // marca como visitado
                final List<String> neighbors = adjacency.getOrDefault(current, List.of()); // obtiene vecinos (si no hay, lista vacía)
                boolean pushed = false; // indica si se empujó algún vecino
                for (String neighbor : neighbors) { // itera vecinos
                    if (!visited.contains(neighbor)) { // si el vecino no fue visitado
                        stack.push(neighbor); // empuja vecino para recorrer
                        pushed = true; // marcamos que se empujó
                    }
                }
                if (pushed) { // si se empujaron vecinos, continuamos (no finalizamos aún este nodo)
                    continue;
                }
            }
            final String finishedVertex = stack.pop(); // no hay vecinos no visitados: sacamos el nodo
            if (finished.add(finishedVertex)) { // si se agrega a 'finished' (no estaba antes)
                finishingOrder.add(finishedVertex); // registramos el orden de finalización
            }
        }
    }

    private void depthFirstCollect(final Map<String, List<String>> adjacency,
            final String vertex,
            final Set<String> explored,
            final Set<String> component) { // DFS iterativo para colectar nodos de una SCC en el grafo transpuesto
        final Deque<String> stack = new ArrayDeque<>(); // pila explícita
        stack.push(vertex); // inicia desde el vértice dado
        while (!stack.isEmpty()) { // mientras haya elementos
            final String current = stack.pop(); // extrae tope
            if (explored.contains(current)) { // si ya fue explorado, continúa
                continue;
            }
            explored.add(current); // marca como explorado
            component.add(current); // lo añade a la componente actual
            final List<String> neighbors = adjacency.getOrDefault(current, List.of()); // obtiene vecinos en el transpuesto
            for (String neighbor : neighbors) { // itera vecinos
                if (!explored.contains(neighbor)) { // si no explorado
                    stack.push(neighbor); // empuja para explorar luego
                }
            }
        }
    }
}
