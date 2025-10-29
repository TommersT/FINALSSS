// package com.gabriel.draw.controller; // Make sure package declaration is correct
package com.gabriel.draw.controller;


import com.gabriel.draw.command.*; // Import all commands
// No ShapeMode import needed here if not handling "Current Shape" tool
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
        // Ensure the service passed is the command-wrapped one
        this.appService = appService;
    }

    @Override
    public void onPropertyUpdated(Property property) {

        // *** Prevent updates triggered BY command execution/undo/redo ***
        if (CommandService.isExecutingCommand()) {
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
            else if (propName.equals("X Location")) {
                int oldValue = appService.getXLocation();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) {
                        int oldY = appService.getYLocation(); // Get current Y
                        cmd = new SetPositionCommand(appService, oldValue, oldY, newValue, oldY);
                    }
                }
            } else if (propName.equals("Y Location")) {
                int oldValue = appService.getYLocation();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) {
                        int oldX = appService.getXLocation(); // Get current X
                        cmd = new SetPositionCommand(appService, oldX, oldValue, oldX, newValue);
                    }
                }
            } else if (propName.equals("Width")) {
                int oldValue = appService.getWidth();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) {
                        cmd = new SetWidthCommand(appService, oldValue, newValue);
                    }
                }
            } else if (propName.equals("Height")) {
                int oldValue = appService.getHeight();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) {
                        cmd = new SetHeightCommand(appService, oldValue, newValue);
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
            } else if (propName.equals("Font Size")) {
                Font oldFont = appService.getFont(); // Get context
                if (oldFont != null && propValue instanceof Integer) {
                    int oldValue = oldFont.getSize();
                    int newValue = (int) propValue;
                    if (oldValue != newValue) {
                        // Pass oldFont context to command
                        cmd = new SetFontSizeCommand(appService, oldValue, newValue, oldFont);
                    }
                }
            } else if (propName.equals("Font Family")) {
                Font oldFont = appService.getFont(); // Get context
                if (oldFont != null) {
                    String oldValue = oldFont.getFamily();
                    String newValue = (String) propValue;
                    if (!Objects.equals(oldValue, newValue)) {
                        // Pass oldFont context to command
                        cmd = new SetFontFamilyCommand(appService, oldFont, newValue);
                    }
                }
            } else if (propName.equals("Font Style")) {
                Font oldFont = appService.getFont(); // Get context
                // Value comes from SelectionProperty<Integer>
                if (oldFont != null && propValue instanceof Integer) {
                    int oldValue = oldFont.getStyle();
                    int newValue = (int) propValue;
                    if (oldValue != newValue) {
                        // Pass oldFont context to command
                        cmd = new SetFontStyleCommand(appService, oldValue, newValue, oldFont);
                    }
                }
            }
            // Ignore "Object Type" as it's read-only
            // Add handlers for any other editable properties you have

        } catch (ClassCastException e) {
            System.err.println("PropertyEventListener: Error casting property value for '" + propName + "'. Value: " + propValue + ", Error: " + e.getMessage());
        } catch (Exception e) { // Catch other potential errors
            System.err.println("PropertyEventListener: Unexpected error processing property '" + propName + "': " + e.getMessage());
            e.printStackTrace();
        }

        // Execute command ONLY if one was created
        if (cmd != null) {
            CommandService.ExecuteCommand(cmd); // This updates the model AND triggers listeners (repaint, property sheet update)
        }
    }
}