package com.gabriel.drawfx.model;

import com.gabriel.drawfx.SelectionMode;
import com.gabriel.drawfx.renderer.Renderer;
import lombok.Data;
import lombok.Getter; // Add specific imports if needed
import lombok.Setter; // Add specific imports if needed

import java.awt.*;

// Using @Getter @Setter might be safer than @Data if you have custom logic later
@Getter
@Setter
public abstract class Shape {
    boolean selected = false;
    private SelectionMode selectionMode = SelectionMode.None;
    private int r = 2; // Handle radius
    private int id = 0;
    private Point location;
    private int width = 0;
    private int height = 0;
    private Color color = Color.RED; // Fore Color / Stroke Color
    private Color fill = Color.WHITE; // Solid Fill Color
    private int thickness = 2;
    private float opacity = 1.0f; // Kept, but not in current property list
    String text;
    Font font;

    // --- NEW PROPERTIES ---
    private Color startColor = Color.LIGHT_GRAY; // Default gradient start
    private Color endColor = Color.DARK_GRAY;   // Default gradient end
    private boolean useGradient = false;        // Default to solid fill
    private boolean visible = true;             // Default to visible
    // --- END NEW PROPERTIES ---

    private Renderer rendererService;

    // --- CONSTRUCTORS (Unchanged) ---
    public Shape(){
    }
    public Shape(Point location){
        this.setLocation(location);
    }
    public Shape(Point location, Point endpoint){
        width = endpoint.x - location.x;
        height = endpoint.y - location.y;
        // Basic normalization, might be handled better elsewhere
        if(width<0){
            location.x += width; // Adjust location if width is negative
            width = -width;
        }
        if(height < 0){
            location.y += height; // Adjust location if height is negative
            height = -height;
        }
        this.setLocation(location);
    }
    public Shape(Point location, int width, int height){
        this.setLocation(location);
        this.width = width;
        this.height = height;
    }
    // --- END CONSTRUCTORS ---


    // Lombok should generate getters/setters for all fields now, including new ones.
    // Manually add them if Lombok isn't working correctly.


    // Equals method - simple ID check
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Shape shape = (Shape) obj;
        return id == shape.id && id != 0; // Only use ID if it's set
    }

    // Simple hashCode based on ID, or default if ID is 0
    @Override
    public int hashCode() {
        return id != 0 ? Integer.hashCode(id) : super.hashCode();
    }
}
