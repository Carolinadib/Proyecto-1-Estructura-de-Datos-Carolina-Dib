/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ui;

import javax.swing.BorderFactory; // fábrica de bordes
import javax.swing.JButton; // botón
import javax.swing.JComboBox; // combo box para selección
import javax.swing.JDialog; // diálogo modal
import javax.swing.JLabel; // etiqueta
import javax.swing.JPanel; // panel contenedor
import javax.swing.SwingUtilities; // utilidades Swing
import javax.swing.WindowConstants; // opciones de cierre
import java.awt.BorderLayout; // layout principal
import java.awt.GridLayout; // layout de formulario
import java.awt.event.ActionEvent; // evento de acción
import java.awt.event.ItemEvent; // evento de cambio de ítem
import java.util.ArrayList; // lista temporal
import java.util.List; // interfaz lista
import java.util.Optional; // valor opcional

public class AddRelationDialog extends JDialog { // diálogo para crear una relación entre dos usuarios

    private final JComboBox<String> originCombo; // combo para origen
    private final JComboBox<String> destinationCombo; // combo para destino
    private final List<String> handles; // lista de handles disponibles
    private String origin; // handle seleccionado como origen
    private String destination; // handle seleccionado como destino

    public AddRelationDialog(final MainFrame parent, final List<String> handles) { // constructor con lista de handles
        super(parent, "Agregar relación", true); // modal
        this.handles = List.copyOf(handles); // copia inmutable de handles
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // disposición
        setResizable(false); // no redimensionable
        setLayout(new BorderLayout(12, 12)); // padding entre componentes
        final JPanel form = new JPanel(new GridLayout(2, 2, 8, 8)); // formulario simple 2x2
        form.add(new JLabel("Origen:")); // etiqueta origen
        originCombo = new JComboBox<>(this.handles.toArray(String[]::new)); // crea combo con handles
        form.add(originCombo); // añade al formulario
        form.add(new JLabel("Destino:")); // etiqueta destino
        destinationCombo = new JComboBox<>(this.handles.toArray(String[]::new)); // combo destino (rellenado luego)
        form.add(destinationCombo); // añade
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12)); // padding interno del formulario
        add(form, BorderLayout.CENTER); // añade formulario al centro del diálogo
        add(buildButtons(), BorderLayout.SOUTH); // añade botones abajo
        originCombo.addItemListener(this::onOriginChanged); // escucha cambios en origen
        onOriginChanged(null); // inicializa candidatos para destino
        pack(); // ajusta tamaño
        setLocationRelativeTo(parent); // centra respecto a parent
        SwingUtilities.invokeLater(originCombo::requestFocusInWindow); // solicita foco en el combo origen
    }

    private JPanel buildButtons() { // crea panel de botones aceptar/cancelar
        final JPanel panel = new JPanel(); // panel contenedor
        final JButton accept = new JButton("Aceptar"); // botón aceptar
        accept.addActionListener(this::onAccept); // acción aceptar
        final JButton cancel = new JButton("Cancelar"); // botón cancelar
        cancel.addActionListener(e -> dispose()); // cierra al cancelar
        panel.add(accept); // añade botones
        panel.add(cancel);
        getRootPane().setDefaultButton(accept); // botón por defecto
        return panel; // retorna panel
    }

    private void onOriginChanged(final ItemEvent event) { // actualiza lista de destinos cuando cambia el origen
        final String selectedOrigin = (String) originCombo.getSelectedItem(); // obtiene origen seleccionado
        if (selectedOrigin == null) { // si no hay selección
            return; // nada que hacer
        }
        final List<String> candidates = new ArrayList<>(); // lista temporal de candidatos
        for (String handle : this.handles) { // itera handles
            if (!handle.equalsIgnoreCase(selectedOrigin)) { // excluye al origen para evitar auto-relación
                candidates.add(handle); // añade candidato
            }
        }
        destinationCombo.removeAllItems(); // limpia combo destino
        candidates.forEach(destinationCombo::addItem); // añade candidatos
        if (!candidates.isEmpty()) { // selecciona el primero si hay
            destinationCombo.setSelectedIndex(0);
        }
    }

    private void onAccept(final ActionEvent event) { // confirma selección
        final String selectedOrigin = (String) originCombo.getSelectedItem(); // obtiene origen
        final String selectedDestination = (String) destinationCombo.getSelectedItem(); // obtiene destino
        if (selectedOrigin == null || selectedDestination == null) { // si falta alguno
            return; // no hace nada
        }
        if (selectedOrigin.equalsIgnoreCase(selectedDestination)) { // protección adicional
            destinationCombo.requestFocusInWindow(); // pide foco
            destinationCombo.setToolTipText("El destino debe ser distinto al origen"); // tooltip informativo
            return; // no acepta
        }
        this.origin = selectedOrigin; // guarda selección
        this.destination = selectedDestination; // guarda selección
        dispose(); // cierra diálogo
    }

    public Optional<RelationSelection> getRelation() { // retorna la relación seleccionada (si existe)
        if (origin == null || destination == null) { // si no se aceptó
            return Optional.empty(); // vacío
        }
        return Optional.of(new RelationSelection(origin, destination)); // tupla con origen y destino
    }

    public record RelationSelection(String origin, String destination) { // registro inmutable con selección

    }
}

