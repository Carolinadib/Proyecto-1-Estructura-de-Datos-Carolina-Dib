/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ui;

import domain.DirectedGraph; // snapshot del grafo
import services.GraphService; // servicio del grafo
import services.GraphService.GraphLoadResult; // resultado al cargar
import services.GraphService.SccComputationResult; // resultado SCC
import util.Alerts; // utilidades para diálogos
import util.UnsavedChangesTracker; // tracker de cambios no guardados

import javax.swing.Box; // espaciador horizontal
import javax.swing.BoxLayout; // layout para status bar
import javax.swing.JButton; // botón
import javax.swing.JFileChooser; // selector de archivos
import javax.swing.JFrame; // ventana principal
import javax.swing.JLabel; // etiqueta de estado
import javax.swing.JMenu; // menú
import javax.swing.JMenuBar; // barra de menú
import javax.swing.JMenuItem; // item de menú
import javax.swing.JOptionPane; // diálogos estándar
import javax.swing.JPanel; // panel contenedor
import javax.swing.JSeparator; // separador en toolbars
import javax.swing.JToolBar; // toolbar
import javax.swing.SwingConstants; // constantes Swing
import javax.swing.SwingUtilities; // utilidades Swing
import javax.swing.WindowConstants; // constantes de ventana
import javax.swing.filechooser.FileNameExtensionFilter; // filtro para selector de archivos
import java.awt.BorderLayout; // layout principal
import java.awt.Dimension; // dimensiones preferidas
import java.awt.event.WindowAdapter; // escucha cierre ventana
import java.awt.event.WindowEvent; // evento de ventana
import java.io.File; // representación de archivo
import java.io.IOException; // excepción E/S
import java.nio.file.Path; // ruta
import java.util.List; // lista
import java.util.Locale; // locale para minúsculas
import java.util.Map; // mapa
import java.util.Objects; // validaciones

public class MainFrame extends JFrame { // ventana principal que contiene menús, toolbar y panel del grafo

    private static final String TITLE_BASE = "Kosaraju-Nexus"; // título base
    private static final String TITLE_DIRTY_SUFFIX = " *"; // sufijo cuando hay cambios
    private static final String MENU_TEXT_ADD_USER = "Agregar usuario"; // textos de menú reutilizables
    private static final String MENU_TEXT_REMOVE_USER = "Eliminar usuario";
    private static final String MENU_TEXT_ADD_RELATION = "Agregar relación";
    private static final String MENU_TEXT_REMOVE_RELATION = "Eliminar relación";
    private static final String MENU_TEXT_DETECT_SCC = "Detectar SCC (Kosaraju)";
    private static final String STATUS_PREFIX_FILE = "Archivo: "; // prefijos para status bar
    private static final String STATUS_PREFIX_USERS = "Usuarios: ";
    private static final String STATUS_PREFIX_RELATIONS = "Relaciones: ";
    private static final String STATUS_PREFIX_SCC = "SCC: ";
    private static final String DEFAULT_FILE_NAME = "Memoria"; // nombre cuando no hay archivo
    private static final String UNSAVED_FILE_NAME = "Sin título"; // nombre mostrado en diálogos de guardado
    private static final String TXT_EXTENSION = ".txt"; // extensión por defecto
    private static final String WARNINGS_HEADER = "Se detectaron observaciones:"; // encabezado para advertencias al cargar
    private static final String WARNINGS_BULLET = "\u2022 "; // viñeta

    private final GraphService graphService; // servicio inyectado
    private final UnsavedChangesTracker changesTracker; // tracker inyectado
    private final GraphPanel graphPanel; // panel de visualización del grafo

    private JMenuItem saveMenuItem; // referencia a item guardar para habilitar/deshabilitar
    private JMenuItem detectSccMenuItem; // item para detectar SCC

    private JLabel statusFileLabel; // etiqueta estado archivo
    private JLabel statusUsersLabel; // etiqueta estado usuarios
    private JLabel statusRelationsLabel; // etiqueta estado relaciones
    private JLabel statusSccLabel; // etiqueta estado SCC

