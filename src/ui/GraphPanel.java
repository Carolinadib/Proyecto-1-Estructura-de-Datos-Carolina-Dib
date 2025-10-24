/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ui;

import domain.DirectedGraph; // snapshot del grafo
import util.ColorPalette; // paleta de colores para componentes

import org.graphstream.graph.Graph; // interfaz de GraphStream
import org.graphstream.graph.Node; // interfaz de nodos
import org.graphstream.graph.implementations.SingleGraph; // implementación simple de grafo
import org.graphstream.ui.layout.springbox.implementations.SpringBox; // layout tipo spring
import org.graphstream.ui.view.View; // vista genérica
import org.graphstream.ui.view.Viewer; // viewer de GraphStream
import org.graphstream.ui.swing_viewer.SwingViewer; // viewer basado en Swing
import org.graphstream.ui.swing_viewer.ViewPanel; // panel Swing que muestra la vista
import org.graphstream.ui.view.camera.Camera; // cámara para manipular vista

import javax.swing.JPanel; // contenedor Swing
import java.awt.BorderLayout; // layout principal
import java.util.LinkedHashSet; // set con orden
import java.util.List; // lista
import java.util.Map; // mapa
import java.util.Objects; // validaciones
import java.util.Set; // set
import java.util.concurrent.atomic.AtomicInteger; // contador atómico


public class GraphPanel extends JPanel { // panel que encapsula la vista del grafo

    private static final String UI_CLASS = "ui.class"; // atributo para clase CSS del nodo
    private static final String UI_STYLESHEET = "ui.stylesheet"; // atributo para hojas de estilo

    private final Graph graph; // grafo subyacente de GraphStream
    private final Viewer viewer; // viewer que maneja la visualización
    private final ViewPanel viewPanel; // panel Swing que contiene la vista
    private Set<String> lastUsers = new LinkedHashSet<>(); // usuarios la última vez renderizados
    private int lastEdgeCount; // número de aristas la última vez

    /**
     * Supported layout algorithms.
     */
    public enum LayoutType { // tipos de layout soportados
        SPRING,
        NO_LAYOUT
    }

    /**
     * Creates the panel and initializes the GraphStream components.
     */
    public GraphPanel() { // constructor: inicializa GraphStream y componentes Swing
        super(new BorderLayout()); // usa BorderLayout
        // Configurar el sistema para usar Swing
        System.setProperty("org.graphstream.ui", "swing"); // fuerza implementación Swing

        this.graph = new SingleGraph("KosarajuNexus"); // crea instancia de grafo GraphStream
        graph.setAttribute(UI_STYLESHEET, ColorPalette.buildStylesheet(0)); // aplica stylesheet inicial (sin componentes)
        graph.setAttribute("ui.quality"); // mejora calidad visual
        graph.setAttribute("ui.antialias"); // activa antialiasing

        // Crear el viewer usando SwingViewer
        this.viewer = new SwingViewer(graph, SwingViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD); // viewer en hilo separado
        this.viewPanel = (ViewPanel) viewer.addDefaultView(false); // crea la vista y la obtiene como ViewPanel

        // No usar layout automático por defecto: usaremos un layout 2D estático (plano)
        viewer.disableAutoLayout(); // desactiva layout automático por defecto

        add(viewPanel, BorderLayout.CENTER); // añade panel de vista al centro
    }

    /**
     * Updates the visualization to reflect the supplied graph snapshot.
     *
     * @param snapshot graph snapshot
     * @param sccMapping optional mapping from handle to SCC id (may be null)
     */
    public void renderGraph(final DirectedGraph snapshot, final Map<String, Integer> sccMapping) { // renderiza un snapshot del grafo
        Objects.requireNonNull(snapshot, "snapshot"); // valida
        final Map<String, List<String>> adjacency = snapshot.getAdjacencyView(); // obtiene adyacencia inmutable
        final Set<String> users = new LinkedHashSet<>(adjacency.keySet()); // conjunto de usuarios actual
        final int edgeCount = snapshot.getEdgeCount(); // número de aristas
        final Map<String, Integer> mapping = (sccMapping == null || sccMapping.isEmpty()) ? null : sccMapping; // mapeo opcional
        final int componentCount = mapping == null ? 0 : (int) mapping.values().stream().distinct().count(); // cantidad de componentes detectadas
        final String stylesheet = ColorPalette.buildStylesheet(componentCount); // construye stylesheet según colores necesarios
        graph.setAttribute(UI_STYLESHEET, stylesheet); // aplica stylesheet

        if (canReuseStructure(users, edgeCount, mapping)) { // intenta reutilizar estructura si no cambió topología
            recolorNodes(users, mapping); // solo recolorea nodos
        } else {
            rebuildGraph(adjacency, mapping, stylesheet); // reconstruye todo el grafo en la vista
        }
        lastUsers = new LinkedHashSet<>(users); // guarda estado para próxima renderización
        lastEdgeCount = edgeCount; // guarda conteo de aristas
    }

    private boolean canReuseStructure(final Set<String> users, final int edgeCount, final Map<String, Integer> mapping) { // decide si se puede reutilizar la estructura existente
        if (mapping == null) { // si no hay mapeo SCC
            return false; // no reutilizar (deseamos reconstruir para aplicar estilo por componentes)
        }
        return !lastUsers.isEmpty() && lastUsers.equals(users) && lastEdgeCount == edgeCount; // solo reutiliza si usuarios y aristas no cambiaron
    }

