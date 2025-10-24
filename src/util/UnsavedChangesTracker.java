/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.util.ArrayList; // lista de listeners
import java.util.List; // interfaz lista
import java.util.Objects; // validación nulos
import java.util.function.Consumer; // consumidor para listeners

public class UnsavedChangesTracker { // permite marcar estado y notificar listeners

    private final List<Consumer<Boolean>> listeners = new ArrayList<>(); // listeners que reciben el nuevo estado
    private boolean dirty; // estado actual

    public void markDirty() { // marca que hay cambios sin guardar
        updateState(true); // actualiza estado y notifica si cambió
    }

    public void markClean() { // marca que ya no hay cambios pendientes
        updateState(false); // actualiza estado
    }

    public boolean hasUnsavedChanges() { // consulta el estado actual
        return dirty; // retorna flag
    }

    public void addListener(final Consumer<Boolean> listener) { // añade un listener para cambios de estado
        listeners.add(Objects.requireNonNull(listener, "listener")); // valida y agrega
    }

    private void updateState(final boolean newState) { // actualiza estado y notifica listeners si cambió
        if (this.dirty == newState) { // si no hay cambio
            return; // no notificar
        }
        this.dirty = newState; // actualiza campo
        listeners.forEach(listener -> listener.accept(newState)); // notifica a todos
    }
}
