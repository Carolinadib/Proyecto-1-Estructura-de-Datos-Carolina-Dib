package util; // utilidades para mostrar diálogos de alerta y confirmación

import javax.swing.JOptionPane; // diálogos estándar Swing
import java.awt.Component; // componente padre para centrar diálogos

/**
 * Utilidad para mostrar diálogos de información, advertencia, error y
 * confirmación usando {@link javax.swing.JOptionPane}. Proporciona métodos
 * estáticos para uso desde la UI.
 */
public final class Alerts { // clase utilitaria no instanciable para mostrar mensajes

    private Alerts() { // previene instanciación
        throw new UnsupportedOperationException("Utility class");
    }

    public static void info(final Component parent, final String title, final String message) { // diálogo informativo
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE); // usa tipo INFORMATION
    }

    public static void warn(final Component parent, final String title, final String message) { // diálogo de advertencia
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE); // tipo WARNING
    }

    public static void error(final Component parent, final String title, final String message) { // diálogo de error
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE); // tipo ERROR
    }

    public static int confirmSaveDiscardOrCancel(final Component parent, final String fileName) { // diálogo personalizado guardar/no guardar/cancelar
        final String safeName = fileName == null ? "Sin título" : fileName;
        final String message = "El archivo '" + safeName + "' tiene cambios sin guardar. ¿Desea guardarlos?"; // mensaje que incluye nombre de archivo
        final Object[] options = {"Guardar", "No guardar", "Cancelar"}; // opciones en ese orden
        final int choice = JOptionPane.showOptionDialog(parent,
                message,
                "Cambios sin guardar",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]); // muestra diálogo personalizado
        if (choice == JOptionPane.CLOSED_OPTION) { // si cerró el diálogo como si cancelara
            return JOptionPane.CANCEL_OPTION; // unifica como cancelar
        }
        return choice; // retorna elección (YES/NO/CANCEL)
    }

    public static boolean confirmDeletion(final Component parent, final String message) { // diálogo de confirmación simple
        final int result = JOptionPane.showConfirmDialog(parent,
                message,
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE); // pregunta sí/no
        return result == JOptionPane.YES_OPTION; // true si confirmó
    }
}
