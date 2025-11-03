package com.gabriel.draw.view;

import com.gabriel.drawfx.ActionCommand;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class DrawingMenuBar extends JMenuBar {

    ActionListener actionListener;
    private JMenuItem undoMenuItem;
    private JMenuItem redoMenuItem;

    public DrawingMenuBar(ActionListener actionListener) {
        super();
        this.actionListener = actionListener;

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

        menuItem = new JMenuItem("Save As...");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        menuItem.addActionListener(actionListener);
        menuItem.setActionCommand(ActionCommand.SAVEAS);
        menu.add(menuItem);

        menuItem = new JMenuItem("Save");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        menuItem.addActionListener(actionListener);
        menuItem.setActionCommand(ActionCommand.SAVE);
        menu.add(menuItem);

        menu.addSeparator();
        menuItem = new JMenuItem("Exit");
        menuItem.setMnemonic(KeyEvent.VK_X);
        menuItem.addActionListener(e -> System.exit(0));
        menu.add(menuItem);

        add(menu);

        menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);

        undoMenuItem = new JMenuItem("Undo");
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undoMenuItem.addActionListener(actionListener);
        undoMenuItem.setActionCommand(ActionCommand.UNDO);
        undoMenuItem.setEnabled(false);
        menu.add(undoMenuItem);

        redoMenuItem = new JMenuItem("Redo");
        redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        redoMenuItem.addActionListener(actionListener);
        redoMenuItem.setActionCommand(ActionCommand.REDO);
        redoMenuItem.setEnabled(false);
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
        menuItem.setActionCommand(ActionCommand.ELLIPSE);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        menuItem = new JMenuItem("Image");
        menuItem.setActionCommand(ActionCommand.IMAGE);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        menuItem = new JMenuItem("Text");
        menuItem.setActionCommand(ActionCommand.TEXT);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Select");
        menuItem.setActionCommand(ActionCommand.SELECT);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        add(menu);

        menu = new JMenu("Format");
        menu.setMnemonic(KeyEvent.VK_O);

        menuItem = new JMenuItem("Fore Color...");
        menuItem.setActionCommand(ActionCommand.COLOR);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        menuItem = new JMenuItem("Fill Color...");
        menuItem.setActionCommand(ActionCommand.FILL);
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        this.add(menu);

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

    public void updateUndoRedoState(boolean canUndo, boolean canRedo) {
        if (undoMenuItem != null) {
            undoMenuItem.setEnabled(canUndo);
        }
        if (redoMenuItem != null) {
            redoMenuItem.setEnabled(canRedo);
        }
    }
}
