package com.gabriel.draw.view;

import com.gabriel.draw.controller.ActionController;
import com.gabriel.drawfx.ActionCommand;
import com.gabriel.drawfx.service.AppService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.net.URL;

public class DrawingToolBar extends JToolBar {

    protected JTextArea textArea;
    ActionListener actionListener;

  public DrawingToolBar( ActionListener actionListener){
        setFloatable(false);
        setRollover(true);
        this.actionListener = actionListener;
        addButtons();

        //Lay out the main panel.
        setPreferredSize(new Dimension(200, 35));
    }

    protected void addButtons() {
        JButton button = null;
        
        button = makeNavigationButton("undo", ActionCommand.UNDO, "Undo", "Undo");
        add(button);
        
        button = makeNavigationButton("redo", ActionCommand.REDO, "Redo", "Redo");
        add(button);
        
        addSeparator();
        
        button = makeNavigationButton("select", ActionCommand.SELECT, "Select", "Select");
        add(button);
        
        button = makeNavigationButton("rect", ActionCommand.RECT, "Rectangle", "Rectangle");
        add(button);

        button = makeNavigationButton("ellipse", ActionCommand.ELLIPSE, "Ellipse", "Ellipse");
        add(button);
        
        button = makeNavigationButton("line", ActionCommand.LINE, "Line", "Line");
        add(button);

        button = makeNavigationButton("text", ActionCommand.TEXT, "Text", "Text");
        add(button);

        button = makeNavigationButton("image", ActionCommand.IMAGE, "Image", "Image");
        add(button);
        
        addSeparator();
        
        button = makeNavigationButton("color", ActionCommand.COLOR, "Color", "Color");
        add(button);
        
        button = makeNavigationButton("fill", ActionCommand.FILL, "Fill", "Fill");
        add(button);
    }

    protected JButton makeNavigationButton(String imageName,
            String actionCommand,
            String toolTipText,
            String altText) {
        //Look for the image.
        String imgLocation = "images/"
                + imageName
                + ".png";
        URL imageURL = DrawingToolBar.class.getResource(imgLocation);

        //Create and initialize the button.
        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(actionListener);

        if (imageURL != null) {                      //image found
            button.setIcon(new ImageIcon(imageURL, altText));
        } else {                                     //no image found
            button.setText(altText);
            System.err.println("Resource not found: "
                    + imgLocation);
        }
        return button;
    }

}