    public MainFrame(final GraphService graphService, final UnsavedChangesTracker changesTracker) { // constructor
        super(TITLE_BASE); // setea título base
        this.graphService = Objects.requireNonNull(graphService, "graphService"); // valida inyección
        this.changesTracker = Objects.requireNonNull(changesTracker, "changesTracker"); // valida
        this.graphPanel = new GraphPanel(); // crea panel de grafo
        initializeUi(); // inicializa todos los componentes UI
    }

    private void initializeUi() { // construye menú, toolbar, panel central y status bar
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // manejo custom al cerrar
        setLayout(new BorderLayout()); // layout principal
        setPreferredSize(new Dimension(1200, 800)); // tamaño preferido
        addWindowListener(new WindowAdapter() { // escucha cierre de ventana
            @Override
            public void windowClosing(final WindowEvent e) {
                attemptExit(); // intenta salir respetando cambios sin guardar
            }
        });

        setJMenuBar(buildMenuBar()); // menú superior
        add(buildToolBar(), BorderLayout.NORTH); // toolbar arriba
        add(graphPanel, BorderLayout.CENTER); // panel de grafo centro
        add(buildStatusBar(), BorderLayout.SOUTH); // status bar abajo

        changesTracker.addListener(this::onDirtyStateChanged); // escucha cambios externos
        onDirtyStateChanged(changesTracker.hasUnsavedChanges()); // inicializa estado guardado
    }

    private JMenuBar buildMenuBar() { // crea barra de menús
        final JMenuBar menuBar = new JMenuBar(); // instancia
        menuBar.add(buildFileMenu()); // agrega menú Archivo
        menuBar.add(buildEditMenu()); // agrega menú Editar
        menuBar.add(buildViewMenu()); // agrega menú Ver
        menuBar.add(buildHelpMenu()); // agrega menú Ayuda
        return menuBar; // retorna barra
    }

    private JMenu buildFileMenu() { // menú Archivo
        final JMenu menu = new JMenu("Archivo"); // etiqueta del menú

        final JMenuItem newItem = new JMenuItem("Nuevo"); // nuevo archivo
        newItem.addActionListener(e -> handleNew()); // acción nuevo

        final JMenuItem openItem = new JMenuItem("Cargar…"); // cargar archivo
        openItem.addActionListener(e -> handleOpen()); // acción cargar

        saveMenuItem = new JMenuItem("Guardar"); // guardar
        saveMenuItem.addActionListener(e -> handleSave()); // acción guardar
        saveMenuItem.setEnabled(false); // por defecto deshabilitado

        final JMenuItem saveAsItem = new JMenuItem("Guardar como…"); // guardar como
        saveAsItem.addActionListener(e -> handleSaveAs()); // acción guardar como

        final JMenuItem exitItem = new JMenuItem("Salir"); // salir
        exitItem.addActionListener(e -> attemptExit()); // acción salir

        menu.add(newItem); // añade items al menú
        menu.add(openItem);
        menu.add(saveMenuItem);
        menu.add(saveAsItem);
        menu.add(new JSeparator()); // separador
        menu.add(exitItem);
        return menu; // retorna menú construido
    }

    private JMenu buildEditMenu() { // menú Editar con opciones de CRUD
        final JMenu menu = new JMenu("Editar"); // etiqueta Editar

        final JMenuItem addUser = new JMenuItem(MENU_TEXT_ADD_USER); // agregar usuario
        addUser.addActionListener(e -> handleAddUser()); // acción

        final JMenuItem removeUser = new JMenuItem(MENU_TEXT_REMOVE_USER); // eliminar usuario
        removeUser.addActionListener(e -> handleRemoveUser()); // acción

        final JMenuItem addRelation = new JMenuItem(MENU_TEXT_ADD_RELATION); // agregar relación
        addRelation.addActionListener(e -> handleAddRelation()); // acción

        final JMenuItem removeRelation = new JMenuItem(MENU_TEXT_REMOVE_RELATION); // eliminar relación
        removeRelation.addActionListener(e -> handleRemoveRelation()); // acción

        menu.add(addUser); // añade opciones
        menu.add(removeUser);
        menu.add(addRelation);
        menu.add(removeRelation);
        return menu; // retorna menú Editar
    }

