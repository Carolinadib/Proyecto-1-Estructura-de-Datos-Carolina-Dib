
package services;

import domain.DirectedGraph; // modelo de grafo
import domain.GraphUtils; // utilidades del dominio
import domain.KosarajuSCC; // algoritmo de SCC
import domain.User; // representación de usuario
import io.GraphFileParser; // parser de archivos
import io.GraphFileWriter; // escritor de archivos

import java.io.IOException; // excepciones E/S
import java.io.InputStreamReader; // lector de recursos
import java.io.Reader; // interfaz Reader
import java.nio.charset.StandardCharsets; // codificación UTF-8
import java.nio.file.Path; // ruta de archivo
import java.util.ArrayList; // lista mutable
import java.util.Collections; // utilidades de colecciones
import java.util.LinkedHashMap; // mapa con orden
import java.util.List; // interfaz lista
import java.util.Map; // interfaz mapa
import java.util.Objects; // validación nulos
import java.util.Optional; // valor opcional
import java.util.Set; // interfaz set
import java.util.concurrent.atomic.AtomicInteger; // contador atómico para ids
import java.util.stream.Collectors; // util stream (no usado en este archivo)

public class GraphService { // servicio principal que coordina lectura, modificaciones y cómputos

    private final GraphFileParser parser; // parser inyectado
    private final GraphFileWriter writer; // escritor inyectado
    private final KosarajuSCC kosaraju; // algoritmo para SCCs

    private DirectedGraph graph; // estado actual del grafo en memoria
    private Path currentFile; // archivo asociado (si se guardó o cargó)
    private boolean dirty; // bandera de cambios sin guardar
    private Map<String, Integer> lastSccMapping; // mapeo de último cálculo SCC: handle->componenteId
    private List<Set<String>> lastComponents; // lista de componentes del último cálculo

    public GraphService(final GraphFileParser parser, final GraphFileWriter writer) { // constructor con dependencias
        this.parser = Objects.requireNonNull(parser, "parser"); // valida parser
        this.writer = Objects.requireNonNull(writer, "writer"); // valida writer
        this.kosaraju = new KosarajuSCC(); // crea instancia del algoritmo Kosaraju
        this.graph = new DirectedGraph(); // grafo vacío por defecto
        this.lastSccMapping = Collections.emptyMap(); // mapeo vacío inicial
        this.lastComponents = Collections.emptyList(); // componentes vacíos
    }

    public synchronized GraphLoadResult loadInitialGraphFromResource(final String resourcePath) throws IOException { // carga un recurso embebido
        Objects.requireNonNull(resourcePath, "resourcePath"); // valida
        try (Reader reader = openResource(resourcePath)) { // abre lector del recurso
            final GraphFileParser.Result result = parser.parse(reader); // parsea el recurso
            applyParsedGraph(result, null); // aplica grafo parseado
            this.dirty = false; // no hay cambios recién cargados
            return buildLoadResult(result, null); // construye resultado para la UI
        }
    }

    public synchronized GraphLoadResult loadFromFile(final Path path) throws IOException { // carga desde archivo del sistema
        final GraphFileParser.Result result = parser.parse(path); // parsea archivo
        applyParsedGraph(result, path); // aplica grafo
        this.dirty = false; // marca limpio
        return buildLoadResult(result, path); // retorna info de carga
    }

    public synchronized void save() throws IOException { // guarda en el archivo actual
        if (currentFile == null) { // si no hay archivo asociado
            throw new IOException("No hay un archivo asociado. Use 'Guardar como…'."); // error
        }
        saveAs(currentFile); // delega a saveAs
    }

    public synchronized void saveAs(final Path path) throws IOException { // guarda en la ruta dada
        Objects.requireNonNull(path, "path"); // valida
        writer.write(path, graph); // escribe grafo
        this.currentFile = path; // actualiza archivo asociado
        this.dirty = false; // limpia bandera
    }

    public synchronized void createNewGraph() { // crea grafo vacío
        this.graph = new DirectedGraph(); // nueva instancia
        this.currentFile = null; // sin archivo asociado
        this.dirty = false; // limpio
        resetSccState(); // borra estado de SCC
    }

    public synchronized void addUser(final String handle) { // agrega un usuario al grafo
        GraphUtils.validateHandle(handle); // valida formato del handle
        if (!graph.addUser(handle)) { // intenta agregar y si ya existe
            throw new IllegalArgumentException("El usuario ya existe: " + handle); // lanza excepción
        }
        markDirty(); // marca cambios pendientes
    }

    public synchronized void removeUser(final String handle) { // elimina usuario
        GraphUtils.validateHandle(handle); // valida handle
        if (!graph.removeUser(handle)) { // intenta eliminar
            throw new IllegalArgumentException("No existe el usuario: " + handle); // lanza si no existe
        }
        markDirty(); // marca cambio
    }

    public synchronized void addRelation(final String origin, final String destination) { // agrega relación dirigida
        GraphUtils.validateHandle(origin); // valida origen
        GraphUtils.validateHandle(destination); // valida destino
        if (origin.equalsIgnoreCase(destination)) { // evita relaciones autorefenciales
            throw new IllegalArgumentException("No se permiten relaciones de un usuario hacia sí mismo."); // error
        }
        ensureUsersExist(origin, destination); // asegura que ambos usuarios existan
        if (!graph.addRelation(origin, destination)) { // intenta agregar relación
            throw new IllegalArgumentException("La relación ya existe: " + origin + " → " + destination); // error si ya existía
        }
        markDirty(); // marca cambios
    }

