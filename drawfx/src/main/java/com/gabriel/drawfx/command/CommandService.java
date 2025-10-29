// tommerst/finalsss/FINALSSS-4b53253892a92ed882030feb653ef76e91b6ab5b/drawfx/src/main/java/com/gabriel/drawfx/command/CommandService.java
package com.gabriel.drawfx.command;

import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

public class CommandService {
    static Stack<Command> undoStack = new Stack<>();
    static Stack<Command> redoStack = new Stack<>();
    private static List<CommandStackListener> listeners = new ArrayList<>();
    private static boolean isExecutingCommand = false; // Flag to prevent listener loops

    // Getter for the flag
    public static boolean isExecutingCommand() {
        return isExecutingCommand;
    }

    public interface CommandStackListener {
        void onStackChanged(boolean canUndo, boolean canRedo);
    }

    public static void addListener(CommandStackListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            System.out.println("CommandService: Listener added. Notifying initial state."); // Log
            notifyListeners(); // Notify with current state
        }
    }

    public static void removeListener(CommandStackListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        boolean canUndo = !undoStack.empty();
        boolean canRedo = !redoStack.empty();
        System.out.println("CommandService: Notifying listeners. CanUndo=" + canUndo + ", CanRedo=" + canRedo); // Log
        // Use try-catch around listener notification for safety
        // Create a copy of listeners to avoid ConcurrentModificationException if a listener modifies the list
        List<CommandStackListener> listenersCopy = new ArrayList<>(listeners);
        for (CommandStackListener listener : listenersCopy) {
            try {
                listener.onStackChanged(canUndo, canRedo);
            } catch (Exception e) {
                System.err.println("Error notifying CommandStackListener: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void ExecuteCommand(Command command) {
        if (command == null) {
            System.err.println("CommandService: Attempted to execute a null command."); // Log
            return;
        }

        System.out.println("CommandService: Executing command: " + command.getClass().getSimpleName()); // Log
        isExecutingCommand = true;
        boolean success = false;
        try {
            command.execute();
            success = true; // Mark success only if no exception
            System.out.println("CommandService: Execution successful."); // Log
        } catch (Exception e) {
            System.err.println("Error executing command: " + command.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        } finally {
            isExecutingCommand = false;
        }

        if (success) {
            undoStack.push(command);
            System.out.println("CommandService: Pushed to undo stack. Size=" + undoStack.size()); // Log
            if (!redoStack.isEmpty()) {
                System.out.println("CommandService: Clearing redo stack."); // Log
                redoStack.clear(); // Clear redo stack on successful new command
            }
        } else {
            System.err.println("CommandService: Execution failed. Stacks not modified."); // Log
        }
        // Always notify listeners after attempting execution
        notifyListeners();
    }

    public static void undo() {
        if (undoStack.empty()) {
            System.out.println("CommandService: Undo stack empty. Cannot undo."); // Log
            return;
        }

        Command command = undoStack.pop();
        System.out.println("CommandService: Undoing command: " + command.getClass().getSimpleName() + ". Popped from undo stack. Size=" + undoStack.size()); // Log
        isExecutingCommand = true;
        boolean success = false;
        try {
            command.undo();
            success = true; // Mark success
            System.out.println("CommandService: Undo successful."); // Log
        } catch (Exception e) {
            System.err.println("Error undoing command: " + command.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            // Critical: If undo fails, clear redo stack as history is broken.
            if (!redoStack.isEmpty()) {
                System.err.println("CommandService: Undo failed. Clearing redo stack."); // Log
                redoStack.clear();
            }
        } finally {
            isExecutingCommand = false;
        }

        // Only push to redo stack if undo was successful
        if (success) {
            redoStack.push(command);
            System.out.println("CommandService: Pushed to redo stack. Size=" + redoStack.size()); // Log
        }
        notifyListeners(); // Notify UI of stack changes
    }

    public static void redo() {
        if (redoStack.empty()) {
            System.out.println("CommandService: Redo stack empty. Cannot redo."); // Log
            return;
        }

        Command command = redoStack.pop();
        System.out.println("CommandService: Redoing command: " + command.getClass().getSimpleName() + ". Popped from redo stack. Size=" + redoStack.size()); // Log
        isExecutingCommand = true;
        boolean success = false;
        try {
            command.redo(); // Redo calls execute or specific redo logic
            success = true; // Mark success
            System.out.println("CommandService: Redo successful."); // Log
        } catch (Exception e) {
            System.err.println("Error redoing command: " + command.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            // If redo fails, don't push back to undo stack. Leave redoStack popped.
            System.err.println("CommandService: Redo failed. Command not pushed to undo stack."); // Log
        } finally {
            isExecutingCommand = false;
        }

        // Only push to undo stack if redo was successful
        if (success) {
            undoStack.push(command);
            System.out.println("CommandService: Pushed to undo stack. Size=" + undoStack.size()); // Log
        }
        notifyListeners(); // Notify UI of stack changes
    }


    public static boolean canUndo() {
        return !undoStack.empty();
    }

    public static boolean canRedo() {
        return !redoStack.empty();
    }

    public static void clear() {
        System.out.println("CommandService: Clearing both stacks."); // Log
        undoStack.clear();
        redoStack.clear();
        notifyListeners();
    }
}