    private JMenu buildViewMenu() { // menú Ver con opciones de visualización
        final JMenu menu = new JMenu("Ver"); // etiqueta Ver

        detectSccMenuItem = new JMenuItem(MENU_TEXT_DETECT_SCC); // opción para detectar SCC
        detectSccMenuItem.addActionListener(e -> handleDetectScc()); // acción
        detectSccMenuItem.setEnabled(false); // deshabilitado inicialmente

        final JMenuItem recenterItem = new JMenuItem("Recentrar/Refrescar"); // re-centra vista
        recenterItem.addActionListener(e -> graphPanel.recenter()); // acción recenter

        final JMenuItem layoutItem = new JMenuItem("Layout"); // cambiar layout
        layoutItem.addActionListener(e -> handleLayoutSwitch()); // acción layout

        menu.add(detectSccMenuItem); // añade opciones al menú
        menu.add(recenterItem);
        menu.add(layoutItem);
        return menu; // retorna menú Ver
    }

    private JMenu buildHelpMenu() { // menú Ayuda
        final JMenu menu = new JMenu("Ayuda"); // etiqueta
        final JMenuItem aboutItem = new JMenuItem("Acerca de…"); // acerca de
        aboutItem.addActionListener(e -> new AboutDialog(this).setVisible(true)); // muestra diálogo Acerca de
        menu.add(aboutItem); // añade item
        return menu; // retorna menú Ayuda
    }

    private JToolBar buildToolBar() { // crea la barra de herramientas
        final JToolBar toolBar = new JToolBar(); // instancia
        toolBar.setFloatable(false); // fija la toolbar

        final JButton addUserButton = new JButton("+ Usuario"); // botón agregar usuario
        addUserButton.addActionListener(e -> handleAddUser()); // acción

        final JButton addRelationButton = new JButton("+ Relación"); // botón agregar relación
        addRelationButton.addActionListener(e -> handleAddRelation()); // acción

        final JButton detectSccButton = new JButton("Detectar SCC"); // botón detectar SCC
        detectSccButton.addActionListener(e -> handleDetectScc()); // acción

        toolBar.add(addUserButton); // añade botones a toolbar
        toolBar.add(addRelationButton);
        toolBar.add(new JSeparator(SwingConstants.VERTICAL)); // separador vertical
        toolBar.add(detectSccButton);
        return toolBar; // retorna toolbar
    }

