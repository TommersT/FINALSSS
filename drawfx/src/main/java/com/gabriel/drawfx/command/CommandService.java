package com.gabriel.drawfx.command;

import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

public class CommandService {
    static Stack<Command> undoStack = new Stack<>();
    static Stack<Command> redoStack = new Stack<>();
    private static List<CommandStackListener> listeners = new ArrayList<>();
    private static boolean isExecutingCommand = false;

    public static boolean isExecutingCommand() {
        return isExecutingCommand;
    }

    public interface CommandStackListener {
        void onStackChanged(boolean canUndo, boolean canRedo);
    }

    public static void addListener(CommandStackListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            System.out.println("CommandService: Listener added. Notifying initial state.");
            notifyListeners();
        }
    }

    public static void removeListener(CommandStackListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        boolean canUndo = !undoStack.empty();
        boolean canRedo = !redoStack.empty();
        System.out.println("CommandService: Notifying listeners. CanUndo=" + canUndo + " (Undo size: " + undoStack.size() + "), CanRedo=" + canRedo + " (Redo size: " + redoStack.size() + ")");
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
            System.err.println("CommandService: Attempted to execute a null command.");
            return;
        }

        System.out.println("CommandService: Executing command: " + command.getClass().getSimpleName());
        isExecutingCommand = true;
        boolean success = false;
        try {
            command.execute();
            success = true;
            System.out.println("CommandService: Execution successful.");
        } catch (Exception e) {
            System.err.println("Error executing command: " + command.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        } finally {
            isExecutingCommand = false;
        }

        if (success) {
            undoStack.push(command);
            System.out.println("CommandService: Pushed to undo stack. Size=" + undoStack.size());
            if (!redoStack.isEmpty()) {
                System.out.println("CommandService: Clearing redo stack.");
                redoStack.clear();
            }
        } else {
            System.err.println("CommandService: Execution failed. Stacks not modified.");
        }
        notifyListeners();
    }

    public static void undo() {
        if (undoStack.empty()) {
            System.out.println("CommandService: Undo stack empty. Cannot undo.");
            notifyListeners();
            return;
        }

        Command command = undoStack.pop();
        System.out.println("CommandService: Undoing command: " + command.getClass().getSimpleName() + ". Popped from undo stack. Size=" + undoStack.size());
        isExecutingCommand = true;
        boolean success = false;
        try {
            command.undo();
            success = true;
            System.out.println("CommandService: Undo successful.");
        } catch (Exception e) {
            System.err.println("Error undoing command: " + command.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            if (!redoStack.isEmpty()) {
                System.err.println("CommandService: Undo failed. Clearing redo stack.");
                redoStack.clear();
            }
            System.err.println("CommandService: Undo failed, success flag is false.");
        } finally {
            isExecutingCommand = false;
        }

        if (success) {
            redoStack.push(command);
            System.out.println("CommandService: Pushed to redo stack. Size=" + redoStack.size());
        } else {
            System.err.println("CommandService: Undo failed, NOT pushing to redo stack. Redo stack size=" + redoStack.size());
        }
        notifyListeners();
    }

    public static void redo() {
        if (redoStack.empty()) {
            System.out.println("CommandService: Redo stack empty. Cannot redo.");
            notifyListeners();
            return;
        }

        Command command = redoStack.pop();
        System.out.println("CommandService: Redoing command: " + command.getClass().getSimpleName() + ". Popped from redo stack. Size=" + redoStack.size());
        isExecutingCommand = true;
        boolean success = false;
        try {
            command.redo();
            success = true;
            System.out.println("CommandService: Redo successful.");
        } catch (Exception e) {
            System.err.println("Error redoing command: " + command.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            System.err.println("CommandService: Redo failed. Command not pushed to undo stack.");
            System.err.println("CommandService: Redo failed, success flag is false.");
        } finally {
            isExecutingCommand = false;
        }

        if (success) {
            undoStack.push(command);
            System.out.println("CommandService: Pushed redo'd command to undo stack. Size=" + undoStack.size());
        } else {
            System.err.println("CommandService: Redo failed, NOT pushing to undo stack. Undo stack size=" + undoStack.size());
        }
        notifyListeners();
    }

    public static boolean canUndo() {
        return !undoStack.empty();
    }

    public static boolean canRedo() {
        return !redoStack.empty();
    }

    public static void clear() {
        System.out.println("CommandService: Clearing both stacks.");
        undoStack.clear();
        redoStack.clear();
        notifyListeners();
    }
}
