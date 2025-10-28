package com.gabriel.draw.view;

import com.gabriel.draw.util.ModernIconFactory;
import com.gabriel.drawfx.ActionCommand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class DrawingToolBar extends JToolBar {

    protected JTextArea textArea;
    ActionListener actionListener;

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
        add(makeToolButton("undo", ActionCommand.UNDO, "Undo (Ctrl+Z)"));
        add(makeToolButton("redo", ActionCommand.REDO, "Redo (Ctrl+Y)"));
        
        addSeparator(new Dimension(10, 30));
        
        add(makeToolButton("select", ActionCommand.SELECT, "Select"));
        add(makeToolButton("move", ActionCommand.MOVE, "Move"));
        add(makeToolButton("scale", ActionCommand.SCALE, "Scale/Resize"));
        
        addSeparator(new Dimension(10, 30));
        
        add(makeToolButton("line", ActionCommand.LINE, "Line"));
        add(makeToolButton("rect", ActionCommand.RECT, "Rectangle"));
        add(makeToolButton("ellipse", ActionCommand.ELLIPSE, "Ellipse"));
        add(makeToolButton("text", ActionCommand.TEXT, "Text"));
        add(makeToolButton("image", ActionCommand.IMAGE, "Image"));
        
        addSeparator(new Dimension(10, 30));
        
        add(makeToolButton("color", ActionCommand.COLOR, "Fore Color"));
        add(makeToolButton("fill", ActionCommand.FILL, "Fill Color"));
    }

    protected JButton makeToolButton(String iconType, String actionCommand, String toolTipText) {
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
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBorderPainted(true);
                button.setContentAreaFilled(true);
                button.setBackground(new Color(220, 220, 220));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBorderPainted(false);
                button.setContentAreaFilled(false);
            }
        });
        
        return button;
    }
}
