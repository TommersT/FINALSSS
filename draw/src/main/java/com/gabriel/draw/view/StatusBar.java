package com.gabriel.draw.view;

import javax.swing.*;
import java.awt.*;

public class StatusBar extends JPanel {
    private JLabel toolLabel;
    private JLabel coordinatesLabel;
    private JLabel shapeLabel;
    
    public StatusBar() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 25));
        setBackground(new Color(240, 240, 240));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)));
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        leftPanel.setBackground(new Color(240, 240, 240));
        
        toolLabel = createLabel("Tool: Select");
        coordinatesLabel = createLabel("X: 0, Y: 0");
        shapeLabel = createLabel("No shape selected");
        
        leftPanel.add(toolLabel);
        leftPanel.add(createSeparator());
        leftPanel.add(coordinatesLabel);
        leftPanel.add(createSeparator());
        leftPanel.add(shapeLabel);
        
        add(leftPanel, BorderLayout.WEST);
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        label.setForeground(new Color(80, 80, 80));
        return label;
    }
    
    private JLabel createSeparator() {
        JLabel sep = new JLabel("|");
        sep.setForeground(new Color(180, 180, 180));
        return sep;
    }
    
    public void setToolText(String tool) {
        toolLabel.setText("Tool: " + tool);
    }
    
    public void setCoordinates(int x, int y) {
        coordinatesLabel.setText("X: " + x + ", Y: " + y);
    }
    
    public void setShapeInfo(String info) {
        shapeLabel.setText(info);
    }
    
    public void setShapeName(String shapeName) {
        if (shapeName == null || shapeName.isEmpty()) {
            shapeLabel.setText("No shape selected");
        } else {
            shapeLabel.setText("Shape: " + shapeName);
        }
    }
}
