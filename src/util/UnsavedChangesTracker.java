package util; // rastreador simple de estado 'dirty' (cambios sin guardar)

/**
 * Pequeño rastreador de cambios sin guardar (flag "dirty") usado por la UI para
 * notificar la necesidad de guardar. Permite registrar listeners que reaccionan
 * a cambios de estado.
 */
public class UnsavedChangesTracker { // permite marcar estado y notificar listeners

    /**
     * Listener funcional que recibe el nuevo estado {@code dirty} como un
     * primitivo {@code boolean}.
     */
    @FunctionalInterface
    public static interface BooleanConsumer {

        /**
         * Notifica el nuevo estado.
         *
         * @param value nuevo estado (true = hay cambios sin guardar)
         */
        void accept(boolean value);
    }

    private BooleanConsumer[] listeners = new BooleanConsumer[4]; // arreglo simple de listeners
    private int listenerCount = 0; // número actual de listeners
    private boolean dirty; // estado actual

    /**
     * Marca el estado como con cambios pendientes (dirty).
     */
    public void markDirty() { // marca que hay cambios sin guardar
        updateState(true); // actualiza estado y notifica si cambió
    }

    /**
     * Marca el estado como limpio (no hay cambios pendientes).
     */
    public void markClean() { // marca que ya no hay cambios pendientes
        updateState(false); // actualiza estado
    }

    /**
     * Indica si existen cambios pendientes de guardado.
     *
     * @return {@code true} si hay cambios sin guardar
     */
    public boolean hasUnsavedChanges() { // consulta el estado actual
        return dirty; // retorna flag
    }

    /**
     * Registra un listener que recibe el estado como primitivo boolean.
     *
     * @param listener listener a registrar
     */
    public void addListener(final BooleanConsumer listener) { // añade un listener para cambios de estado
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        ensureCapacity();
        listeners[listenerCount++] = listener;
    }

    /**
     * Compatibilidad binaria: registra un {@link java.util.function.Consumer}
     * que acepta {@link Boolean} y lo delega al listener primitivo.
     *
     * @param listener consumidor de {@link Boolean}
     */
    public void addListener(final java.util.function.Consumer<Boolean> listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        // delega en la implementación existente; el autoboxing hará la conversión
        addListener((boolean value) -> listener.accept(value));
    }

    private void ensureCapacity() {
        if (listenerCount >= listeners.length) {
            final BooleanConsumer[] next = new BooleanConsumer[listeners.length * 2];
            System.arraycopy(listeners, 0, next, 0, listeners.length);
            listeners = next;
        }
    }

    private void updateState(final boolean newState) { // actualiza estado y notifica listeners si cambió
        if (this.dirty == newState) { // si no hay cambio
            return; // no notificar
        }
        this.dirty = newState; // actualiza campo
        for (int i = 0; i < listenerCount; i++) {
            final BooleanConsumer l = listeners[i];
            if (l != null) {
                l.accept(newState);
            }
        }
    }
}
