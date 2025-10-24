/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ui;

import javax.swing.BorderFactory; // factory para bordes
import javax.swing.JButton; // botón Swing
import javax.swing.JDialog; // diálogo modal
import javax.swing.JLabel; // etiqueta de texto
import javax.swing.JPanel; // panel contenedor
import javax.swing.WindowConstants; // constantes para cierre de ventana
import java.awt.BorderLayout; // layout de bordes

public class AboutDialog extends JDialog { // diálogo simple 'Acerca de'

    public AboutDialog(final MainFrame parent) { // constructor que recibe ventana padre
        super(parent, "Acerca de Kosaraju-Nexus", true); // modal
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // disposición al cerrar
        setResizable(false); // no redimensionable
        setLayout(new BorderLayout()); // layout principal
        final JLabel label = new JLabel("<html><h2>Kosaraju-Nexus</h2>Version 1.0.0<br/>Autores: Equipo Ciudadela-Conexa</html>"); // contenido HTML
        label.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16)); // padding interno
        add(label, BorderLayout.CENTER); // añade etiqueta al centro
        final JPanel buttons = new JPanel(); // panel para botones
        final JButton close = new JButton("Cerrar"); // botón cerrar
        close.addActionListener(e -> dispose()); // cierra diálogo al pulsar
        buttons.add(close); // añade botón al panel
        add(buttons, BorderLayout.SOUTH); // coloca panel de botones abajo
        pack(); // ajusta tamaño según contenido
        setLocationRelativeTo(parent); // centra respecto a la ventana padre
        getRootPane().setDefaultButton(close); // botón por defecto
    }
}