    public synchronized void removeRelation(final String origin, final String destination) { // elimina relación
        GraphUtils.validateHandle(origin); // valida
        GraphUtils.validateHandle(destination); // valida
        if (!graph.removeRelation(origin, destination)) { // intenta remover
            throw new IllegalArgumentException("No existe la relación: " + origin + " → " + destination); // error si no existía
        }
        markDirty(); // marca cambios
    }

    public synchronized SccComputationResult computeStronglyConnectedComponents() { // calcula SCCs usando Kosaraju
        final List<Set<String>> components = kosaraju.compute(graph); // ejecuta algoritmo
        final Map<String, Integer> mapping = buildSccMapping(components); // construye mapeo handle->id
        this.lastComponents = components; // guarda resultado
        this.lastSccMapping = mapping; // guarda mapeo
        return new SccComputationResult(components, mapping); // retorna resultado
    }

    public synchronized DirectedGraph getGraphSnapshot() { // obtiene copia del grafo para la UI
        return GraphUtils.copyOf(graph); // retorna copia profunda
    }

    public synchronized int getUserCount() { // número de usuarios
        return graph.getUsers().size(); // size del set de usuarios
    }

    public synchronized int getRelationCount() { // número de aristas
        return graph.getEdgeCount(); // delega en DirectedGraph
    }

    public synchronized List<User> getUsers() { // lista de usuarios como objetos User
        return graph.getUsers().stream().map(User::new).toList(); // crea lista de User
    }

    public synchronized List<Relation> getRelations() { // lista de relaciones (tuplas)
        final List<Relation> relations = new ArrayList<>(); // lista mutable
        graph.getAdjacencyView().forEach((from, neighbors)
                -> neighbors.forEach(to -> relations.add(new Relation(from, to)))); // agrega cada arista como Relation
        return relations; // retorna lista
    }

    public synchronized Optional<Path> getCurrentFile() { // archivo actualmente asociado
        return Optional.ofNullable(currentFile); // puede estar vacío
    }

    public synchronized boolean hasUnsavedChanges() { // indica si hay cambios sin guardar
        return dirty; // retorna flag
    }

    public synchronized Map<String, Integer> getLastSccMapping() { // mapeo de último cálculo SCC
        return Collections.unmodifiableMap(lastSccMapping); // retorna vista inmutable
    }

    public synchronized List<Set<String>> getLastComponents() { // componentes del último cálculo
        return Collections.unmodifiableList(lastComponents); // vista inmutable
    }

    private void applyParsedGraph(final GraphFileParser.Result result, final Path source) { // aplica grafo parseado al estado
        this.graph = GraphUtils.copyOf(result.graph()); // copia profunda
        this.currentFile = source; // actualiza origen
        resetSccState(); // limpia estado de SCC previo
    }

    private GraphLoadResult buildLoadResult(final GraphFileParser.Result result, final Path path) { // construye resultado rico para UI
        final DirectedGraph snapshot = GraphUtils.copyOf(result.graph()); // snapshot inmutable
        return new GraphLoadResult(snapshot,
                List.copyOf(result.warnings()),
                path,
                snapshot.getUsers().size(),
                snapshot.getEdgeCount()); // empaqueta info útil
    }

    private Reader openResource(final String resourcePath) throws IOException { // abre un recurso embebido como Reader
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader(); // obtiene class loader actual
        if (classLoader == null) { // si no se pudo obtener
            throw new IOException("No se pudo resolver el class loader actual"); // error
        }
        final var stream = classLoader.getResourceAsStream(resourcePath); // obtiene stream del recurso
        if (stream == null) { // si no existe recurso
            throw new IOException("Recurso no encontrado: " + resourcePath); // error
        }
        return new InputStreamReader(stream, StandardCharsets.UTF_8); // devuelve Reader con UTF-8
    }

    private void ensureUsersExist(final String... handles) { // valida que los handles existan en el grafo
        for (String handle : handles) { // itera
            if (!graph.containsUser(handle)) { // si falta alguno
                throw new IllegalArgumentException("Debe agregar primero al usuario: " + handle); // lanza
            }
        }
    }

    private void markDirty() { // marca que hubo cambios y resetea estado SCC
        this.dirty = true; // pone flag
        resetSccState(); // limpia último cálculo SCC
    }

    private void resetSccState() { // borra cache/estado de SCC
        this.lastSccMapping = Collections.emptyMap(); // mapa vacío
        this.lastComponents = Collections.emptyList(); // lista vacía
    }

    private Map<String, Integer> buildSccMapping(final List<Set<String>> components) { // construye mapeo user->componentId
        final Map<String, Integer> mapping = new LinkedHashMap<>(); // mantiene orden
        final AtomicInteger counter = new AtomicInteger(0); // contador para ids de componentes
        components.forEach(component -> {
            final int componentId = counter.getAndIncrement(); // id único por componente
            component.forEach(user -> mapping.put(user, componentId)); // asigna id a cada usuario
        });
        return mapping; // retorna mapeo
    }

    public record Relation(String from, String to) { // tupla inmutable que representa una relación

    }

    public record GraphLoadResult(DirectedGraph graphSnapshot,
            List<String> warnings,
            Path source,
            int userCount,
            int relationCount) { // resultado enriquecido al cargar un grafo

    }

    public record SccComputationResult(List<Set<String>> components, Map<String, Integer> mapping) { // resultado del cómputo SCC

    }
}