    private void rebuildGraph(final Map<String, List<String>> adjacency,
            final Map<String, Integer> mapping,
            final String stylesheet) { // limpia y reconstruye la estructura visual
        graph.clear(); // borra nodos y aristas actuales
        graph.setAttribute(UI_STYLESHEET, stylesheet); // aplica stylesheet actualizado
        adjacency.keySet().forEach(user -> addOrUpdateNode(user, mapping)); // añade/actualiza nodos
        final AtomicInteger edgeCounter = new AtomicInteger(); // contador para ids de aristas
        adjacency.forEach((from, neighbors) -> neighbors.forEach(to -> {
            final String edgeId = "e" + edgeCounter.incrementAndGet() + ":" + from + "->" + to; // id único por arista
            graph.addEdge(edgeId, from, to, true); // crea arista dirigida
        }));
        // Aplicar un layout 2D estático y plano (sin movimiento). Usamos un layout circular simple.
        applyStaticLayout(adjacency.keySet()); // asigna posiciones fijas a nodos
    }

    private void recolorNodes(final Set<String> users, final Map<String, Integer> mapping) { // actualiza etiquetas y clases CSS de nodos existentes
        for (String user : users) { // itera usuarios
            final Node node = graph.getNode(user); // obtiene nodo por id (handle)
            if (node != null) { // si existe
                node.setAttribute("ui.label", user); // actualiza etiqueta visible
                applyNodeClass(node, mapping); // aplica clase CSS según componente
            }
        }
    }

    private void addOrUpdateNode(final String handle, final Map<String, Integer> mapping) { // añade un nodo si no existe o lo actualiza
        Node node = graph.getNode(handle); // intenta obtener nodo
        if (node == null) { // si no existe
            node = graph.addNode(handle); // lo crea
        }
        node.setAttribute("ui.label", handle); // etiqueta con el handle
        // Marcar el nodo como no interactivo / fijo para evitar arrastre por ratón
        node.setAttribute("ui.lock", true); // atributo usado por GraphStream
        node.setAttribute("locked", true); // atributo adicional para asegurar bloqueo
        applyNodeClass(node, mapping); // aplica clase CSS
    }

    private void applyNodeClass(final Node node, final Map<String, Integer> mapping) { // asigna clase CSS al nodo según su SCC
        if (mapping != null) { // si hay mapeo
            final Integer componentId = mapping.get(node.getId()); // obtiene id de componente para este nodo
            if (componentId != null) { // si pertenece a una componente
                node.setAttribute(UI_CLASS, "scc-" + componentId); // asigna clase scc-N
                return; // fin
            }
        }
        node.setAttribute(UI_CLASS, "default"); // clase por defecto
    }

    /**
     * Asigna posiciones 2D fijas a los nodos usando un layout circular simple.
     * Las coordenadas se asignan en el atributo "xy" para que la vista muestre
     * un grafo plano y sin animaciones.
     */
    private void applyStaticLayout(final Set<String> nodes) { // asigna posiciones en círculo
        if (nodes == null || nodes.isEmpty()) { // nada que hacer
            return;
        }
        final int n = nodes.size(); // número de nodos
        final double radius = Math.max(1.0, n / 2.0); // radio escalado según cantidad de nodos
        int i = 0; // índice para distribuir ángulos
        for (String id : nodes) { // itera nodos
            final Node node = graph.getNode(id); // obtiene nodo
            if (node != null) { // si existe
                double angle = 2.0 * Math.PI * i / n; // ángulo para posición circular
                double x = Math.cos(angle) * radius; // coordenada x
                double y = Math.sin(angle) * radius; // coordenada y
                // Atributo 'xy' reconocido por GraphStream para posiciones 2D
                node.setAttribute("xy", x, y); // asigna posición
                // Asegurarse que la posición esté bloqueada para que la vista no permita moverla
                node.setAttribute("ui.lock", true); // bloqueo UI
                node.setAttribute("locked", true); // bloqueo adicional
            }
            i++; // incrementa índice
        }
    }

    /**
     * Recenters the camera resetting zoom and translation.
     */
    public void recenter() { // centra la cámara en la vista
        final Camera camera = viewPanel.getCamera(); // obtiene cámara de la vista
        camera.resetView(); // resetea zoom/traducción
    }

    /**
     * Applies the selected layout algorithm.
     *
     * @param layoutType desired layout
     */
    public void applyLayout(final LayoutType layoutType) { // permite aplicar layouts soportados
        Objects.requireNonNull(layoutType, "layoutType"); // valida
        switch (layoutType) {
            case SPRING -> { // layout tipo resorte (dinámico)
                SpringBox springBox = new SpringBox(); // instancia SpringBox
                viewer.enableAutoLayout(springBox); // habilita layout automático con SpringBox
            }
            case NO_LAYOUT ->
                viewer.disableAutoLayout(); // desactiva layout automático
            default ->
                throw new IllegalArgumentException("Layout no soportado: " + layoutType); // caso no esperado
        }
    }

    /**
     * @return underlying GraphStream {@link Viewer}
     */
    public Viewer getViewer() { // expone viewer subyacente
        return viewer; // retorna viewer
    }

    /**
     * @return underlying GraphStream {@link ViewPanel}
     */
    public ViewPanel getViewPanel() { // expone panel de vista
        return viewPanel; // retorna viewPanel
    }
}

