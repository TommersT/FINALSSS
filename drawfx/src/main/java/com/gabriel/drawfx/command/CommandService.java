package com.gabriel.drawfx.command;

import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

public class CommandService {
    static Stack<Command> undoStack = new Stack<Command>();
    static Stack<Command> redoStack = new Stack<Command>();
    private static List<CommandStackListener> listeners = new ArrayList<>();

    public interface CommandStackListener {
        void onStackChanged(boolean canUndo, boolean canRedo);
    }

    public static void addListener(CommandStackListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(CommandStackListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        boolean canUndo = !undoStack.empty();
        boolean canRedo = !redoStack.empty();
        for (CommandStackListener listener : listeners) {
            listener.onStackChanged(canUndo, canRedo);
        }
    }

    public static void ExecuteCommand(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
        notifyListeners();
    }

    public static void undo() {
        if (undoStack.empty())
            return;
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        notifyListeners();
    }

    public static void redo() {
        if (redoStack.empty())
            return;
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        notifyListeners();
    }

    public static boolean canUndo() {
        return !undoStack.empty();
    }

    public static boolean canRedo() {
        return !redoStack.empty();
    }

    public static void clear() {
        undoStack.clear();
        redoStack.clear();
        notifyListeners();
    }
}