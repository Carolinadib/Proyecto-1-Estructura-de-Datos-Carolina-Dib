package io; // paquete para entrada/salida de grafos

import domain.DirectedGraph; // modelo de grafo dirigido
import domain.GraphUtils; // utilidades para grafos

import java.io.BufferedReader; // lector eficiente de líneas
import java.io.IOException; // excepción de E/S
import java.io.Reader; // interfaz lector
import java.nio.file.Files; // utilidades de archivos
import java.nio.file.Path; // representación de rutas

/**
 * Parser de archivos de grafos sin depender de java.util en la API pública.
 */
public class GraphFileParser { // parsea archivos con formato específico (usuarios/relaciones)

    private static final String SECTION_USERS = "usuarios"; // marcador de sección usuarios
    private static final String SECTION_RELATIONS = "relaciones"; // marcador de sección relaciones

    public Result parse(final Path path) throws IOException { // parsea desde un Path
        if (path == null) {
            throw new IllegalArgumentException("El archivo no puede ser nulo");
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) { // abre lector con autocierre
            return parse(reader, path); // parsea y registra origen
        }
    }

    /**
     * Parsea el contenido desde un {@link java.io.Reader} y construye el grafo.
     *
     * @param reader lector del contenido
     * @return resultado con grafo y advertencias
     * @throws IOException si ocurre un error leyendo
     */
    public Result parse(final Reader reader) throws IOException { // parsea desde un Reader
        if (reader == null) {
            throw new IllegalArgumentException("El lector no puede ser nulo");
        }
        return parse(reader, null); // sin origen físico
    }

    private Result parse(final Reader reader, final Path origin) throws IOException { // núcleo del parseo
        final BufferedReader bufferedReader = reader instanceof BufferedReader br ? br : new BufferedReader(reader); // asegura BufferedReader
        final ParseAccumulator accumulator = new ParseAccumulator(origin); // acumulador del parse
        String line; // variable para leer líneas
        while ((line = bufferedReader.readLine()) != null) { // lee línea por línea
            accumulator.accept(line); // procesa la línea
        }
        return accumulator.buildResult(); // construye resultado final
    }

    private enum Section { // secciones reconocidas en el archivo
        NONE,
        USERS,
        RELATIONS
    }

    private record RelationTuple(String from, String to, boolean skipped) { // tupla para representar una relación parseada

        RelationTuple   {
            if (from == null) {
                throw new IllegalArgumentException("from"); // valida campos no nulos

            }
            if (to == null) {
                throw new IllegalArgumentException("to");
            }
        }
    }

    private static final class ParseAccumulator { // clase interna para acumular estado durante el parseo

        private final Path origin; // origen del parseo (si existe)
        private final StringSet users = new StringSet(); // usuarios declarados, sin duplicados
        private RelationTuple[] relations = new RelationTuple[8]; // relaciones parseadas
        private int relationsSize = 0;
        private String[] warnings = new String[8]; // advertencias encontradas
        private int warningsSize = 0;
        private Section section = Section.NONE; // sección actual
        private boolean usersSectionSeen; // si se vio etiqueta usuarios
        private boolean relationsSectionSeen; // si se vio etiqueta relaciones
        private int lineNumber; // contador de línea para mensajes

        private ParseAccumulator(final Path origin) { // constructor con origen opcional
            this.origin = origin; // guarda origen
        }

        private void accept(final String line) throws IOException { // procesa una línea
            lineNumber++; // incrementa contador de líneas
            final String trimmed = line.trim(); // recorta espacios
            if (trimmed.isEmpty()) { // ignora líneas vacías
                return;
            }
            final Section marker = sectionFromMarker(trimmed); // verifica si la línea es un marcador de sección
            if (marker != null) { // si es marcador
                updateSection(marker); // actualiza sección
                return; // no procesar más
            }
            handleContentLine(trimmed); // maneja línea de contenido según sección
        }

        private void updateSection(final Section marker) { // actualiza estado de sección
            section = marker; // setea sección actual
            if (marker == Section.USERS) { // si es usuarios
                usersSectionSeen = true; // marca vista
            } else if (marker == Section.RELATIONS) { // si es relaciones
                relationsSectionSeen = true; // marca vista
            }
        }

        private Section sectionFromMarker(final String trimmedLine) { // detecta si la línea es marcador de sección
            final String lower = trimmedLine.toLowerCase(); // normaliza a minúsculas (locale-default)
            if (SECTION_USERS.equals(lower)) { // coincide con 'usuarios'
                return Section.USERS; // devuelve enum USERS
            }
            if (SECTION_RELATIONS.equals(lower)) { // coincide con 'relaciones'
                return Section.RELATIONS; // devuelve enum RELATIONS
            }
            return null; // no es marcador
        }

        private void handleContentLine(final String trimmed) throws IOException { // maneja línea según sección actual
            switch (section) {
                case USERS ->
                    handleUserLine(trimmed); // procesa usuario
                case RELATIONS ->
                    handleRelationLine(trimmed); // procesa relación
                case NONE ->
                    addWarning(formatWarning("Línea ignorada antes de declarar la sección 'usuarios': " + trimmed)); // aviso si contenido fuera de secciones
                default ->
                    throw new IllegalStateException("Sección desconocida: " + section); // caso inesperado
            }
        }

