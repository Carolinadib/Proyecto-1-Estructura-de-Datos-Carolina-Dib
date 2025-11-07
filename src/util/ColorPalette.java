package util; // paleta de colores y utilidad para construir stylesheet para GraphStream

/**
 * Paleta de colores y utilidad para construir hojas de estilo (stylesheet)
 * utilizadas por GraphStream para colorear componentes fuertemente conectadas
 * en la visualización.
 */
public final class ColorPalette { // clase utilitaria para colores

    private static final String[] COLORS = new String[]{ // arreglo de colores hexadecimales
        "#1f77b4",
        "#ff7f0e",
        "#2ca02c",
        "#d62728",
        "#9467bd",
        "#8c564b",
        "#e377c2",
        "#7f7f7f",
        "#bcbd22",
        "#17becf",
        "#fdd835",
        "#ff6f61",
        "#6b5b95",
        "#88b04b",
        "#ffa500"
    };

    private ColorPalette() { // previene instanciación
        throw new UnsupportedOperationException("Utility class");
    }

    public static String colorForIndex(final int index) { // obtiene color seguro por índice
        final int safeIndex = Math.floorMod(index, COLORS.length); // modulo que maneja índices negativos
        return COLORS[safeIndex]; // retorna color
    }

    public static String[] palette() { // expone la paleta completa como arreglo
        return COLORS.clone(); // retorna copia para mantener inmutabilidad
    }

    public static String buildStylesheet(final int componentCount) { // construye stylesheet CSS para GraphStream según número de componentes
        final StringBuilder builder = new StringBuilder("graph { padding: 60px; }"); // inicio del stylesheet
        for (int i = 0; i < componentCount; i++) { // crea reglas para cada componente
            builder.append(" node.scc-").append(i)
                    .append(" { fill-color: ")
                    .append(colorForIndex(i))
                    .append("; text-size: 18px; }"); // añade regla con color y tamaño de texto
        }
        builder.append(" node.default { fill-color: #546e7a; text-size: 18px; }"); // estilo por defecto
        builder.append(" edge { fill-color: #90a4ae; arrow-shape: arrow; }"); // estilo para aristas
        return builder.toString(); // retorna stylesheet final
    }
}