    private JPanel buildStatusBar() { // construye la barra de estado inferior
        final JPanel statusBar = new JPanel(); // panel contenedor
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS)); // layout horizontal

        statusFileLabel = new JLabel(STATUS_PREFIX_FILE + DEFAULT_FILE_NAME); // etiqueta archivo
        statusUsersLabel = new JLabel(STATUS_PREFIX_USERS + 0); // etiqueta usuarios
        statusRelationsLabel = new JLabel(STATUS_PREFIX_RELATIONS + 0); // etiqueta relaciones
        statusSccLabel = new JLabel(STATUS_PREFIX_SCC + 0); // etiqueta SCC

        statusBar.add(Box.createHorizontalStrut(12)); // espacio
        statusBar.add(statusFileLabel); // añade etiquetas al status bar
        statusBar.add(Box.createHorizontalStrut(24));
        statusBar.add(statusUsersLabel);
        statusBar.add(Box.createHorizontalStrut(24));
        statusBar.add(statusRelationsLabel);
        statusBar.add(Box.createHorizontalStrut(24));
        statusBar.add(statusSccLabel);
        statusBar.add(Box.createHorizontalGlue()); // empuja contenido a la izquierda
        return statusBar; // retorna panel de estado
    }

    public void refreshGraph() { // refresca la vista del grafo usando snapshot del servicio
        final DirectedGraph snapshot = graphService.getGraphSnapshot(); // obtiene copia del grafo
        final Map<String, Integer> mapping = graphService.getLastSccMapping(); // mapeo SCC si existe
        graphPanel.renderGraph(snapshot, mapping.isEmpty() ? null : mapping); // renderiza (pasa null si vacío)
        updateStatusBar(); // actualiza información en la barra de estado
    }

    private void handleNew() { // crea un grafo nuevo, pidiendo confirmar cambios si es necesario
        if (!ensureChangesSaved()) { // si el usuario cancela
            return; // aborta
        }
        graphService.createNewGraph(); // crea grafo vacío
        changesTracker.markClean(); // marca limpio
        refreshGraph(); // refresca vista
    }

    private void handleOpen() { // abre un archivo desde el sistema
        if (!ensureChangesSaved()) { // confirma cambios pendientes
            return; // aborta si cancela
        }
        final JFileChooser fileChooser = createFileChooser(); // crea selector
        final int choice = fileChooser.showOpenDialog(this); // muestra diálogo abrir
        if (choice == JFileChooser.APPROVE_OPTION) { // si eligió archivo
            final File file = fileChooser.getSelectedFile(); // obtiene selección
            try {
                final GraphLoadResult result = graphService.loadFromFile(file.toPath()); // carga archivo
                deliverWarnings(result.warnings()); // muestra advertencias si las hay
                changesTracker.markClean(); // marca limpio
                refreshGraph(); // refresca
            } catch (IOException ex) { // error al cargar
                Alerts.error(this, "Error al cargar", ex.getMessage()); // muestra diálogo de error
            }
        }
    }

    private void handleSave() { // guarda en el archivo asociado o pide ruta si no existe
        if (graphService.getCurrentFile().isEmpty()) { // si no hay archivo asociado
            handleSaveAs(); // invoca guardar como
            return; // y sale
        }
        try {
            graphService.save(); // guarda en archivo actual
            changesTracker.markClean(); // marca limpio
            refreshGraph(); // refresca
        } catch (IOException ex) { // error al guardar
            Alerts.error(this, "Error al guardar", ex.getMessage()); // muestra error
        }
    }

    private void handleSaveAs() { // guarda el grafo en una ruta elegida por el usuario
        final JFileChooser chooser = createFileChooser(); // crea selector
        final int choice = chooser.showSaveDialog(this); // muestra diálogo guardar
        if (choice == JFileChooser.APPROVE_OPTION) { // si aprobó
            File file = chooser.getSelectedFile(); // obtiene archivo seleccionado
            file = ensureTxtExtension(file); // asegura extensión .txt si hace falta
            if (file.exists()) { // si el archivo ya existe
                final int overwrite = JOptionPane.showConfirmDialog(this,
                        "El archivo ya existe. ¿Desea sobrescribirlo?",
                        "Confirmar guardado",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE); // confirma sobrescritura
                if (overwrite != JOptionPane.YES_OPTION) { // si no aceptó sobrescribir
                    return; // aborta
                }
            }
            try {
                graphService.saveAs(file.toPath()); // guarda en la ruta elegida
                changesTracker.markClean(); // marca limpio
                refreshGraph(); // refresca
            } catch (IOException ex) { // error al guardar
                Alerts.error(this, "Error al guardar", ex.getMessage()); // muestra error
            }
        }
    }

    private void handleAddUser() { // flujo para agregar un usuario (diálogo + servicio)
        final AddUserDialog dialog = new AddUserDialog(this); // crea diálogo
        dialog.setVisible(true); // muestra modalmente
        dialog.getUserHandle().ifPresent(handle -> { // si el usuario ingresó un handle
            try {
                graphService.addUser(handle); // agrega en servicio
                changesTracker.markDirty(); // marca cambios
                refreshGraph(); // refresca UI
            } catch (IllegalArgumentException ex) { // si handle no válido o existe
                Alerts.warn(this, MENU_TEXT_ADD_USER, ex.getMessage()); // muestra advertencia
            }
        });
    }

    private void handleRemoveUser() { // flujo para eliminar un usuario
        final List<String> handles = graphService.getUsers().stream().map(user -> user.getHandle()).toList(); // obtiene lista de handles
        if (handles.isEmpty()) { // si no hay usuarios
            Alerts.warn(this, MENU_TEXT_REMOVE_USER, "No hay usuarios para eliminar."); // muestra advertencia
            return; // nada que hacer
        }
        final RemoveUserDialog dialog = new RemoveUserDialog(this, handles); // muestra diálogo de selección
        dialog.setVisible(true); // modal
        dialog.getSelectedHandle().ifPresent(handle -> { // si seleccionó uno
            if (!Alerts.confirmDeletion(this, "¿Eliminar usuario " + handle + " y sus relaciones?")) { // confirma eliminación
                return; // si no confirma, aborta
            }
            try {
                graphService.removeUser(handle); // elimina en servicio
                changesTracker.markDirty(); // marca cambios
                refreshGraph(); // refresca
            } catch (IllegalArgumentException ex) { // error al eliminar
                Alerts.warn(this, MENU_TEXT_REMOVE_USER, ex.getMessage()); // muestra advertencia
            }
        });
    }

    private void handleAddRelation() { // flujo para agregar relación
        final List<String> handles = graphService.getUsers().stream().map(user -> user.getHandle()).toList(); // lista de handles
        if (handles.size() < 2) { // requiere al menos dos usuarios
            Alerts.warn(this, MENU_TEXT_ADD_RELATION, "Se requieren al menos dos usuarios."); // advierte
            return; // aborta
        }
        final AddRelationDialog dialog = new AddRelationDialog(this, handles); // diálogo selección origen/destino
        dialog.setVisible(true); // muestra
        dialog.getRelation().ifPresent(relation -> { // si hizo selección
            try {
                graphService.addRelation(relation.origin(), relation.destination()); // agrega relación
                changesTracker.markDirty(); // marca cambios
                refreshGraph(); // refresca
            } catch (IllegalArgumentException ex) { // si no se pudo agregar
                Alerts.warn(this, MENU_TEXT_ADD_RELATION, ex.getMessage()); // muestra advertencia
            }
        });
    }

    private void handleRemoveRelation() { // flujo para eliminar relación
        final var relations = graphService.getRelations(); // obtiene lista de relaciones
        if (relations.isEmpty()) { // si no hay relaciones
            Alerts.warn(this, MENU_TEXT_REMOVE_RELATION, "No hay relaciones que eliminar."); // advierte
            return; // aborta
        }
        final RemoveRelationDialog dialog = new RemoveRelationDialog(this, relations); // diálogo selección de relación
        dialog.setVisible(true); // muestra
        dialog.getRelation().ifPresent(relation -> { // si seleccionó
            if (!Alerts.confirmDeletion(this, "¿Eliminar relación " + relation.from() + " → " + relation.to() + "?")) { // confirma
                return; // aborta si no
            }
            try {
                graphService.removeRelation(relation.from(), relation.to()); // elimina
                changesTracker.markDirty(); // marca cambios
                refreshGraph(); // refresca
            } catch (IllegalArgumentException ex) { // error al eliminar
                Alerts.warn(this, MENU_TEXT_REMOVE_RELATION, ex.getMessage()); // muestra advertencia
            }
        });
    }

    private void handleDetectScc() { // ejecuta detección de SCCs y pinta resultados
        if (graphService.getUserCount() == 0) { // requiere al menos un usuario
            Alerts.warn(this, MENU_TEXT_DETECT_SCC, "No hay usuarios cargados en el grafo."); // advierte
            return; // aborta
        }
        final SccComputationResult result = graphService.computeStronglyConnectedComponents(); // computa SCCs
        graphPanel.renderGraph(graphService.getGraphSnapshot(), result.mapping()); // renderiza con mapeo
        updateStatusBar(); // actualiza barra estado
    }

    private void handleLayoutSwitch() { // permite seleccionar layout de la vista
        final Object[] options = {"Spring", "Desactivar"}; // opciones del diálogo
        final int choice = JOptionPane.showOptionDialog(this,
                "Seleccione un layout",
                "Layout",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]); // muestra diálogo de opciones
        if (choice == 0) { // Spring
            graphPanel.applyLayout(GraphPanel.LayoutType.SPRING); // activa layout dinámico
        } else if (choice == 1) { // Desactivar
            graphPanel.applyLayout(GraphPanel.LayoutType.NO_LAYOUT); // desactiva layout automático
        }
    }

    private void deliverWarnings(final List<String> warnings) { // muestra advertencias en bloque
        if (warnings.isEmpty()) { // si no hay nada
            return; // sale
        }
        final String lineSeparator = System.lineSeparator(); // separador de líneas
        final StringBuilder message = new StringBuilder(WARNINGS_HEADER).append(lineSeparator); // construye mensaje
        warnings.forEach(warning -> message.append(WARNINGS_BULLET).append(warning).append(lineSeparator)); // añade viñetas
        Alerts.warn(this, "Advertencias", message.toString()); // muestra diálogo de advertencias
    }

    private JFileChooser createFileChooser() { // crea un JFileChooser preconfigurado
        final JFileChooser chooser = new JFileChooser(); // instancia
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos de texto", "txt")); // filtra .txt
        chooser.setAcceptAllFileFilterUsed(false); // no permitir otros tipos
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY); // solo archivos
        graphService.getCurrentFile().map(Path::toFile).ifPresent(chooser::setSelectedFile); // si hay archivo asociado, selecciónalo
        return chooser; // retorna chooser
    }

    private File ensureTxtExtension(final File file) { // asegura extensión .txt al guardar
        final String lowerName = file.getName().toLowerCase(Locale.ROOT); // nombre en minúsculas
        if (lowerName.endsWith(TXT_EXTENSION)) { // si ya tiene .txt
            return file; // retorna tal cual
        }
        final File parent = file.getParentFile(); // obtiene carpeta padre
        final String newName = file.getName() + TXT_EXTENSION; // añade sufijo .txt
        return parent == null ? new File(newName) : new File(parent, newName); // construye nuevo File
    }

    private void updateStatusBar() { // actualiza etiquetas informativas en la barra de estado
        final String fileName = graphService.getCurrentFile()
                .map(Path::getFileName)
                .map(Path::toString)
                .orElse(DEFAULT_FILE_NAME); // obtiene nombre del archivo o 'Memoria'
        statusFileLabel.setText(STATUS_PREFIX_FILE + fileName); // actualiza etiqueta archivo
        statusUsersLabel.setText(STATUS_PREFIX_USERS + graphService.getUserCount()); // actualiza count usuarios
        statusRelationsLabel.setText(STATUS_PREFIX_RELATIONS + graphService.getRelationCount()); // actualiza count relaciones
        final int sccCount = graphService.getLastComponents().size(); // obtiene número de SCCs detectadas
        statusSccLabel.setText(STATUS_PREFIX_SCC + sccCount); // actualiza etiqueta SCC
        if (detectSccMenuItem != null) { // habilita opción detectar SCC si hay usuarios
            detectSccMenuItem.setEnabled(graphService.getUserCount() > 0);
        }
    }

    private void onDirtyStateChanged(final boolean dirty) { // callback cuando cambia estado 'dirty'
        if (saveMenuItem != null) { // habilita/deshabilita item Guardar según estado
            saveMenuItem.setEnabled(dirty);
        }
        setTitle(dirty ? TITLE_BASE + TITLE_DIRTY_SUFFIX : TITLE_BASE); // actualiza título con sufijo si hay cambios
    }

    private boolean ensureChangesSaved() { // muestra diálogo para guardar/descartar/cancelar cambios
        if (!graphService.hasUnsavedChanges() && !changesTracker.hasUnsavedChanges()) { // si no hay cambios en ambos
            return true; // no hay nada que preguntar
        }
        final String fileName = graphService.getCurrentFile()
                .map(Path::getFileName)
                .map(Path::toString)
                .orElse(UNSAVED_FILE_NAME); // nombre para mostrar en diálogo
        final int choice = Alerts.confirmSaveDiscardOrCancel(this, fileName); // muestra diálogo personalizado
        if (choice == JOptionPane.CANCEL_OPTION) { // si canceló
            return false; // aborta operación solicitada
        }
        if (choice == JOptionPane.YES_OPTION) { // si eligió guardar
            handleSave(); // intenta guardar
            return !graphService.hasUnsavedChanges(); // retorna true si ya no hay cambios
        }
        return true; // si eligió 'No guardar', continúa
    }

    private void attemptExit() { // intenta cerrar la aplicación respetando cambios
        if (!ensureChangesSaved()) { // confirma cambios
            return; // aborta si canceló
        }
        dispose(); // destruye ventana
        System.exit(0); // finaliza JVM
    }

    public void showUi() { // muestra la UI en el hilo de eventos Swing
        SwingUtilities.invokeLater(() -> {
            pack(); // ajusta tamaño según componentes
            setLocationRelativeTo(null); // centra en pantalla
            setVisible(true); // muestra ventana
            refreshGraph(); // refresca vista del grafo
        });
    }
}

