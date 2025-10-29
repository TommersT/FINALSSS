package com.gabriel.drawfx.model;

import com.gabriel.drawfx.DrawMode;
import com.gabriel.drawfx.ShapeMode;
import lombok.Data; // Keep @Data or switch to @Getter/@Setter
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Use @Getter @Setter for more control
@Getter
@Setter
public class Drawing {
    Point location;
    private String filename;
    private String imageFilename;
    private Color color; // Fore Color default
    private Color fill; // Solid Fill Color default
    int thickness  = 1;
    private ShapeMode shapeMode = ShapeMode.Select; // Default to Select
    private DrawMode drawMode = DrawMode.Idle;
    private int SearchRadius = 5;
    private Font font;
    int width; // Canvas width? Usually not stored here
    int height; // Canvas height? Usually not stored here
    List<Shape> shapes;
    // Removed duplicate 'Filename'
    Shape selectedShape;
    String text = "Default text"; // Changed default

    // --- NEW Default Gradient/Visibility Properties ---
    private Color startColor = Color.CYAN; // Default gradient start
    private Color endColor = Color.MAGENTA;   // Default gradient end
    private boolean useGradient = false;        // Default to solid fill
    // Note: 'visible' is typically a shape property, not a drawing default.
    // --- END NEW ---

    public Drawing(){
        location  = new Point(0,0);
        color = Color.RED; // Default fore color
        fill = Color.WHITE; // Default fill color
        font = new Font("SansSerif", Font.PLAIN, 12); // Changed default font
        shapes = new ArrayList<>();
        imageFilename = null;
    }

    // Lombok should generate getters/setters. Add manually if needed:
    // public Color getStartColor() { return startColor; }
    // public void setStartColor(Color startColor) { this.startColor = startColor; }
    // public Color getEndColor() { return endColor; }
    // public void setEndColor(Color endColor) { this.endColor = endColor; }
    // public boolean isUseGradient() { return useGradient; }
    // public void setUseGradient(boolean useGradient) { this.useGradient = useGradient; }
}
