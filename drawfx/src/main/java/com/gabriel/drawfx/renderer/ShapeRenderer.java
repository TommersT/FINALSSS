// tommerst/finalsss/FINALSSS-de875fc7483c0551319e977be48e282c3289c2a8/drawfx/src/main/java/com/gabriel/drawfx/renderer/ShapeRenderer.java
// Updated with Line handle logic
package com.gabriel.drawfx.renderer;

import com.gabriel.drawfx.model.Shape;

import java.awt.*;

public class ShapeRenderer implements Renderer {

    @Override
    public void render(Graphics g,  Shape shape, boolean xor) {
        if(shape.isSelected()){
            Point loc = shape.getLocation();
            int width = shape.getWidth();
            int height = shape.getHeight();
            int r = 5; // Handle radius/half-size

            Graphics2D g2 = (Graphics2D) g; // Use Graphics2D for potential stroke changes
            Color handleColor = Color.BLACK; // Or use shape.getColor()?
            Stroke handleStroke = new BasicStroke(1); // Thin stroke for handles

            // Set XOR mode or normal color
            Composite originalComposite = g2.getComposite(); // Save original composite
            if(xor){
                g2.setXORMode(handleColor); // XOR with handle color
            }
            else {
                g2.setColor(handleColor);
            }
            g2.setStroke(handleStroke);


            // --- START MODIFICATION ---
            // Check if the shape is an instance of the specific Line class from the draw module
            // Using instanceof is generally preferred over comparing class names
            boolean isLine = shape instanceof com.gabriel.draw.model.Line;

            if (isLine) {
                // Draw handles only at start and end points for Lines
                g2.fillRect(loc.x - r, loc.y - r, 2 * r, 2 * r); // Start point handle (filled)
                g2.fillRect(loc.x + width - r, loc.y + height - r, 2 * r, 2 * r); // End point handle (filled)
            } else {
                // Draw all 8 handles for other shapes (Rect, Ellipse, Text, Image)
                // Use fillRect for solid handles which are easier to see/click
                g2.fillRect(loc.x-r,loc.y-r, 2*r,2*r); // UpperLeft
                g2.fillRect(loc.x-r,loc.y+height-r, 2*r, 2*r); // LowerLeft
                g2.fillRect(loc.x + width -r,loc.y -r, 2*r, 2*r); // UpperRight
                g2.fillRect(loc.x + width -r,loc.y+height-r, 2*r, 2*r); // LowerRight

                g2.fillRect(loc.x + width/2 -r,loc.y-r, 2*r, 2*r); // MiddleTop
                g2.fillRect(loc.x -r,loc.y+height/2-r, 2*r, 2*r); // MiddleLeft
                g2.fillRect(loc.x + width -r,loc.y+height/2-r, 2*r, 2*r); // MiddleRight
                g2.fillRect(loc.x + width/2 -r,loc.y+height-r, 2*r, 2*r); // MiddleBottom
            }
            // --- END MODIFICATION ---

            // Restore original paint mode and composite if XOR was used
            if (xor) {
                g2.setPaintMode();
            }
            g2.setComposite(originalComposite); // Restore original composite
        }
    }
}