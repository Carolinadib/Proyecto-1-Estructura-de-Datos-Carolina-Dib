package ui; // diálogo para eliminar una relación existente

import services.GraphService; // tipo Relation usado

import javax.swing.BorderFactory; // fábrica de bordes
import javax.swing.JButton; // botón
import javax.swing.JComboBox; // combo box
import javax.swing.JDialog; // diálogo modal
import javax.swing.JLabel; // etiqueta
import javax.swing.JPanel; // panel
import javax.swing.SwingUtilities; // utilidades Swing
import javax.swing.WindowConstants; // constantes ventana
import java.awt.BorderLayout; // layout principal
import java.awt.GridLayout; // layout de formulario
import java.awt.event.ActionEvent; // evento acción

/**
 * Diálogo modal para seleccionar y eliminar una relación existente del grafo.
 * Muestra las relaciones en formato texto y devuelve la relación elegida o
 * {@code null} si el usuario cancela.
 */
public class RemoveRelationDialog extends JDialog { // diálogo que permite escoger una relación para eliminar

    private final GraphService.Relation[] relations; // arreglo de relaciones disponibles
    private final JComboBox<String> relationCombo; // combo con representaciones texto de relaciones
    private GraphService.Relation selectedRelation; // relación seleccionada al aceptar

    public RemoveRelationDialog(final MainFrame parent, final GraphService.Relation[] relations) { // constructor
        super(parent, "Eliminar relación", true); // modal
        this.relations = relations == null ? new GraphService.Relation[0] : relations.clone(); // copia defensiva
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // disposición
        setResizable(false); // no redimensionable
        setLayout(new BorderLayout()); // layout
        final JPanel form = new JPanel(new GridLayout(1, 2, 8, 8)); // formulario simple
        form.add(new JLabel("Relación:")); // etiqueta
        final String[] entries = new String[this.relations.length];
        for (int i = 0; i < this.relations.length; i++) {
            final GraphService.Relation r = this.relations[i];
            entries[i] = r.from() + " \u2192 " + r.to();
        }
        relationCombo = new JComboBox<>(entries); // combo con entradas
        form.add(relationCombo); // añade al formulario
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12)); // padding
        add(form, BorderLayout.CENTER); // añade formulario
        add(buildButtons(), BorderLayout.SOUTH); // añade botones
        pack(); // ajusta tamaño
        setLocationRelativeTo(parent); // centra respecto a parent
        SwingUtilities.invokeLater(relationCombo::requestFocusInWindow); // pide foco en combo
    }

    private JPanel buildButtons() { // panel botones Aceptar/Cancelar
        final JPanel panel = new JPanel(); // contenedor
        final JButton accept = new JButton("Aceptar"); // aceptar
        accept.addActionListener(this::onAccept); // acción aceptar
        final JButton cancel = new JButton("Cancelar"); // cancelar
        cancel.addActionListener(e -> dispose()); // cierra
        panel.add(accept); // añade botones
        panel.add(cancel);
        getRootPane().setDefaultButton(accept); // botón por defecto
        return panel; // retorna panel
    }

    private void onAccept(final ActionEvent event) { // al aceptar, guarda la relación seleccionada
        final int index = relationCombo.getSelectedIndex(); // índice seleccionado
        if (index >= 0 && index < relations.length) { // si válido
            selectedRelation = relations[index]; // obtiene relación correspondiente
        }
        dispose(); // cierra diálogo
    }

    public GraphService.Relation getRelation() { // retorna relación seleccionada o null
        return selectedRelation; // puede ser null
    }
}
