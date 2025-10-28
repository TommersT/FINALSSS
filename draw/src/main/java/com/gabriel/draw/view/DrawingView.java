// tommerst/finalsss/FINALSSS-88c0dd560a5808d9ed1c9bce7a17f6f1ba863bfb/draw/src/main/java/com/gabriel/draw/view/DrawingView.java
package com.gabriel.draw.view;

import com.gabriel.draw.controller.DrawingController; // Import Controller
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class DrawingView extends JPanel {
    AppService appService;
    private BufferedImage buffer;
    private Graphics2D bufferGraphics;
    private DrawingController drawingController; // Add reference to controller

    public DrawingView(AppService appService){
        this.appService  = appService;
        setBackground(Color.WHITE);
        setDoubleBuffered(true); // Keep this true for Swing's double buffering
        setFocusable(true); // Allow the panel to receive keyboard focus
    }

    // Setter for the controller
    public void setDrawingController(DrawingController controller) {
        this.drawingController = controller;
        // Add listeners here now that controller exists
        addMouseListener(controller);
        addMouseMotionListener(controller);
        addKeyListener(controller);
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Let Swing do its initial painting

        int width = getWidth();
        int height = getHeight();

        // Ensure dimensions are valid
        if (width <= 0 || height <= 0) {
            return;
        }

        // Create or recreate the buffer if size changed or it's null
        if (buffer == null || buffer.getWidth() != width || buffer.getHeight() != height) {
            buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            // Get graphics context from the new buffer
            if (bufferGraphics != null) {
                bufferGraphics.dispose(); // Dispose old graphics context if it exists
            }
            bufferGraphics = buffer.createGraphics();
            // Apply rendering hints for quality
            bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            bufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            bufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            bufferGraphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE); // For cleaner strokes
        }

        // --- Drawing onto the buffer ---

        // 1. Clear the buffer with the background color
        bufferGraphics.setColor(getBackground()); // Use panel's background
        bufferGraphics.fillRect(0, 0, width, height);

        // 2. Draw all finalized shapes from the model
        Drawing drawing = appService.getDrawing();
        if (drawing != null) {
            List<Shape> shapes = drawing.getShapes();
            if (shapes != null) {
                // Iterate using index to respect drawing order (optional, but good practice)
                for (int i = 0; i < shapes.size(); i++) {
                    Shape shape = shapes.get(i);
                    // Defensive checks for null shape or renderer
                    if (shape != null && shape.getRendererService() != null) {
                        try {
                            shape.getRendererService().render(bufferGraphics, shape, false);
                        } catch (Exception e) {
                            System.err.println("Error rendering shape " + i + ": " + e.getMessage());
                            // Optionally draw a placeholder or skip
                        }
                    }
                }
            }
        }


        // 3. Draw the shape currently being created (the preview)
        if (drawingController != null) {
            Shape preview = drawingController.getPreviewShape();
            // Defensive checks
            if (preview != null && preview.getRendererService() != null) {
                try {
                    // Render normally (not XOR) as it's drawn on the buffer
                    preview.getRendererService().render(bufferGraphics, preview, false);
                } catch (Exception e) {
                    System.err.println("Error rendering preview shape: " + e.getMessage());
                }
            }
        }

        // --- Drawing the buffer to the screen ---
        g.drawImage(buffer, 0, 0, null);

        // Optional: Clean up graphics context if you were drawing directly to 'g' (not needed here)
        // if (g instanceof Graphics2D) {
        //     ((Graphics2D) g).dispose();
        // }
    }

    // Override getGraphics - Not typically needed when using paintComponent correctly
    // If you ever *do* need to get a Graphics context outside paintComponent (e.g., for direct drawing),
    // it's generally better to draw to the buffer and then call repaint().
    // Avoid returning bufferGraphics directly as it bypasses the normal paint cycle.
    // public Graphics getGraphics() {
    //     // THIS IS GENERALLY DISCOURAGED for custom painting in Swing
    //     // If absolutely necessary for some legacy reason, maybe return bufferGraphics,
    //     // but understand the implications.
    //     // return bufferGraphics != null ? bufferGraphics : super.getGraphics();

    //     // Safer to just call the super method, but rely on paintComponent for drawing
    //      return super.getGraphics();
    // }

    // Ensure proper resource cleanup when the component is removed
    @Override
    public void removeNotify() {
        super.removeNotify();
        if (bufferGraphics != null) {
            bufferGraphics.dispose();
            bufferGraphics = null;
        }
        if (buffer != null) {
            buffer.flush(); // Release system resources associated with the image
            buffer = null;
        }
    }
}