// tommerst/finalsss/FINALSSS-ab98287e5a251ea0f90f45adc2a537ebda4a1511/draw/src/main/java/com/gabriel/draw/controller/PropertyEventListener.java
package com.gabriel.draw.controller;

import com.gabriel.draw.command.*; // Import all commands
import com.gabriel.drawfx.ShapeMode;
import com.gabriel.drawfx.command.CommandService; // Import CommandService
import com.gabriel.property.event.PropertyEventAdapter;
import com.gabriel.property.property.Property;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.drawfx.command.Command; // Import Command interface
import java.awt.Color;
import java.awt.Font;
import java.util.Objects; // Import Objects for null-safe equals

public class PropertyEventListener extends PropertyEventAdapter {
    private AppService appService; // This should be the command-wrapped service

    public PropertyEventListener(AppService appService) {
        this.appService = appService;
    }

    @Override
    public void onPropertyUpdated(Property property) {

        // *** Prevent updates triggered BY command execution/undo/redo ***
        if (CommandService.isExecutingCommand()) {
            // If a command (like Undo/Redo or the command triggered by a previous
            // property change) is currently running, don't create a new command.
            return; // Exit early to prevent feedback loops
        }

        Command cmd = null; // Initialize command to null
        String propName = property.getName();
        Object propValue = property.getValue(); // Get value once

        try {
            // --- Match property names EXACTLY as defined in PropertySheet ---

            // Colors
            if (propName.equals("Fore Color")) {
                Color oldValue = appService.getColor();
                Color newValue = (Color) propValue;
                if (!Objects.equals(oldValue, newValue)) {
                    cmd = new SetColorCommand(appService, oldValue, newValue);
                }
            } else if (propName.equals("Fill Color")) {
                Color oldValue = appService.getFill();
                Color newValue = (Color) propValue;
                if (!Objects.equals(oldValue, newValue)) {
                    cmd = new SetFillCommand(appService, oldValue, newValue);
                }
            } else if (propName.equals("Start Color")) {
                Color oldValue = appService.getStartColor();
                Color newValue = (Color) propValue;
                if (!Objects.equals(oldValue, newValue)) {
                    cmd = new SetStartColorCommand(appService, oldValue, newValue);
                }
            } else if (propName.equals("End Color")) {
                Color oldValue = appService.getEndColor();
                Color newValue = (Color) propValue;
                if (!Objects.equals(oldValue, newValue)) {
                    cmd = new SetEndColorCommand(appService, oldValue, newValue);
                }
            }
            // Booleans
            else if (propName.equals("Use Gradient")) {
                boolean oldValue = appService.isUseGradient();
                if (propValue instanceof Boolean) {
                    boolean newValue = (Boolean) propValue;
                    if (oldValue != newValue) {
                        cmd = new SetUseGradientCommand(appService, oldValue, newValue);
                    }
                }
            } else if (propName.equals("Visible")) {
                boolean oldValue = appService.isVisible(); // Gets selected shape's visibility
                if (propValue instanceof Boolean) {
                    boolean newValue = (Boolean) propValue;
                    // Only create command if a shape is selected and value changed
                    if (appService.getSelectedShape() != null && oldValue != newValue) {
                        cmd = new SetVisibleCommand(appService, oldValue, newValue);
                    }
                }
            }
            // Position/Size
            else if (propName.equals("X Location")) { // <<< MATCHED NAME
                int oldValue = appService.getXLocation();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) { // Check if shape selected
                        int oldY = appService.getYLocation(); // Get current Y for command
                        cmd = new SetPositionCommand(appService, oldValue, oldY, newValue, oldY); // Create command
                    }
                }
            } else if (propName.equals("Y Location")) { // <<< MATCHED NAME
                int oldValue = appService.getYLocation();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) { // Check if shape selected
                        int oldX = appService.getXLocation(); // Get current X for command
                        cmd = new SetPositionCommand(appService, oldX, oldValue, oldX, newValue); // Create command
                    }
                }
            } else if (propName.equals("Width")) { // <<< MATCHED NAME
                int oldValue = appService.getWidth();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) { // Check if shape selected
                        cmd = new SetWidthCommand(appService, oldValue, newValue); // Create command
                    }
                }
            } else if (propName.equals("Height")) { // <<< MATCHED NAME
                int oldValue = appService.getHeight();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) { // Check if shape selected
                        cmd = new SetHeightCommand(appService, oldValue, newValue); // Create command
                    }
                }
            }
            // Thickness
            else if (propName.equals("Line Thickness")) {
                int oldValue = appService.getThickness();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue) {
                        cmd = new SetThicknessCommand(appService, oldValue, newValue);
                    }
                }
            }
            // Text/Font
            else if (propName.equals("Text")) {
                String oldValue = appService.getText();
                String newValue = (String) propValue;
                if (!Objects.equals(oldValue, newValue)) {
                    cmd = new SetTextCommand(appService, oldValue, (newValue != null ? newValue : ""));
                }
            } else if (propName.equals("Font Size")) { // <<< MATCHED NAME
                Font oldFont = appService.getFont();
                if (oldFont != null && propValue instanceof Integer) {
                    int oldValue = oldFont.getSize();
                    int newValue = (int) propValue;
                    if (oldValue != newValue) {
                        cmd = new SetFontSizeCommand(appService, oldValue, newValue, oldFont);
                    }
                }
            } else if (propName.equals("Font Family")) { // <<< MATCHED NAME
                Font oldFont = appService.getFont();
                if (oldFont != null) {
                    String oldValue = oldFont.getFamily();
                    String newValue = (String) propValue;
                    if (!Objects.equals(oldValue, newValue)) {
                        cmd = new SetFontFamilyCommand(appService, oldFont, newValue);
                    }
                }
            } else if (propName.equals("Font Style")) { // <<< MATCHED NAME
                Font oldFont = appService.getFont();
                // Font style value comes from SelectionProperty<Integer>
                if (oldFont != null && propValue instanceof Integer) {
                    int oldValue = oldFont.getStyle();
                    int newValue = (int) propValue;
                    if (oldValue != newValue) {
                        cmd = new SetFontStyleCommand(appService, oldValue, newValue, oldFont);
                    }
                }
            }
            // Current Shape Tool (If you add this back to PropertySheet)
            /* else if (propName.equals("Current Shape Tool")) { // Changed name slightly
                ShapeMode oldValue = appService.getShapeMode();
                if (propValue instanceof ShapeMode) {
                    ShapeMode newValue = (ShapeMode) propValue;
                    if (oldValue != newValue) {
                        cmd = new SetShapeCommand(appService, oldValue, newValue);
                    }
                }
            } */

        } catch (ClassCastException e) {
            System.err.println("PropertyEventListener: Error casting property value for '" + propName + "'. Value: " + propValue + ", Error: " + e.getMessage());
        } catch (Exception e) { // Catch other potential errors
            System.err.println("PropertyEventListener: Unexpected error processing property '" + propName + "': " + e.getMessage());
            e.printStackTrace();
        }

        // Execute command ONLY if one was created (meaning a change occurred)
        if (cmd != null) {
            // System.out.println("Executing command from PropertyEventListener for: " + propName);
            CommandService.ExecuteCommand(cmd); // This will update the model and trigger listeners (incl. PropertySheet repaint)
        }
    }
}