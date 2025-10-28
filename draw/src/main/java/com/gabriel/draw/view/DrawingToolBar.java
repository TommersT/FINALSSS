package com.gabriel.draw.view;

import com.gabriel.draw.util.ModernIconFactory;
import com.gabriel.drawfx.ActionCommand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class DrawingToolBar extends JToolBar {

    protected JTextArea textArea;
    ActionListener actionListener;
    private Map<String, JButton> toolButtons = new HashMap<>();
    private JButton currentActiveButton = null;
    private JButton undoButton;
    private JButton redoButton;

    public DrawingToolBar(ActionListener actionListener){
        setFloatable(false);
        setRollover(true);
        this.actionListener = actionListener;
        setBackground(new Color(240, 240, 240));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));
        addButtons();
        setPreferredSize(new Dimension(200, 40));
    }

    protected void addButtons() {
        undoButton = makeToolButton("undo", ActionCommand.UNDO, "Undo (Ctrl+Z)");
        redoButton = makeToolButton("redo", ActionCommand.REDO, "Redo (Ctrl+Y)");
        add(undoButton);
        add(redoButton);

        addSeparator(new Dimension(10, 30));

        add(makeToolButton("select", ActionCommand.SELECT, "Select", true));
        add(makeToolButton("move", ActionCommand.MOVE, "Move", true));
        add(makeToolButton("scale", ActionCommand.SCALE, "Scale/Resize", true));

        addSeparator(new Dimension(10, 30));

        add(makeToolButton("line", ActionCommand.LINE, "Line", true));
        add(makeToolButton("rect", ActionCommand.RECT, "Rectangle", true));
        add(makeToolButton("ellipse", ActionCommand.ELLIPSE, "Ellipse", true));
        add(makeToolButton("text", ActionCommand.TEXT, "Text", true));
        add(makeToolButton("image", ActionCommand.IMAGE, "Image", true));

        addSeparator(new Dimension(10, 30));

        add(makeToolButton("color", ActionCommand.COLOR, "Fore Color"));
        add(makeToolButton("fill", ActionCommand.FILL, "Fill Color"));

        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
    }

    protected JButton makeToolButton(String iconType, String actionCommand, String toolTipText) {
        return makeToolButton(iconType, actionCommand, toolTipText, false);
    }

    protected JButton makeToolButton(String iconType, String actionCommand, String toolTipText, boolean isToggleable) {
        JButton button = new JButton();
        button.setIcon(ModernIconFactory.createIcon(iconType));
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(actionListener);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(32, 32));
        button.setMargin(new Insets(4, 4, 4, 4));

        if (isToggleable) {
            toolButtons.put(actionCommand, button);
        }

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!button.equals(currentActiveButton)) {
                    button.setBorderPainted(true);
                    button.setContentAreaFilled(true);
                    button.setBackground(new Color(220, 220, 220));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!button.equals(currentActiveButton)) {
                    button.setBorderPainted(false);
                    button.setContentAreaFilled(false);
                }
            }
        });

        return button;
    }

    public void setActiveTool(String actionCommand) {
        if (currentActiveButton != null) {
            currentActiveButton.setBorderPainted(false);
            currentActiveButton.setContentAreaFilled(false);
            currentActiveButton.setBackground(null);
        }

        JButton newActiveButton = toolButtons.get(actionCommand);
        if (newActiveButton != null) {
            currentActiveButton = newActiveButton;
            currentActiveButton.setBorderPainted(true);
            currentActiveButton.setContentAreaFilled(true);
            currentActiveButton.setBackground(new Color(180, 200, 220));
        }
    }

    public void updateUndoRedoState(boolean canUndo, boolean canRedo) {
        undoButton.setEnabled(canUndo);
        redoButton.setEnabled(canRedo);
    }
}
