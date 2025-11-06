
package io;

import domain.DirectedGraph; // modelo de grafo dirigido

import java.io.BufferedWriter; // escritor eficiente de texto
import java.io.IOException; // excepción E/S
import java.io.Writer; // interfaz Writer
import java.nio.file.Files; // utilidades de archivos
import java.nio.file.Path; // ruta de archivo
import java.util.List; // listas
import java.util.Map; // mapas
import java.util.Objects; // validaciones

public class GraphFileWriter { // escribe el grafo en el formato de texto esperado

    private static final String LINE_SEPARATOR = System.lineSeparator(); // separador de líneas del sistema

    public void write(final Path path, final DirectedGraph graph) throws IOException { // escribe a un Path
        Objects.requireNonNull(path, "El archivo no puede ser nulo"); // valida parámetros
        Objects.requireNonNull(graph, "El grafo no puede ser null");
        try (BufferedWriter writer = Files.newBufferedWriter(path)) { // abre escritor con autocierre
            write(writer, graph); // delega a la versión que usa Writer
        }
    }

    public void write(final Writer writer, final DirectedGraph graph) throws IOException { // escribe usando un Writer
        Objects.requireNonNull(writer, "El escritor no puede ser nulo"); // valida
        Objects.requireNonNull(graph, "El grafo no puede ser null");
        final BufferedWriter bufferedWriter = writer instanceof BufferedWriter bw ? bw : new BufferedWriter(writer); // asegura BufferedWriter
        bufferedWriter.append("usuarios").append(LINE_SEPARATOR); // escribe marcador usuarios
        for (String user : graph.getUsers()) { // escribe cada usuario en su línea
            bufferedWriter.append(user).append(LINE_SEPARATOR);
        }
        bufferedWriter.append("relaciones").append(LINE_SEPARATOR); // escribe marcador relaciones
        final Map<String, List<String>> adjacency = graph.getAdjacencyView(); // snapshot de adyacencia
        for (Map.Entry<String, List<String>> entry : adjacency.entrySet()) { // itera entradas
            final String from = entry.getKey(); // origen
            for (String to : entry.getValue()) { // para cada destino
                bufferedWriter.append(from).append(", ").append(to).append(LINE_SEPARATOR); // escribe 'origen, destino'
            }
        }
        bufferedWriter.flush(); // asegura que todo se escriba
    }
}
