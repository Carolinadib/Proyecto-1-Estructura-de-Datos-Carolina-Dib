package services; // capa de servicio que expone operaciones sobre el grafo

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
// Usamos arreglos y tipos nulos en lugar de java.util

/**
 * Servicio que coordina la lectura/escritura de archivos, modificaciones sobre
 * el grafo en memoria y el cálculo de componentes fuertemente conectadas.
 * Expone operaciones seguras para la UI.
 */
public class GraphService { // servicio principal que coordina lectura, modificaciones y cómputos

    private final GraphFileParser parser; // parser inyectado
    private final GraphFileWriter writer; // escritor inyectado
    private final KosarajuSCC kosaraju; // algoritmo para SCCs

    private DirectedGraph graph; // estado actual del grafo en memoria
    private Path currentFile; // archivo asociado (si se guardó o cargó)
    private boolean dirty; // bandera de cambios sin guardar
    private SccMapping lastSccMapping; // mapeo de último cálculo SCC: handle->componenteId
    private String[][] lastComponents; // lista de componentes del último cálculo (arreglos de handles)

    /**
     * Construye el servicio con el parser y writer inyectados.
     *
     * @param parser parser para leer archivos de grafo
     * @param writer escritor para guardar grafos
     */
    public GraphService(final GraphFileParser parser, final GraphFileWriter writer) { // constructor con dependencias
        if (parser == null) {
            throw new IllegalArgumentException("parser");
        }
        if (writer == null) {
            throw new IllegalArgumentException("writer");
        }
        this.parser = parser;
        this.writer = writer;
        this.kosaraju = new KosarajuSCC(); // crea instancia del algoritmo Kosaraju
        this.graph = new DirectedGraph(); // grafo vacío por defecto
        this.lastSccMapping = SccMapping.empty(); // mapeo vacío inicial
        this.lastComponents = new String[0][]; // componentes vacíos
    }

    /**
     * Carga el grafo inicial desde un recurso incluido en el JAR.
     *
     * @param resourcePath ruta del recurso
     * @return información enriquecida del resultado de carga
     * @throws IOException si falla la lectura
     */
    public synchronized GraphLoadResult loadInitialGraphFromResource(final String resourcePath) throws IOException { // carga un recurso embebido
        if (resourcePath == null) {
            throw new IllegalArgumentException("resourcePath"); // valida

        }
        try (Reader reader = openResource(resourcePath)) { // abre lector del recurso
            final GraphFileParser.Result result = parser.parse(reader); // parsea el recurso
            applyParsedGraph(result, null); // aplica grafo parseado
            this.dirty = false; // no hay cambios recién cargados
            return buildLoadResult(result, null); // construye resultado para la UI
        }
    }

    /**
     * Carga un grafo desde un archivo del sistema de ficheros.
     *
     * @param path ruta al archivo
     * @return resultado de la carga con advertencias
     * @throws IOException si falla la lectura
     */
    public synchronized GraphLoadResult loadFromFile(final Path path) throws IOException { // carga desde archivo del sistema
        final GraphFileParser.Result result = parser.parse(path); // parsea archivo
        applyParsedGraph(result, path); // aplica grafo
        this.dirty = false; // marca limpio
        return buildLoadResult(result, path); // retorna info de carga
    }

    /**
     * Guarda el grafo en el archivo actualmente asociado.
     *
     * @throws IOException si ocurre un error al escribir
     */
    public synchronized void save() throws IOException { // guarda en el archivo actual
        if (currentFile == null) { // si no hay archivo asociado
            throw new IOException("No hay un archivo asociado. Use 'Guardar como…'."); // error
        }
        saveAs(currentFile); // delega a saveAs
    }

    /**
     * Guarda el grafo en la ruta indicada y la asocia como archivo actual.
     *
     * @param path ruta destino
     * @throws IOException si falla la escritura
     */
    public synchronized void saveAs(final Path path) throws IOException { // guarda en la ruta dada
        if (path == null) {
            throw new IllegalArgumentException("path"); // valida

        }
        writer.write(path, graph); // escribe grafo
        this.currentFile = path; // actualiza archivo asociado
        this.dirty = false; // limpia bandera
    }

    /**
     * Reemplaza el grafo en memoria por uno nuevo vacío.
     */
    public synchronized void createNewGraph() { // crea grafo vacío
        this.graph = new DirectedGraph(); // nueva instancia
        this.currentFile = null; // sin archivo asociado
        this.dirty = false; // limpio
        resetSccState(); // borra estado de SCC
    }

