/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ui;

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
import java.util.List; // lista de relaciones
import java.util.Optional; // valor opcional

public class RemoveRelationDialog extends JDialog { // diálogo que permite escoger una relación para eliminar

    private final List<GraphService.Relation> relations; // lista de relaciones disponibles
    private final JComboBox<String> relationCombo; // combo con representaciones texto de relaciones
    private GraphService.Relation selectedRelation; // relación seleccionada al aceptar

    public RemoveRelationDialog(final MainFrame parent, final List<GraphService.Relation> relations) { // constructor
        super(parent, "Eliminar relación", true); // modal
        this.relations = List.copyOf(relations); // copia inmutable
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // disposición
        setResizable(false); // no redimensionable
        setLayout(new BorderLayout()); // layout
        final JPanel form = new JPanel(new GridLayout(1, 2, 8, 8)); // formulario simple
        form.add(new JLabel("Relación:")); // etiqueta
        final String[] entries = this.relations.stream()
                .map(relation -> relation.from() + " \u2192 " + relation.to()) // representa como 'origen → destino'
                .toArray(String[]::new);
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
        if (index >= 0 && index < relations.size()) { // si válido
            selectedRelation = relations.get(index); // obtiene relación correspondiente
        }
        dispose(); // cierra diálogo
    }

    public Optional<GraphService.Relation> getRelation() { // retorna relación seleccionada si existe
        return Optional.ofNullable(selectedRelation); // puede ser vacío
    }
}
