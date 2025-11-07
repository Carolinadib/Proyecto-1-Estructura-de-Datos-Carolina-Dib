
package io; // paquete de IO para grafos

import domain.DirectedGraph; // modelo de grafo dirigido

import java.io.BufferedWriter; // escritor eficiente de texto
import java.io.IOException; // excepción E/S
import java.io.Writer; // interfaz Writer
import java.nio.file.Files; // utilidades de archivos
import java.nio.file.Path; // ruta de archivo

/**
 * Escritor que serializa un {@link domain.DirectedGraph} en el formato de texto
 * requerido (secciones 'usuarios' y 'relaciones').
 */
public class GraphFileWriter { // escribe el grafo en el formato de texto esperado

    private static final String LINE_SEPARATOR = System.lineSeparator(); // separador de líneas del sistema

    public void write(final Path path, final DirectedGraph graph) throws IOException { // escribe a un Path
        if (path == null) {
            throw new IllegalArgumentException("El archivo no puede ser nulo"); // valida parámetros

        }
        if (graph == null) {
            throw new IllegalArgumentException("El grafo no puede ser null");
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path)) { // abre escritor con autocierre
            write(writer, graph); // delega a la versión que usa Writer
        }
    }

    /**
     * Serializa el grafo usando el {@link java.io.Writer} provisto.
     *
     * @param writer escritor donde se volcará el contenido
     * @param graph grafo a serializar
     * @throws IOException si ocurre un error de E/S
     */
    public void write(final Writer writer, final DirectedGraph graph) throws IOException { // escribe usando un Writer
        if (writer == null) {
            throw new IllegalArgumentException("El escritor no puede ser nulo"); // valida

        }
        if (graph == null) {
            throw new IllegalArgumentException("El grafo no puede ser null");
        }
        final BufferedWriter bufferedWriter = writer instanceof BufferedWriter bw ? bw : new BufferedWriter(writer); // asegura BufferedWriter
        bufferedWriter.append("usuarios").append(LINE_SEPARATOR); // escribe marcador usuarios
        for (String user : graph.getUsers()) { // escribe cada usuario en su línea
            bufferedWriter.append(user).append(LINE_SEPARATOR);
        }
        bufferedWriter.append("relaciones").append(LINE_SEPARATOR); // escribe marcador relaciones
        final DirectedGraph.AdjacencyView view = graph.getAdjacencyView(); // snapshot de adyacencia
        final String[] users = view.users();
        final String[][] neighbors = view.neighbors();
        for (int i = 0; i < users.length; i++) {
            final String from = users[i];
            final String[] dests = neighbors[i];
            for (int j = 0; j < dests.length; j++) {
                bufferedWriter.append(from).append(", ").append(dests[j]).append(LINE_SEPARATOR); // escribe 'origen, destino'
            }
        }
        bufferedWriter.flush(); // asegura que todo se escriba
    }
}
