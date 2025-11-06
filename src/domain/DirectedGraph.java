/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package domain;

import java.util.ArrayList; // lista dinámica para adyacencias
import java.util.Collections; // utilidades de colecciones
import java.util.LinkedHashMap; // mapa con orden de inserción
import java.util.LinkedHashSet; // set con orden de inserción
import java.util.List; // interfaz de lista
import java.util.Map; // interfaz de mapa
import java.util.Objects; // utilidades para validar nulos
import java.util.Set; // interfaz de conjunto

public class DirectedGraph { // representación simple de grafo dirigido

    private final Map<String, List<String>> adjacency; // mapa: nodo -> lista de vecinos

    public DirectedGraph() { // constructor vacío
        this.adjacency = new LinkedHashMap<>(); // mantiene orden predecible de iteración
    }

    public DirectedGraph(final DirectedGraph other) { // copia profunda de otro grafo
        Objects.requireNonNull(other, "other graph cannot be null"); // valida no nulo
        this.adjacency = new LinkedHashMap<>(); // nuevo mapa para la copia
        other.adjacency.forEach((user, neighbors)
                -> this.adjacency.put(user, new ArrayList<>(neighbors))); // copia listas de vecinos
    }

    public boolean addUser(final String userHandle) { // agrega un usuario/nodo
        final String normalized = normalizeHandle(userHandle); // normaliza y valida handle
        if (adjacency.containsKey(normalized)) { // si ya existe, no hace nada
            return false; // indica que no se agregó
        }
        adjacency.put(normalized, new ArrayList<>()); // crea lista vacía de vecinos
        return true; // indica agregado con éxito
    }

    public boolean removeUser(final String userHandle) { // elimina un usuario y sus referencias
        final String normalized = normalizeHandle(userHandle); // normaliza
        if (!adjacency.containsKey(normalized)) { // si no existe, no hace nada
            return false; // indica que no se eliminó
        }
        adjacency.remove(normalized); // elimina la clave
        adjacency.values().forEach(neighbors -> neighbors.remove(normalized)); // elimina aristas entrantes
        return true; // éxito
    }

    public boolean addRelation(final String fromHandle, final String toHandle) { // agrega arista dirigida
        final String from = normalizeHandle(fromHandle); // normaliza origen
        final String to = normalizeHandle(toHandle); // normaliza destino
        ensureUserExists(from); // asegura que exista el nodo origen
        ensureUserExists(to); // asegura que exista el nodo destino
        final List<String> neighbors = adjacency.get(from); // obtiene lista de vecinos del origen
        if (neighbors.contains(to)) { // si ya existe la relación, no agrega
            return false; // indica que no se agregó
        }
        neighbors.add(to); // agrega el destino a la lista de vecinos
        return true; // éxito
    }

    public boolean removeRelation(final String fromHandle, final String toHandle) { // elimina arista
        final String from = normalizeHandle(fromHandle); // normaliza origen
        final String to = normalizeHandle(toHandle); // normaliza destino
        if (!adjacency.containsKey(from)) { // si origen no existe
            return false; // nothing to remove
        }
        return adjacency.get(from).remove(to); // intenta remover el destino y retorna si lo hizo
    }

    public List<String> getNeighbors(final String userHandle) { // devuelve vecinos de un nodo
        final String normalized = normalizeHandle(userHandle); // normaliza
        final List<String> neighbors = adjacency.get(normalized); // obtiene lista
        if (neighbors == null) { // si no existe el nodo
            return Collections.emptyList(); // retorna lista vacía inmodificable
        }
        return Collections.unmodifiableList(neighbors); // retorna vista de solo lectura
    }

    public Set<String> getUsers() { // lista de usuarios (nodos)
        return Collections.unmodifiableSet(adjacency.keySet()); // vista inmodificable del conjunto de claves
    }

    public int getEdgeCount() { // cuenta total de aristas
        return adjacency.values().stream().mapToInt(List::size).sum(); // suma tamaños de todas las listas
    }

    public Map<String, List<String>> getAdjacencyView() { // vista inmutable del mapa de adyacencia
        final Map<String, List<String>> snapshot = new LinkedHashMap<>(); // snapshot para no exponer estructura interna
        adjacency.forEach((key, value) -> snapshot.put(key, Collections.unmodifiableList(new ArrayList<>(value)))); // copia listas y las vuelve inmutables
        return Collections.unmodifiableMap(snapshot); // retorna mapa inmutable
    }

    public boolean containsUser(final String userHandle) { // verifica existencia de usuario
        return adjacency.containsKey(normalizeHandle(userHandle)); // busca clave normalizada
    }

    public void ensureUsersPresent(final Iterable<String> userHandles) { // asegura que una colección de handles exista como usuarios
        final Set<String> normalizedHandles = new LinkedHashSet<>(); // conjunto para evitar duplicados
        for (String handle : userHandles) {
            normalizedHandles.add(normalizeHandle(handle)); // normaliza y añade
        }
        normalizedHandles.forEach(this::addUser); // agrega cada handle (addUser ignora duplicados existentes)
    }

    private void ensureUserExists(final String userHandle) { // lanza si un usuario no existe
        if (!adjacency.containsKey(userHandle)) {
            throw new IllegalArgumentException("Usuario no registrado: " + userHandle); // error si falta usuario
        }
    }

    private String normalizeHandle(final String handle) { // valida y normaliza un handle (trim)
        final String normalized = Objects.requireNonNull(handle, "El handle no puede ser nulo").trim(); // valida no nulo y recorta espacios
        if (normalized.isEmpty()) { // si queda vacío
            throw new IllegalArgumentException("El handle no puede estar vacío"); // error
        }
        return normalized; // retorna el handle normalizado
    }
}