        private void handleUserLine(final String trimmed) { // procesa una línea de usuario
            try {
                GraphUtils.validateHandle(trimmed); // valida formato del handle
                if (!users.add(trimmed)) { // intenta añadir; si ya existía
                    addWarning(formatWarning("Usuario duplicado ignorado: " + trimmed)); // registra advertencia
                }
            } catch (IllegalArgumentException ex) { // si handle inválido
                addWarning(formatWarning(ex.getMessage())); // añade advertencia con mensaje
            }
        }

        private void handleRelationLine(final String trimmed) throws IOException { // procesa línea de relación
            final RelationTuple tuple = parseRelation(trimmed); // parsea la relación
            if (!tuple.skipped()) { // si no se marcó para ignorarse
                ensureRelationsCapacity();
                relations[relationsSize++] = tuple; // añade a la lista de relaciones
            }
        }

        private RelationTuple parseRelation(final String line) throws IOException { // parsea una relación del formato 'origen, destino'
            final String[] tokens = line.split(","); // separa por coma
            if (tokens.length != 2) { // formato inválido
                throw new IOException(formatWarning("Relación inválida, use el formato '@origen, @destino': " + line)); // lanza IOException con advertencia formateada
            }
            final String from = tokens[0].trim(); // origen recortado
            final String to = tokens[1].trim(); // destino recortado
            GraphUtils.validateHandle(from); // valida origen
            GraphUtils.validateHandle(to); // valida destino
            if (from.equalsIgnoreCase(to)) { // relación autorefencial
                addWarning(formatWarning("Se ignoró la relación por ser auto-referencial: " + line)); // registra advertencia
                return new RelationTuple(from, to, true); // devuelve tupla marcada como 'skipped'
            }
            return new RelationTuple(from, to, false); // tupla válida
        }

        private Result buildResult() throws IOException { // construye el resultado final después del parseo
            validateSections(); // valida que ambas secciones hayan sido encontradas
            if (users.isEmpty()) { // si no hubo usuarios
                addWarning("No se declararon usuarios en la sección 'usuarios'."); // añade advertencia
            }
            final DirectedGraph graph = new DirectedGraph(); // crea grafo vacío
            final String[] declaredUsers = users.toArray();
            for (int i = 0; i < declaredUsers.length; i++) {
                graph.addUser(declaredUsers[i]); // añade usuarios declarados

            }
            final StringSet autoCreated = new StringSet(); // usuarios que serán creados automáticamente desde relaciones
            for (int r = 0; r < relationsSize; r++) { // procesa relaciones
                final RelationTuple relation = relations[r];
                if (!graph.containsUser(relation.from())) { // si origen no existe
                    autoCreated.add(relation.from()); // añade a autoCreated
                    graph.addUser(relation.from()); // crea usuario
                }
                if (!graph.containsUser(relation.to())) { // si destino no existe
                    autoCreated.add(relation.to()); // añade a autoCreated
                    graph.addUser(relation.to()); // crea usuario
                }
                graph.addRelation(relation.from(), relation.to()); // añade arista al grafo
            }
            final String[] autoCreatedArr = autoCreated.toArray();
            for (int i = 0; i < autoCreatedArr.length; i++) {
                addWarning("Usuario auto-creado desde relaciones: " + autoCreatedArr[i]); // reporta usuarios auto-creados

            }
            return new Result(graph, warningsClone(), autoCreatedArr); // retorna resultado inmutable
        }

        private void validateSections() throws IOException { // valida que ambas secciones existan
            if (!usersSectionSeen || !relationsSectionSeen) { // si falta alguna
                throw new IOException("El archivo debe contener las secciones 'usuarios' y 'relaciones'."); // lanza excepción
            }
        }

        private String formatWarning(final String message) { // formatea un mensaje de advertencia con origen y línea
            if (origin != null) {
                return origin + ":" + lineNumber + ": " + message;
            }
            return "Línea " + lineNumber + ": " + message; // si no hay origen, usa número de línea
        }

        private void addWarning(final String msg) {
            if (warningsSize >= warnings.length) {
                final String[] n = new String[warnings.length * 2];
                for (int i = 0; i < warnings.length; i++) {
                    n[i] = warnings[i];
                }
                warnings = n;
            }
            warnings[warningsSize++] = msg;
        }

        private String[] warningsClone() {
            final String[] out = new String[warningsSize];
            for (int i = 0; i < warningsSize; i++) {
                out[i] = warnings[i];
            }
            return out;
        }

        private void ensureRelationsCapacity() {
            if (relationsSize >= relations.length) {
                final RelationTuple[] n = new RelationTuple[relations.length * 2];
                for (int i = 0; i < relations.length; i++) {
                    n[i] = relations[i];
                }
                relations = n;
            }
        }
    }

    public record Result(DirectedGraph graph, String[] warnings, String[] autoCreated) { // resultado del parseo

    }

    // Implementación mínima de conjunto de Strings que mantiene orden de inserción y evita duplicados.
    private static final class StringSet {

        private String[] data = new String[8];
        private int size = 0;

        boolean add(final String s) {
            if (s == null) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                if (data[i].equals(s)) {
                    return false;
                }
            }
            if (size >= data.length) {
                final String[] n = new String[data.length * 2];
                for (int i = 0; i < data.length; i++) {
                    n[i] = data[i];
                }
                data = n;
            }
            data[size++] = s;
            return true;
        }

        boolean isEmpty() {
            return size == 0;
        }

        String[] toArray() {
            final String[] out = new String[size];
            for (int i = 0; i < size; i++) {
                out[i] = data[i];
            }
            return out;
        }
    }
}