    /**
     * Agrega un nuevo usuario al grafo.
     *
     * @param handle handle del usuario a agregar
     * @throws IllegalArgumentException si el handle es inválido o ya existe
     */
    public synchronized void addUser(final String handle) { // agrega un usuario al grafo
        GraphUtils.validateHandle(handle); // valida formato del handle
        if (!graph.addUser(handle)) { // intenta agregar y si ya existe
            throw new IllegalArgumentException("El usuario ya existe: " + handle); // lanza excepción
        }
        markDirty(); // marca cambios pendientes
    }

    /**
     * Elimina un usuario del grafo.
     *
     * @param handle handle del usuario a eliminar
     * @throws IllegalArgumentException si el usuario no existe
     */
    public synchronized void removeUser(final String handle) { // elimina usuario
        GraphUtils.validateHandle(handle); // valida handle
        if (!graph.removeUser(handle)) { // intenta eliminar
            throw new IllegalArgumentException("No existe el usuario: " + handle); // lanza si no existe
        }
        markDirty(); // marca cambio
    }

    /**
     * Añade una relación dirigida entre dos usuarios existentes.
     *
     * @param origin handle del usuario origen
     * @param destination handle del usuario destino
     * @throws IllegalArgumentException si los handles no son válidos o la
     * relación ya existe
     */
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

    /**
     * Elimina una relación dirigida existente.
     *
     * @param origin handle del usuario origen
     * @param destination handle del usuario destino
     * @throws IllegalArgumentException si la relación no existe
     */
    public synchronized void removeRelation(final String origin, final String destination) { // elimina relación
        GraphUtils.validateHandle(origin); // valida
        GraphUtils.validateHandle(destination); // valida
        if (!graph.removeRelation(origin, destination)) { // intenta remover
            throw new IllegalArgumentException("No existe la relación: " + origin + " → " + destination); // error si no existía
        }
        markDirty(); // marca cambios
    }

    /**
     * Calcula los componentes fuertemente conectados usando Kosaraju.
     *
     * @return resultado con las componentes y el mapeo
     */
    public synchronized SccComputationResult computeStronglyConnectedComponents() { // calcula SCCs usando Kosaraju
        final String[][] components = kosaraju.compute(graph); // ejecuta algoritmo (now returns String[][])
        final SccMapping mapping = buildSccMapping(components); // construye mapeo handle->id
        this.lastComponents = components; // guarda resultado
        this.lastSccMapping = mapping; // guarda mapeo
        return new SccComputationResult(components, mapping); // retorna resultado
    }

    /**
     * Devuelve una copia profunda del grafo actual para uso por la UI.
     *
     * @return snapshot del grafo
     */
    public synchronized DirectedGraph getGraphSnapshot() { // obtiene copia del grafo para la UI
        return GraphUtils.copyOf(graph); // retorna copia profunda
    }

    /**
     * Retorna el número de usuarios cargados en el grafo.
     *
     * @return número de usuarios
     */
    public synchronized int getUserCount() { // número de usuarios
        return graph.getUsers().length; // size del arreglo de usuarios
    }

    /**
     * Retorna el número de relaciones (aristas) en el grafo.
     *
     * @return número de relaciones
     */
    public synchronized int getRelationCount() { // número de aristas
        return graph.getEdgeCount(); // delega en DirectedGraph
    }

    /**
     * Devuelve un arreglo de {@link domain.User} que representan los usuarios.
     *
     * @return arreglo de usuarios
     */
    public synchronized User[] getUsers() { // arreglo de usuarios como objetos User (sin java.util)
        final String[] handles = graph.getUsers();
        final User[] users = new User[handles.length];
        for (int i = 0; i < handles.length; i++) {
            users[i] = new User(handles[i]);
        }
        return users;
    }

    /**
     * Devuelve las relaciones actuales como un arreglo de tuplas.
     *
     * @return arreglo de relaciones (from,to)
     */
    public synchronized Relation[] getRelations() { // arreglo de relaciones (tuplas)
        final var view = graph.getAdjacencyView();
        final String[] users = view.users();
        final String[][] neighbors = view.neighbors();
        final int total = graph.getEdgeCount();
        final Relation[] relations = new Relation[total];
        int idx = 0;
        for (int i = 0; i < users.length; i++) {
            final String from = users[i];
            final String[] neigh = neighbors[i];
            for (int j = 0; j < neigh.length; j++) {
                relations[idx++] = new Relation(from, neigh[j]);
            }
        }
        return relations; // retorna arreglo
    }

