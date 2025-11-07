package ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;

/**
 * Diálogo modal para agregar una relación entre dos usuarios.
 * Recibe un arreglo de handles y retorna una RelationSelection o null si se canceló.
 */
public class AddRelationDialog extends JDialog {

    private final JComboBox<String> originCombo;
    private final JComboBox<String> destinationCombo;
    private final String[] handles;
    private String origin;
    private String destination;

    public AddRelationDialog(final MainFrame parent, final String[] handles) {
        super(parent, "Agregar relación", true);
        this.handles = handles == null ? new String[0] : handles.clone();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout(12, 12));
        final JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Origen:"));
        originCombo = new JComboBox<>(this.handles);
        form.add(originCombo);
        form.add(new JLabel("Destino:"));
        destinationCombo = new JComboBox<>(this.handles);
        form.add(destinationCombo);
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(form, BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        originCombo.addItemListener(this::onOriginChanged);
        onOriginChanged(null);
        pack();
        setLocationRelativeTo(parent);
        SwingUtilities.invokeLater(originCombo::requestFocusInWindow);
    }

    private JPanel buildButtons() {
        final JPanel panel = new JPanel();
        final JButton accept = new JButton("Aceptar");
        accept.addActionListener(this::onAccept);
        final JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(e -> dispose());
        panel.add(accept);
        panel.add(cancel);
        getRootPane().setDefaultButton(accept);
        return panel;
    }

    private void onOriginChanged(final ItemEvent ev) {
        final String selectedOrigin = (String) originCombo.getSelectedItem();
        if (selectedOrigin == null) {
            return;
        }
        int count = 0;
        for (String h : this.handles) {
            if (!h.equalsIgnoreCase(selectedOrigin)) {
                count++;
            }
        }
        final String[] candidates = new String[count];
        int idx = 0;
        for (String h : this.handles) {
            if (!h.equalsIgnoreCase(selectedOrigin)) {
                candidates[idx++] = h;
            }
        }
        destinationCombo.removeAllItems();
        for (String c : candidates) {
            destinationCombo.addItem(c);
        }
        if (candidates.length > 0) {
            destinationCombo.setSelectedIndex(0);
        }
    }

    private void onAccept(final ActionEvent ev) {
        final String selOrigin = (String) originCombo.getSelectedItem();
        final String selDest = (String) destinationCombo.getSelectedItem();
        if (selOrigin == null || selDest == null) {
            return;
        }
        if (selOrigin.equalsIgnoreCase(selDest)) {
            destinationCombo.requestFocusInWindow();
            destinationCombo.setToolTipText("El destino debe ser distinto al origen");
            return;
        }
        this.origin = selOrigin;
        this.destination = selDest;
        dispose();
    }

    public RelationSelection getRelation() {
        if (origin == null || destination == null) {
            return null;
        }
        return new RelationSelection(origin, destination);
    }

    public record RelationSelection(String origin, String destination) {
    }
}
