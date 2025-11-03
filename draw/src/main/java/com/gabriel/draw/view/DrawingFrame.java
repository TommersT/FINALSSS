package com.gabriel.draw.view;

import com.gabriel.draw.component.PropertySheet;
import com.gabriel.draw.controller.ActionController;
import com.gabriel.draw.controller.DrawingController;
import com.gabriel.draw.controller.DrawingWindowController;
import com.gabriel.draw.service.DrawingAppService;
import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.drawfx.command.CommandService;
import com.gabriel.property.PropertyOptions;
import com.gabriel.draw.controller.PropertyEventListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class DrawingFrame extends JFrame {

    DrawingAppService drawingAppService;
    AppService appService;
    Container pane;
    PropertySheet propertySheet;
    ActionController actionListener;
    DrawingMenuBar drawingMenuBar;
    DrawingToolBar drawingToolBar;
    DrawingView drawingView;
    DrawingController drawingController;
    JScrollPane drawingScrollPane;
    DrawingStatusPanel drawingStatusPanel;
    DrawingWindowController drawingWindowController;
    JScrollPane propertyScrollPane;

    public DrawingFrame() {
        setTitle("GoDraw Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        drawingAppService = new DrawingAppService();
        appService = DrawingCommandAppService.getInstance(drawingAppService);

        pane = getContentPane();
        setLayout(new BorderLayout(5, 5));
        ((JPanel) pane).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        actionListener = new ActionController(appService);
        drawingController = new DrawingController(appService, null);
        drawingWindowController = new DrawingWindowController(appService);

        drawingMenuBar = new DrawingMenuBar(actionListener);
        setJMenuBar(drawingMenuBar);

        drawingToolBar = new DrawingToolBar(actionListener);
        pane.add(drawingToolBar, BorderLayout.NORTH);

        drawingStatusPanel = new DrawingStatusPanel();
        pane.add(drawingStatusPanel, BorderLayout.SOUTH);
        drawingController.setDrawingStatusPanel(drawingStatusPanel);

        drawingView = new DrawingView(appService);
        drawingView.setPreferredSize(new Dimension(2000, 1500));
        drawingScrollPane = new JScrollPane(drawingView);
        drawingScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        drawingScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        drawingScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        drawingScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        pane.add(drawingScrollPane, BorderLayout.CENTER);

        buildPropertyTableAndScrollPane();
        pane.add(propertyScrollPane, BorderLayout.EAST);

        drawingView.setDrawingController(drawingController);
        drawingController.setDrawingView(drawingView);
        drawingController.setPropertySheet(propertySheet);
        actionListener.setToolBar(drawingToolBar);
        actionListener.setDrawingController(drawingController);
        actionListener.setComponent(drawingView);
        actionListener.setFrame(this);

        drawingAppService.setDrawingView(drawingView);

        this.addWindowListener(drawingWindowController);
        this.addWindowFocusListener(drawingWindowController);
        this.addWindowStateListener(drawingWindowController);

        CommandService.addListener((canUndo, canRedo) -> {
            Runnable updateTask = () -> {
                if (drawingToolBar != null) drawingToolBar.updateUndoRedoState(canUndo, canRedo);
                if (drawingMenuBar != null) drawingMenuBar.updateUndoRedoState(canUndo, canRedo);
                if (propertySheet != null) propertySheet.populateTable(appService);
                if (drawingView != null) drawingView.repaint();
            };
            if (SwingUtilities.isEventDispatchThread()) {
                updateTask.run();
            } else {
                SwingUtilities.invokeLater(updateTask);
            }
        });

        setSize(1200, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        SwingUtilities.invokeLater(() -> {
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, com.gabriel.drawfx.ActionCommand.SELECT));
            if (propertySheet != null) {
                propertySheet.populateTable(appService);
            }
            if (drawingView != null) drawingView.repaint();
        });
    }

    void buildPropertyTableAndScrollPane() {
        PropertyOptions options = new PropertyOptions.Builder().build();
        propertySheet = new PropertySheet(options);
        propertySheet.addEventListener(new PropertyEventListener(appService, propertySheet));
        propertyScrollPane = new JScrollPane(propertySheet);
        propertyScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        propertyScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        propertyScrollPane.setPreferredSize(new Dimension(280, 500));
        propertyScrollPane.setMinimumSize(new Dimension(220, 300));
        propertyScrollPane.getViewport().setBackground(Color.WHITE);
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                System.err.println("Failed to set LookAndFeel.");
            }
        }

        SwingUtilities.invokeLater(() -> {
            DrawingFrame frame = new DrawingFrame();
            frame.setVisible(true);
        });
    }
}
