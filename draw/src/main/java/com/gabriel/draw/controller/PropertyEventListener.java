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

        // Prevent updates triggered by command execution/undo/redo
        if (CommandService.isExecutingCommand()) {
            return;
        }

        Command cmd = null; // Initialize command to null
        String propName = property.getName();
        Object propValue = property.getValue(); // Get value once

        try {
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
                // --- NEW PROPERTIES ---
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
            } else if (propName.equals("Use Gradient")) {
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
                    // Only create command if a shape is selected
                    if (appService.getSelectedShape() != null && oldValue != newValue) {
                        cmd = new SetVisibleCommand(appService, oldValue, newValue);
                    }
                }
                // --- END NEW PROPERTIES ---
            } else if (propName.equals("X Location")) {
                int oldValue = appService.getXLocation();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) { // Check if shape selected
                        int oldY = appService.getYLocation();
                        cmd = new SetPositionCommand(appService, oldValue, oldY, newValue, oldY);
                    }
                }
            } else if (propName.equals("Y Location")) {
                int oldValue = appService.getYLocation();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) { // Check if shape selected
                        int oldX = appService.getXLocation();
                        cmd = new SetPositionCommand(appService, oldX, oldValue, oldX, newValue);
                    }
                }
            } else if (propName.equals("Width")) {
                int oldValue = appService.getWidth();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) { // Check if shape selected
                        cmd = new SetWidthCommand(appService, oldValue, newValue);
                    }
                }
            } else if (propName.equals("Height")) {
                int oldValue = appService.getHeight();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) { // Check if shape selected
                        cmd = new SetHeightCommand(appService, oldValue, newValue);
                    }
                }
            } else if (propName.equals("Line Thickness")) {
                int oldValue = appService.getThickness();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue) {
                        cmd = new SetThicknessCommand(appService, oldValue, newValue);
                    }
                }
            } else if (propName.equals("Text")) {
                String oldValue = appService.getText();
                String newValue = (String) propValue;
                if (!Objects.equals(oldValue, newValue)) {
                    cmd = new SetTextCommand(appService, oldValue, (newValue != null ? newValue : ""));
                }
            } else if (propName.equals("Font Size")) {
                Font oldFont = appService.getFont();
                if (oldFont != null && propValue instanceof Integer) {
                    int oldValue = oldFont.getSize();
                    int newValue = (int) propValue;
                    if (oldValue != newValue) {
                        cmd = new SetFontSizeCommand(appService, oldValue, newValue, oldFont);
                    }
                }
            } else if (propName.equals("Font Family")) {
                Font oldFont = appService.getFont();
                if (oldFont != null) {
                    String oldValue = oldFont.getFamily();
                    String newValue = (String) propValue;
                    if (!Objects.equals(oldValue, newValue)) {
                        cmd = new SetFontFamilyCommand(appService, oldFont, newValue);
                    }
                }
            } else if (propName.equals("Font Style")) {
                Font oldFont = appService.getFont();
                if (oldFont != null && propValue instanceof Integer) {
                    int oldValue = oldFont.getStyle();
                    int newValue = (int) propValue;
                    if (oldValue != newValue) {
                        cmd = new SetFontStyleCommand(appService, oldValue, newValue, oldFont);
                    }
                }
            } else if (propName.equals("Current Shape Tool")) { // Changed name slightly
                ShapeMode oldValue = appService.getShapeMode();
                if (propValue instanceof ShapeMode) {
                    ShapeMode newValue = (ShapeMode) propValue;
                    if (oldValue != newValue) {
                        cmd = new SetShapeCommand(appService, oldValue, newValue);
                    }
                }
            }
        } catch (ClassCastException e) {
            System.err.println("PropertyEventListener: Error casting property value for '" + propName + "'. Value: " + propValue + ", Error: " + e.getMessage());
        } catch (Exception e) { // Catch other potential errors
            System.err.println("PropertyEventListener: Unexpected error processing property '" + propName + "': " + e.getMessage());
            e.printStackTrace();
        }

        // Execute command ONLY if one was created
        if (cmd != null) {
            CommandService.ExecuteCommand(cmd);
        }
    }
}
