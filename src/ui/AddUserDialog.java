package ui; // diálogo para agregar un usuario

import domain.GraphUtils; // utilidades del dominio para validar handles

import javax.swing.BorderFactory; // fábrica de bordes
import javax.swing.JButton; // botón
import javax.swing.JDialog; // diálogo modal
import javax.swing.JLabel; // etiqueta
import javax.swing.JPanel; // panel contenedor
import javax.swing.JTextField; // campo de texto
import javax.swing.SwingUtilities; // utilidades Swing
import javax.swing.WindowConstants; // constantes de ventana
import java.awt.BorderLayout; // layout principal
import java.awt.GridLayout; // layout de formulario
import java.awt.event.ActionEvent; // evento de acción

/**
 * Diálogo modal para solicitar al usuario el handle de un nuevo usuario que
 * será agregado al grafo. Retorna el handle ingresado o {@code null} si el
 * usuario cancela.
 */
public class AddUserDialog extends JDialog { // diálogo que solicita un nuevo handle

    private static final int FIELD_COLUMNS = 20; // ancho en columnas del campo

    private final JTextField handleField; // campo para ingresar el handle
    private String userHandle; // resultado si se aceptó

    /**
     * Crea y configura el diálogo para ingresar un nuevo handle.
     *
     * @param parent ventana padre para centrar el diálogo
     */
    public AddUserDialog(final MainFrame parent) { // constructor
        super(parent, "Agregar usuario", true); // modal
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // disposición
        setResizable(false); // no redimensionable
        setLayout(new BorderLayout()); // layout principal
        final JPanel form = new JPanel(new GridLayout(2, 1)); // formulario con dos filas
        form.add(new JLabel("Handle (ej. @usuario):")); // etiqueta instructiva
        handleField = new JTextField("@", FIELD_COLUMNS); // campo inicializado con '@'
        handleField.setToolTipText("Debe comenzar con '@' y ser único"); // tooltip
        form.add(handleField); // añade campo al formulario
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12)); // padding
        add(form, BorderLayout.CENTER); // añade formulario al centro
        add(buildButtons(), BorderLayout.SOUTH); // añade botones abajo
        pack(); // ajusta tamaño
        setLocationRelativeTo(parent); // centra respecto a parent
        SwingUtilities.invokeLater(handleField::requestFocusInWindow); // solicita foco en el campo
    }

    private JPanel buildButtons() { // crea panel de botones Aceptar/Cancelar
        final JPanel panel = new JPanel(); // contenedor
        final JButton accept = new JButton("Aceptar"); // botón aceptar
        accept.addActionListener(this::onAccept); // acción aceptar
        final JButton cancel = new JButton("Cancelar"); // botón cancelar
        cancel.addActionListener(e -> dispose()); // cierra al cancelar
        panel.add(accept); // añade botones
        panel.add(cancel);
        getRootPane().setDefaultButton(accept); // botón por defecto
        return panel; // retorna panel
    }

    private void onAccept(final ActionEvent event) { // validación al aceptar
        final String handle = handleField.getText().trim(); // obtiene texto recortado
        try {
            GraphUtils.validateHandle(handle); // valida formato
            this.userHandle = handle; // guarda resultado
            dispose(); // cierra diálogo
        } catch (IllegalArgumentException ex) { // si inválido
            handleField.setToolTipText(ex.getMessage()); // muestra mensaje como tooltip
            handleField.requestFocusInWindow(); // vuelve a pedir foco
        }
    }

    /**
     * Devuelve el handle ingresado por el usuario o {@code null} si el diálogo
     * fue cancelado.
     *
     * @return handle ingresado o {@code null}
     */
    public String getUserHandle() { // retorna handle ingresado si existe (nullable)
        return userHandle;
    }
}
