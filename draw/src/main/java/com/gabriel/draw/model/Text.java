package com.gabriel.draw.model;

import java.awt.*;
// Removed unused RectangleRenderer import
// import com.gabriel.draw.renderer.RectangleRenderer;

// --- START CORRECTION ---
// Add the correct import for TextRenderer
import com.gabriel.draw.renderer.TextRenderer;
// --- END CORRECTION ---

import com.gabriel.drawfx.model.Shape;
// Removed unused lombok import, as @Data wasn't used here explicitly
// import lombok.Data;

// Removed @Data annotation as it wasn't being used and might hide missing methods if lombok isn't set up correctly
public class Text extends Shape {

    public Text(Point location){
        super (location);
        // This line now compiles because TextRenderer is imported
        this.setRendererService(new TextRenderer());
    }
}