    /**
     * Obtiene la ruta del archivo actualmente asociado al grafo (puede ser
     * {@code null}).
     *
     * @return ruta del archivo o {@code null}
     */
    public synchronized Path getCurrentFile() { // archivo actualmente asociado (nullable)
        return currentFile; // puede ser null
    }

    /**
     * Indica si existen cambios pendientes de guardado en el servicio.
     *
     * @return {@code true} si hay cambios sin guardar
     */
    public synchronized boolean hasUnsavedChanges() { // indica si hay cambios sin guardar
        return dirty; // retorna flag
    }

    /**
     * Retorna el mapeo de componentes del último cálculo de SCC.
     *
     * @return mapeo handle->componentId
     */
    public synchronized SccMapping getLastSccMapping() { // mapeo de último cálculo SCC
        return lastSccMapping == null ? SccMapping.empty() : lastSccMapping; // retorna mapeo (posible vacío)
    }

    /**
     * Retorna una copia de las componentes del último cálculo de SCC.
     *
     * @return arreglo de componentes (cada componente es un arreglo de handles)
     */
    public synchronized String[][] getLastComponents() { // componentes del último cálculo
        if (lastComponents == null) {
            return new String[0][];
        }
        final String[][] out = new String[lastComponents.length][];
        for (int i = 0; i < lastComponents.length; i++) {
            out[i] = lastComponents[i] == null ? new String[0] : lastComponents[i].clone();
        }
        return out;
    }

    private void applyParsedGraph(final GraphFileParser.Result result, final Path source) { // aplica grafo parseado al estado
        this.graph = GraphUtils.copyOf(result.graph()); // copia profunda
        this.currentFile = source; // actualiza origen
        resetSccState(); // limpia estado de SCC previo
    }

    private GraphLoadResult buildLoadResult(final GraphFileParser.Result result, final Path path) { // construye resultado rico para UI
        final DirectedGraph snapshot = GraphUtils.copyOf(result.graph()); // snapshot inmutable
        return new GraphLoadResult(snapshot,
                result.warnings(),
                path,
                snapshot.getUsers().length,
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
        this.lastSccMapping = SccMapping.empty(); // mapa vacío
        this.lastComponents = new String[0][]; // lista vacía
    }

    private SccMapping buildSccMapping(final String[][] components) { // construye mapeo user->componentId como arrays paralelos
        if (components == null) {
            return SccMapping.empty();
        }
        // contar elementos
        int total = 0;
        for (int i = 0; i < components.length; i++) {
            final String[] comp = components[i];
            if (comp != null) {
                total += comp.length;
            }
        }
        if (total == 0) {
            return SccMapping.empty();
        }
        final String[] handles = new String[total];
        final int[] ids = new int[total];
        int idx = 0;
        for (int i = 0; i < components.length; i++) {
            final String[] comp = components[i];
            if (comp == null) {
                continue;
            }
            for (int j = 0; j < comp.length; j++) {
                handles[idx] = comp[j];
                ids[idx] = i;
                idx++;
            }
        }
        return new SccMapping(handles, ids);
    }

    public record Relation(String from, String to) { // tupla inmutable que representa una relación

    }

    public record GraphLoadResult(DirectedGraph graphSnapshot,
            String[] warnings,
            Path source,
            int userCount,
            int relationCount) { // resultado enriquecido al cargar un grafo

    }

    public record SccComputationResult(String[][] components, SccMapping mapping) { // resultado del cómputo SCC

    }

    public static record SccMapping(String[] handles, int[] componentIds) {

        public static SccMapping empty() {
            return new SccMapping(new String[0], new int[0]);
        }

        public boolean isEmpty() {
            return handles == null || handles.length == 0;
        }

        /**
         * Busca el id de componente asociado a un handle. Retorna -1 si no
         * existe.
         */
        public int findComponentIdFor(final String handle) {
            if (handles == null) {
                return -1;
            }
            for (int i = 0; i < handles.length; i++) {
                if (handles[i].equals(handle)) {
                    return componentIds[i];
                }
            }
            return -1;
        }
    }
}
