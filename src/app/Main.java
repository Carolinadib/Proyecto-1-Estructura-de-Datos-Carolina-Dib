
package app;

import services.GraphService; // servicio que gestiona el grafo y operaciones asociadas
import util.Alerts; // utilidades para mostrar cuadros de diálogo/alertas
import util.UnsavedChangesTracker; // rastreador de cambios no guardados en la UI
import io.GraphFileParser; // parser para leer archivos de grafo
import io.GraphFileWriter; // escritor para guardar archivos de grafo
import ui.MainFrame; // ventana principal de la interfaz gráfica

import javax.swing.SwingUtilities; // utilidades para ejecutar código en el EDT de Swing
import javax.swing.UIManager; // permite configurar el look-and-feel
import javax.swing.UnsupportedLookAndFeelException; // excepción si el L&F no está disponible
import java.io.IOException; // excepción de entrada/salida

public final class Main { // clase de arranque de la aplicación, no instanciable

    private static final String INITIAL_RESOURCE = "initial_data.txt"; // recurso por defecto con datos iniciales

    private Main() { // constructor privado para evitar instancias
        throw new UnsupportedOperationException("Utility class"); // lanza si alguien intenta instanciar
    }

    public static void main(final String[] args) { // punto de entrada de la aplicación
        SwingUtilities.invokeLater(Main::startApplication); // programa el inicio en el hilo de eventos de Swing
    }

    private static void startApplication() { // configura y muestra la UI
        configureLookAndFeel(); // intenta aplicar el L&F nativo del sistema
        final GraphFileParser parser = new GraphFileParser(); // crea el parser de archivos
        final GraphFileWriter writer = new GraphFileWriter(); // crea el escritor de archivos
        final GraphService graphService = new GraphService(parser, writer); // inicializa el servicio del grafo
        final UnsavedChangesTracker changesTracker = new UnsavedChangesTracker(); // rastreador de cambios
        try {
            final GraphService.GraphLoadResult loadResult = graphService.loadInitialGraphFromResource(INITIAL_RESOURCE); // carga el grafo inicial desde recurso
            changesTracker.markClean(); // marca que no hay cambios pendientes
            final MainFrame frame = new MainFrame(graphService, changesTracker); // crea la ventana principal y la inyecta con servicios
            frame.showUi(); // muestra la interfaz al usuario
            if (!loadResult.warnings().isEmpty()) { // si hubo advertencias al cargar
                SwingUtilities.invokeLater(() -> Alerts.warn(frame, "Advertencias", String.join("\n", loadResult.warnings()))); // muestra advertencias en un diálogo
            }
        } catch (IOException ex) { // captura errores de E/S al cargar recurso
            Alerts.error(null, "Error crítico", "No se pudo cargar el grafo inicial: " + ex.getMessage()); // muestra error crítico
        }
    }

    private static void configureLookAndFeel() { // intenta establecer el look and feel del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // aplica L&F nativo
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            // si falla, no hacemos nada y se usa el L&F por defecto de Java
        }
    }
}
