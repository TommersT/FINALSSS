package com.gabriel.draw.controller;

import com.gabriel.draw.command.*;
import com.gabriel.draw.component.PropertySheet;
import com.gabriel.drawfx.command.CommandService;
import com.gabriel.property.event.PropertyEventAdapter;
import com.gabriel.property.property.Property;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.drawfx.command.Command;

import javax.swing.*;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;

public class PropertyEventListener extends PropertyEventAdapter {
    private AppService appService;
    private PropertySheet propertySheet;

    public PropertyEventListener(AppService appService, PropertySheet propertySheet) {
        this.appService = appService;
        this.propertySheet = propertySheet;
    }

    public AppService getAppService() {
        return appService;
    }

    @Override
    public void onPropertyUpdated(Property property) {

        if (CommandService.isExecutingCommand() || (propertySheet != null && propertySheet.isUpdatingFromController())) {
            return;
        }

        Command cmd = null;
        String propName = property.getName();
        Object propValue = property.getValue();

        try {

            if (propName.equals("Fore Color")) {
                Color oldValue = appService.getColor();
                Color newValue = (propValue instanceof Color) ? (Color) propValue : oldValue;
                if (!Objects.equals(oldValue, newValue)) {
                    cmd = new SetColorCommand(appService, oldValue, newValue);
                }
            } else if (propName.equals("Fill Color")) {
                Color oldValue = appService.getFill();
                Color newValue = (propValue instanceof Color) ? (Color) propValue : oldValue;
                if (!Objects.equals(oldValue, newValue)) {
                    cmd = new SetFillCommand(appService, oldValue, newValue);
                }
            } else if (propName.equals("Start Color")) {
                Color oldValue = appService.getStartColor();
                Color newValue = (propValue instanceof Color) ? (Color) propValue : oldValue;
                if (!Objects.equals(oldValue, newValue)) {
                    cmd = new SetStartColorCommand(appService, oldValue, newValue);
                }
            } else if (propName.equals("End Color")) {
                Color oldValue = appService.getEndColor();
                Color newValue = (propValue instanceof Color) ? (Color) propValue : oldValue;
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
                boolean oldValue = appService.isVisible();
                if (propValue instanceof Boolean) {
                    boolean newValue = (Boolean) propValue;
                    if (appService.getSelectedShape() != null && oldValue != newValue) {
                        cmd = new SetVisibleCommand(appService, oldValue, newValue);
                    }
                }
            } else if (propName.equals("X Location")) {
                int oldValue = appService.getXLocation();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) {
                        int currentY = appService.getYLocation();
                        cmd = new SetPositionCommand(appService, oldValue, currentY, newValue, currentY);
                    }
                }
            } else if (propName.equals("Y Location")) {
                int oldValue = appService.getYLocation();
                if (propValue instanceof Integer) {
                    int newValue = (int) propValue;
                    if (oldValue != newValue && appService.getSelectedShape() != null) {
                        int currentX = appService.getXLocation();
                        cmd = new SetPositionCommand(appService, currentX, oldValue, currentX, newValue);
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
                String newValue = (propValue instanceof String) ? (String) propValue : oldValue;
                if (!Objects.equals(oldValue, newValue) &&
                        (appService.getSelectedShape() == null || "Text".equals(appService.getSelectedShape().getClass().getSimpleName()))) {
                    cmd = new SetTextCommand(appService, oldValue, (newValue != null ? newValue : ""));
                }
            } else if (propName.equals("Font Size")) {
                Font oldFont = appService.getFont();
                if (oldFont != null && propValue instanceof Integer) {
                    int oldValue = oldFont.getSize();
                    int newValue = (int) propValue;
                    if (oldValue != newValue &&
                            (appService.getSelectedShape() == null || "Text".equals(appService.getSelectedShape().getClass().getSimpleName()))) {
                        cmd = new SetFontSizeCommand(appService, oldValue, newValue, oldFont);
                    }
                }
            } else if (propName.equals("Font Family")) {
                Font oldFont = appService.getFont();
                if (oldFont != null && propValue instanceof String) {
                    String oldValue = oldFont.getFamily();
                    String newValue = (String) propValue;
                    if (!Objects.equals(oldValue, newValue) &&
                            (appService.getSelectedShape() == null || "Text".equals(appService.getSelectedShape().getClass().getSimpleName()))) {
                        cmd = new SetFontFamilyCommand(appService, oldFont, newValue);
                    }
                }
            } else if (propName.equals("Font Style")) {
                Font oldFont = appService.getFont();
                if (oldFont != null && propValue instanceof Integer) {
                    int oldValue = oldFont.getStyle();
                    int newValue = (int) propValue;
                    if (oldValue != newValue &&
                            (appService.getSelectedShape() == null || "Text".equals(appService.getSelectedShape().getClass().getSimpleName()))) {
                        cmd = new SetFontStyleCommand(appService, oldValue, newValue, oldFont);
                    }
                }
            }

        } catch (ClassCastException e) {
            System.err.println("PropertyEventListener: Error casting property value for '" + propName + "'. Value: " + propValue + ", Error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("PropertyEventListener: Unexpected error processing property '" + propName + "': " + e.getMessage());
            e.printStackTrace();
        }

        if (cmd != null) {
            final Command finalCmd = cmd;
            if (SwingUtilities.isEventDispatchThread()) {
                CommandService.ExecuteCommand(finalCmd);
            } else {
                SwingUtilities.invokeLater(() -> CommandService.ExecuteCommand(finalCmd));
            }
        } else {
        }
    }
}
