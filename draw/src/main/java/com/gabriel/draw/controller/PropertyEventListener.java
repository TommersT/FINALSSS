// tommerst/finalsss/FINALSSS-4b53253892a92ed882030feb653ef76e91b6ab5b/draw/src/main/java/com/gabriel/draw/controller/PropertyEventListener.java
package com.gabriel.draw.controller;

import com.gabriel.draw.command.*; // Import all commands from the package
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

        // *** NEW: Check if another command is currently executing ***
        if (CommandService.isExecutingCommand()) {
            return; // Ignore updates triggered by undo/redo/execute itself
        }
        // *** END NEW ***

        Command cmd = null; // Initialize command to null

        try {
            // --- Existing checks remain the same ---
            if (property.getName().equals("Fill color")) {
                Color oldValue = appService.getFill();
                Color newValue = (Color) property.getValue();
                if (!Objects.equals(oldValue, newValue)) {
                    cmd = new SetFillCommand(appService, oldValue, newValue);
                }
            } else if (property.getName().equals("Fore color")) {
                Color oldValue = appService.getColor();
                Color newValue = (Color) property.getValue();
                if (!Objects.equals(oldValue, newValue)) {
                    cmd = new SetColorCommand(appService, oldValue, newValue);
                }
            } else if (property.getName().equals("X Location")) {
                int oldValue = appService.getXLocation();
                if (property.getValue() instanceof Integer) {
                    int newValue = (int) property.getValue();
                    if (oldValue != newValue) {
                        int oldY = appService.getYLocation();
                        cmd = new SetPositionCommand(appService, oldValue, oldY, newValue, oldY);
                    }
                }
            } else if (property.getName().equals("Y Location")) {
                int oldValue = appService.getYLocation();
                if (property.getValue() instanceof Integer) {
                    int newValue = (int) property.getValue();
                    if (oldValue != newValue) {
                        int oldX = appService.getXLocation();
                        cmd = new SetPositionCommand(appService, oldX, oldValue, oldX, newValue);
                    }
                }
            } else if (property.getName().equals("Width")) {
                int oldValue = appService.getWidth();
                if (property.getValue() instanceof Integer) {
                    int newValue = (int) property.getValue();
                    if (oldValue != newValue) {
                        cmd = new SetWidthCommand(appService, oldValue, newValue);
                    }
                }
            } else if (property.getName().equals("Height")) {
                int oldValue = appService.getHeight();
                if (property.getValue() instanceof Integer) {
                    int newValue = (int) property.getValue();
                    if (oldValue != newValue) {
                        cmd = new SetHeightCommand(appService, oldValue, newValue);
                    }
                }
            } else if (property.getName().equals("Line Thickness")) {
                int oldValue = appService.getThickness();
                if (property.getValue() instanceof Integer) {
                    int newValue = (int) property.getValue();
                    if (oldValue != newValue) {
                        cmd = new SetThicknessCommand(appService, oldValue, newValue);
                    }
                }
            } else if (property.getName().equals("Text")) {
                String oldValue = appService.getText();
                String newValue = (String) property.getValue();
                if (!Objects.equals(oldValue, newValue)) {
                    cmd = new SetTextCommand(appService, oldValue, (newValue != null ? newValue : ""));
                }
            } else if (property.getName().equals("Font size")) {
                Font oldFont = appService.getFont();
                if (oldFont != null && property.getValue() instanceof Integer) {
                    int oldValue = oldFont.getSize();
                    int newValue = (int) property.getValue();
                    if (oldValue != newValue) {
                        cmd = new SetFontSizeCommand(appService, oldValue, newValue, oldFont);
                    }
                }
            } else if (property.getName().equals("Font Family")) {
                Font oldFont = appService.getFont();
                if (oldFont != null) {
                    String oldValue = oldFont.getFamily();
                    String newValue = (String) property.getValue();
                    if (!Objects.equals(oldValue, newValue)) {
                        cmd = new SetFontFamilyCommand(appService, oldFont, newValue);
                    }
                }
            } else if (property.getName().equals("Font Style")) {
                Font oldFont = appService.getFont();
                if (oldFont != null && property.getValue() instanceof Integer) {
                    int oldValue = oldFont.getStyle();
                    int newValue = (int) property.getValue();
                    if (oldValue != newValue) {
                        cmd = new SetFontStyleCommand(appService, oldValue, newValue, oldFont);
                    }
                }
            } else if (property.getName().equals("Current Shape")) {
                ShapeMode oldValue = appService.getShapeMode();
                if (property.getValue() instanceof ShapeMode) {
                    ShapeMode newValue = (ShapeMode) property.getValue();
                    if (oldValue != newValue) {
                        cmd = new SetShapeCommand(appService, oldValue, newValue);
                    }
                }
            }
        } catch (ClassCastException e) {
            System.err.println("PropertyEventListener: Error casting property value for '" + property.getName() + "'. Value: " + property.getValue() + ", Error: " + e.getMessage());
        }

        // Execute command ONLY if one was created AND we aren't already executing another command
        if (cmd != null) {
            CommandService.ExecuteCommand(cmd);
        }
    }
}