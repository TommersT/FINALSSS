package com.gabriel.draw.controller;
import com.gabriel.draw.service.ImageFileService;
import com.gabriel.draw.service.XmlDocumentService;
import com.gabriel.draw.view.DrawingToolBar;
import com.gabriel.drawfx.ActionCommand;
import com.gabriel.drawfx.ShapeMode;
import com.gabriel.drawfx.ToolMode;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import lombok.Setter;
// FIX: Changed wrong import package from org.drjekyll.fontchooser to com.gabriel.fontchooser
// NOTE: fontchooser module is missing from repository, using basic font selection instead
// import com.gabriel.fontchooser.FontDialog;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class ActionController implements ActionListener {
    AppService appService;
    ImageFileService imageFileService;

    @Setter
    Component component;

    @Setter
    DrawingToolBar toolBar;

    @Setter
    DrawingController drawingController;

    Drawing drawing;
    @Setter
    JFrame frame;

    // FIX: Manually add setter that Lombok was supposed to create
    public void setComponent(Component component) {
        this.component = component;
    }

    // FIX: Manually add setter for frame that Lombok was also supposed to create
    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    public void setToolBar(DrawingToolBar toolBar) {
        this.toolBar = toolBar;
    }

    public void setDrawingController(DrawingController drawingController) {
        this.drawingController = drawingController;
    }

    public  ActionController(AppService appService){
        this.appService = appService;
        drawing = appService.getDrawing();
        imageFileService = new ImageFileService();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (ActionCommand.UNDO.equals(cmd)) {
            appService.undo();
        } else if (ActionCommand.REDO.equals(cmd)) {
            appService.redo();
        } else if (ActionCommand.LINE.equals(cmd)) {
            appService.setShapeMode(ShapeMode.Line);
            appService.setToolMode(ToolMode.DRAW);
            if (toolBar != null) toolBar.setActiveTool(cmd);
            if (drawingController != null) drawingController.updateStatusBarTool("Line");
        } else if (ActionCommand.RECT.equals(cmd)) {
            appService.setShapeMode(ShapeMode.Rectangle);
            appService.setToolMode(ToolMode.DRAW);
            if (toolBar != null) toolBar.setActiveTool(cmd);
            if (drawingController != null) drawingController.updateStatusBarTool("Rectangle");
        } else if (ActionCommand.ELLIPSE.equals(cmd)) {
            appService.setShapeMode(ShapeMode.Ellipse);
            appService.setToolMode(ToolMode.DRAW);
            if (toolBar != null) toolBar.setActiveTool(cmd);
            if (drawingController != null) drawingController.updateStatusBarTool("Ellipse");
        } else if (ActionCommand.IMAGE.equals(cmd)) {
            if(drawing.getImageFilename() == null) {
                imageFileService.setImage(drawing);
            }
            appService.setShapeMode(ShapeMode.Image);
            appService.setToolMode(ToolMode.DRAW);
            if (toolBar != null) toolBar.setActiveTool(cmd);
            if (drawingController != null) drawingController.updateStatusBarTool("Image");
        } else if (ActionCommand.COLOR.equals(cmd)) {
            Color currentColor = appService.getColor();
            if (currentColor == null) currentColor = Color.BLACK;
            Color color = JColorChooser.showDialog(component, "Select Fore Color", currentColor);
            if (color != null) {
                appService.setColor(color);
            }
        } else if (ActionCommand.TEXT.equals(cmd)) {
            appService.setShapeMode(ShapeMode.Text);
            appService.setToolMode(ToolMode.DRAW);
            if (toolBar != null) toolBar.setActiveTool(cmd);
            if (drawingController != null) drawingController.updateStatusBarTool("Text");
        } else if (ActionCommand.FILL.equals(cmd)) {
            Color currentFill = appService.getFill();
            if (currentFill == null) currentFill = Color.WHITE;
            Color color = JColorChooser.showDialog(component, "Select Fill Color", currentFill);
            if (color != null) {
                appService.setFill(color);
            }
        } else if (ActionCommand.SAVEAS.equals(cmd)) {
            FileDialog fDialog = new FileDialog(frame, "Save As", FileDialog.SAVE);
            fDialog.setFile("*.xml");
            fDialog.setVisible(true);
            if (fDialog.getFile() != null) {
                String path = fDialog.getDirectory() + fDialog.getFile();
                if (!path.toLowerCase().endsWith(".xml")) {
                    path += ".xml";
                }
                drawing.setFilename(path);
                appService.saveas(path);
            }

        } else if (ActionCommand.SELECT.equals(cmd)) {
            appService.clearSelections();
            appService.setShapeMode(ShapeMode.Select);
            appService.setToolMode(ToolMode.SELECT);
            if (toolBar != null) toolBar.setActiveTool(cmd);
            if (drawingController != null) drawingController.updateStatusBarTool("Select");
        } else if (ActionCommand.MOVE.equals(cmd)) {
            appService.setShapeMode(ShapeMode.Select);
            appService.setToolMode(ToolMode.MOVE);
            if (toolBar != null) toolBar.setActiveTool(cmd);
            if (drawingController != null) drawingController.updateStatusBarTool("Move");
        } else if (ActionCommand.SCALE.equals(cmd)) {
            appService.setShapeMode(ShapeMode.Select);
            appService.setToolMode(ToolMode.SCALE);
            if (toolBar != null) toolBar.setActiveTool(cmd);
            if (drawingController != null) drawingController.updateStatusBarTool("Scale");
        } else if (ActionCommand.SAVE.equals(cmd)) {
            String filename = drawing.getFilename();
            if (filename == null || filename.isEmpty()) {
                JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                fileChooser.addChoosableFileFilter(new FileFilter() {
                    public String getDescription() {
                        return "XML Documents (*.xml)";
                    }

                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        } else {
                            return f.getName().toLowerCase().endsWith(".xml");
                        }
                    }
                });
                int result = fileChooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    filename = fileChooser.getSelectedFile().getAbsolutePath();
                    if (!filename.toLowerCase().endsWith(".xml")) {
                        filename += ".xml";
                    }
                    drawing.setFilename(filename);
                } else {
                    return;
                }
            }
            appService.save();

            // TODO Insert the handler for the File menuitems.

        } else if (ActionCommand.OPEN.equals(cmd)) {
            JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            fileChooser.addChoosableFileFilter(new FileFilter() {
                public String getDescription() {
                    return "XML Documents (*.xml)";
                }

                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    } else {
                        return f.getName().toLowerCase().endsWith(".xml");
                    }
                }
            });
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filename = fileChooser.getSelectedFile().getAbsolutePath();
                drawing.setFilename(filename);
                appService.open(filename);
            }
        } else if (ActionCommand.NEW.equals(cmd)) {
            appService.newDrawing();
            drawing.setFilename(null);
        } else if (ActionCommand.DELETE.equals(cmd)) {
            Shape selectedShape = appService.getSelectedShape();
            if (selectedShape != null) {
                appService.delete(selectedShape);
            }
        } else if (ActionCommand.BRING_TO_FRONT.equals(cmd)) {
            Shape selectedShape = appService.getSelectedShape();
            if (selectedShape != null) {
                appService.bringToFront(selectedShape);
            }
        } else if (ActionCommand.SEND_TO_BACK.equals(cmd)) {
            Shape selectedShape = appService.getSelectedShape();
            if (selectedShape != null) {
                appService.sendToBack(selectedShape);
            }
        } else if (ActionCommand.BRING_FORWARD.equals(cmd)) {
            Shape selectedShape = appService.getSelectedShape();
            if (selectedShape != null) {
                appService.bringForward(selectedShape);
            }
        } else if (ActionCommand.SEND_BACKWARD.equals(cmd)) {
            Shape selectedShape = appService.getSelectedShape();
            if (selectedShape != null) {
                appService.sendBackward(selectedShape);
            }
        }
    }
}