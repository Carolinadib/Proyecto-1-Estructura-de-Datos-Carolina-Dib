/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ui;

import javax.swing.BorderFactory; // fábrica de bordes
import javax.swing.JButton; // botón
import javax.swing.JComboBox; // combo para seleccionar handle
import javax.swing.JDialog; // diálogo modal
import javax.swing.JLabel; // etiqueta
import javax.swing.JPanel; // panel
import javax.swing.SwingUtilities; // utilidades Swing
import javax.swing.WindowConstants; // constantes ventana
import java.awt.BorderLayout; // layout principal
import java.awt.GridLayout; // layout de formulario
import java.awt.event.ActionEvent; // evento acción
import java.util.List; // lista de handles
import java.util.Optional; // valor opcional

public class RemoveUserDialog extends JDialog { // diálogo que permite elegir un usuario a eliminar

    private final JComboBox<String> handleCombo; // combo con handles disponibles
    private String selectedHandle; // handle seleccionado al aceptar

    public RemoveUserDialog(final MainFrame parent, final List<String> handles) { // constructor
        super(parent, "Eliminar usuario", true); // modal
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // disposición
        setResizable(false); // no redimensionable
        setLayout(new BorderLayout()); // layout
        final JPanel form = new JPanel(new GridLayout(1, 2, 8, 8)); // formulario 1x2
        form.add(new JLabel("Usuario:")); // etiqueta
        handleCombo = new JComboBox<>(handles.toArray(String[]::new)); // combo con handles
        form.add(handleCombo); // añade
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12)); // padding
        add(form, BorderLayout.CENTER); // añade form
        add(buildButtons(), BorderLayout.SOUTH); // añade botones
        pack(); // ajusta tamaño
        setLocationRelativeTo(parent); // centra
        SwingUtilities.invokeLater(handleCombo::requestFocusInWindow); // pide foco
    }

    private JPanel buildButtons() { // panel botones
        final JPanel buttons = new JPanel(); // contenedor
        final JButton accept = new JButton("Aceptar"); // aceptar
        accept.addActionListener(this::onAccept); // acción aceptar
        final JButton cancel = new JButton("Cancelar"); // cancelar
        cancel.addActionListener(e -> dispose()); // cierre
        buttons.add(accept); // añade botones
        buttons.add(cancel);
        getRootPane().setDefaultButton(accept); // botón por defecto
        return buttons; // retorna panel
    }

    private void onAccept(final ActionEvent event) { // al aceptar, guarda handle seleccionado
        selectedHandle = (String) handleCombo.getSelectedItem(); // obtiene selección
        dispose(); // cierra diálogo
    }

    public Optional<String> getSelectedHandle() { // devuelve handle seleccionado si existe
        return Optional.ofNullable(selectedHandle); // puede ser vacío
    }
}
