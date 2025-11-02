package com.gabriel.draw.model;

import java.awt.*;
// Removed unused RectangleRenderer import
// import com.gabriel.draw.renderer.RectangleRenderer;

// --- START CORRECTION ---
// Add the correct import for TextRenderer
import com.gabriel.draw.renderer.TextRenderer;
// --- END CORRECTION ---

import com.gabriel.drawfx.model.Shape;

public class Text extends Shape {

    public Text(Point location){
        super (location);
        this.setRendererService(new TextRenderer());
    }
}