package com.gabriel.property.event;

import com.gabriel.property.property.Property;

import java.util.ArrayList;
import java.util.Collections; // Import Collections
import java.util.List;      // Import List

public class EventDispatcher {

    private ArrayList<PropertyEventListener> listeners;

    public EventDispatcher() {
        this.listeners = new ArrayList<>();
    }

    public void addEventListener(PropertyEventListener eventListener) {
        listeners.add(eventListener);
    }

    public void removeEventListener(PropertyEventListener eventListener) {
        listeners.remove(eventListener);
    }

    public void dispatchUpdateEvent(Property property) {
        // Use a copy to avoid ConcurrentModificationException if a listener modifies the list
        List<PropertyEventListener> listenersCopy = new ArrayList<>(listeners);
        for (PropertyEventListener listener : listenersCopy) {
            try {
                listener.onPropertyUpdated(property);
            } catch (Exception e) {
                System.err.println("Error dispatching update event to listener: " + listener.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void dispatchPropertyAddedEvent(Property property) {
        // Use a copy for safety
        List<PropertyEventListener> listenersCopy = new ArrayList<>(listeners);
        for (PropertyEventListener listener : listenersCopy) {
            try {
                listener.onPropertyAdded(property);
            } catch (Exception e) {
                System.err.println("Error dispatching property added event to listener: " + listener.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // *** THIS METHOD MUST BE PRESENT ***
    /**
     * Returns an unmodifiable list of the current listeners.
     * Used by PropertySheet to access the AppService via the listener.
     * @return An unmodifiable list of PropertyEventListener.
     */
    public List<PropertyEventListener> getListeners() {
        // Return an unmodifiable view to prevent external modification
        return Collections.unmodifiableList(listeners);
    }
    // *** END METHOD ***
}