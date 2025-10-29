// tommerst/finalsss/FINALSSS-4b53253892a92ed882030feb653ef76e91b6ab5b/draw/src/main/java/com/gabriel/draw/view/DrawingMenuBar.java
package com.gabriel.draw.view;

import com.gabriel.drawfx.ActionCommand;
// Removed unused ShapeMode and AppService imports

import javax.swing.*;
// Removed unused Color import
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class DrawingMenuBar extends JMenuBar {

    ActionListener actionListener;
    // Keep references to menu items that need state changes
    private JMenuItem undoMenuItem;
    private JMenuItem redoMenuItem;

    public DrawingMenuBar( ActionListener actionListener ){
        super();
        this.actionListener = actionListener;

        // Insert the File menu and menuitems
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        JMenuItem menuItem = new JMenuItem("New");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(actionListener);
        menuItem.setActionCommand(ActionCommand.NEW);
        menu.add(menuItem);

        menuItem = new JMenuItem("Open");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(actionListener);
        menuItem.setActionCommand(ActionCommand.OPEN);
        menu.add(menuItem);

        // FIX: Save As should likely have a different shortcut (e.g., Ctrl+Shift+S)
        menuItem = new JMenuItem("Save As..."); // Added ellipsis
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)); // Changed shortcut
        menuItem.addActionListener(actionListener);
        menuItem.setActionCommand(ActionCommand.SAVEAS);
        menu.add(menuItem);

        menuItem = new JMenuItem("Save");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(actionListener);
        menuItem.setActionCommand(ActionCommand.SAVE);
        menu.add(menuItem);

        // Consider adding Exit
        menu.addSeparator();
        menuItem = new JMenuItem("Exit");
        menuItem.setMnemonic(KeyEvent.VK_X);
        menuItem.addActionListener(e -> System.exit(0)); // Simple exit action
        menu.add(menuItem);


        add(menu);


        menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);

        // Store references to undo/redo items
        undoMenuItem = new JMenuItem("Undo");
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undoMenuItem.addActionListener(actionListener);
        undoMenuItem.setActionCommand(ActionCommand.UNDO);
        undoMenuItem.setEnabled(false); // Initially disabled
        menu.add(undoMenuItem);

        redoMenuItem = new JMenuItem("Redo");
        // FIX: Redo shortcut is typically Ctrl+Y or Ctrl+Shift+Z
        redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        // redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        redoMenuItem.addActionListener(actionListener);
        redoMenuItem.setActionCommand(ActionCommand.REDO);
        redoMenuItem.setEnabled(false); // Initially disabled
        menu.add(redoMenuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Delete");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        menuItem.addActionListener(actionListener);
        menuItem.setActionCommand(ActionCommand.DELETE);
        menu.add(menuItem);

        add(menu);

        menu = new JMenu("Draw");
        menu.setMnemonic(KeyEvent.VK_D);

        menuItem = new JMenuItem("Line");
        menuItem.setActionCommand(ActionCommand.LINE);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        menuItem = new JMenuItem("Rectangle");
        menuItem.setActionCommand(ActionCommand.RECT);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        menuItem = new JMenuItem("Ellipse");
        menuItem.setActionCommand(ActionCommand.ELLIPSE); // Was missing action command
        menuItem.addActionListener(actionListener);
        menu.add(menuItem); // Was adding to wrong place

        menuItem = new JMenuItem("Image"); // Changed from Picture
        //menu.add(menuItem); // Don't add here
        menuItem.setActionCommand(ActionCommand.IMAGE);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem); // Add after setting properties

        menuItem = new JMenuItem("Text");
        menuItem.setActionCommand(ActionCommand.TEXT);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        menu.addSeparator(); // Separate drawing tools from selection tools

        menuItem = new JMenuItem("Select");
        menuItem.setActionCommand(ActionCommand.SELECT);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        // Add Move and Scale to Draw menu for completeness? Or Edit? Let's keep in Edit for now.
        // It might be better to remove them from menu if they are primarily toolbar actions.


        add(menu);

        // Properties - Maybe rename to Format or Style?
        menu = new JMenu("Format"); // Renamed
        menu.setMnemonic(KeyEvent.VK_O); // Changed mnemonic

        menuItem = new JMenuItem("Fore Color..."); // Added ellipsis
        menuItem.setActionCommand(ActionCommand.COLOR);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        menuItem = new JMenuItem("Fill Color..."); // Added ellipsis
        menuItem.setActionCommand(ActionCommand.FILL);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        // Add Thickness here?
        // menuItem = new JMenuItem("Line Thickness...");
        // menuItem.setActionCommand(ActionCommand.THICKNESS); // Need to define this command
        // menuItem.addActionListener(actionListener);
        // menu.add(menuItem);


        this.add(menu);

        // Arrange (Layer) menu
        menu = new JMenu("Arrange");
        menu.setMnemonic(KeyEvent.VK_A);

        menuItem = new JMenuItem("Bring to Front");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        menuItem.setActionCommand(ActionCommand.BRING_TO_FRONT);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        menuItem = new JMenuItem("Bring Forward");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.CTRL_DOWN_MASK));
        menuItem.setActionCommand(ActionCommand.BRING_FORWARD);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        menuItem = new JMenuItem("Send Backward");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.CTRL_DOWN_MASK));
        menuItem.setActionCommand(ActionCommand.SEND_BACKWARD);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        menuItem = new JMenuItem("Send to Back");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        menuItem.setActionCommand(ActionCommand.SEND_TO_BACK);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        this.add(menu);

    }

    // Method to update the enabled state of Undo/Redo menu items
    public void updateUndoRedoState(boolean canUndo, boolean canRedo) {
        if (undoMenuItem != null) {
            undoMenuItem.setEnabled(canUndo);
        }
        if (redoMenuItem != null) {
            redoMenuItem.setEnabled(canRedo);
        }
    }
}