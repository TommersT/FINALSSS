package com.gabriel.draw.view;

import com.gabriel.draw.component.PropertySheet;
import com.gabriel.draw.controller.ActionController;
import com.gabriel.draw.controller.DrawingController;
import com.gabriel.draw.controller.DrawingWindowController;
import com.gabriel.draw.service.DrawingAppService;
import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.ShapeMode;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.drawfx.command.CommandService;
import com.gabriel.property.PropertyOptions;
import com.gabriel.property.event.PropertyEventAdapter;
import com.gabriel.property.property.Property;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class DrawingFrame extends JFrame {

    Drawing drawing;
    DrawingAppService drawingAppService;
    AppService appService;
    DrawingFrame drawingFrame;
    Container pane;
    private PropertySheet propertySheet;
    ActionController actionListener;
    DrawingMenuBar drawingMenuBar;
    DrawingToolBar drawingToolBar;
    DrawingView drawingView;
    DrawingController drawingController;
    JScrollPane jScrollPane;
    DrawingStatusPanel drawingStatusPanel;
    DrawingWindowController drawingWindowController;
    public DrawingFrame() {

        drawing = new Drawing();
        drawingAppService = new DrawingAppService();
        appService = DrawingCommandAppService.getInstance(drawingAppService);

        pane = getContentPane();
        setLayout(new BorderLayout());

        actionListener = new ActionController(appService);
        drawingMenuBar = new DrawingMenuBar( actionListener);

        setJMenuBar(drawingMenuBar);

        drawingMenuBar.setVisible(true);


        drawingToolBar = new DrawingToolBar(actionListener);
        drawingToolBar.setVisible(true);
        actionListener.setToolBar(drawingToolBar);

        drawingView = new DrawingView(appService);
        actionListener.setComponent(drawingView);


        drawingController = new DrawingController(appService, drawingView);
        drawingController.setDrawingView(drawingView);
        actionListener.setDrawingController(drawingController);

        drawingView.addMouseMotionListener(drawingController);
        drawingView.addMouseListener(drawingController);
        drawingView.setPreferredSize(new Dimension(4095, 8192));

        jScrollPane = new JScrollPane(drawingView);
        jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        drawingStatusPanel = new DrawingStatusPanel();
        drawingController.setDrawingStatusPanel(drawingStatusPanel);

        pane.add(drawingToolBar, BorderLayout.PAGE_START);
        pane.add(jScrollPane, BorderLayout.CENTER );
        pane.add(drawingStatusPanel, BorderLayout.PAGE_END);

        drawingAppService.setDrawingView(drawingView);

        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500,500);

        drawingWindowController = new DrawingWindowController(appService);
        this.addWindowListener(drawingWindowController);
        this.addWindowFocusListener(drawingWindowController);
        this.addWindowStateListener(drawingWindowController);
        buildGUI(pane);
        drawingController.setPropertySheet(propertySheet);

        CommandService.addListener((canUndo, canRedo) -> {
            drawingToolBar.updateUndoRedoState(canUndo, canRedo);
        });

        drawingToolBar.setActiveTool(com.gabriel.drawfx.ActionCommand.SELECT);
    }

    public void buildGUI(Container pane){
        buildPropertyTable(pane);
        JScrollPane scrollPane = new JScrollPane(propertySheet);
        pane.add(scrollPane, BorderLayout.LINE_END);
        pack();
    }


    void buildPropertyTable(Container pane) {
        String[] headers = new String[]{"Property", "value"};
        Color backgroundColor = new Color(255, 255, 255);
        Color invalidColor = new Color(255, 179, 176);
        int rowHeight = 30;
        PropertyOptions options = new PropertyOptions(headers, backgroundColor, invalidColor, rowHeight);

        propertySheet = new PropertySheet(new PropertyOptions.Builder().build());

        // Use the correct, centralized event listener
        propertySheet.addEventListener(new com.gabriel.draw.controller.PropertyEventListener(appService));

        propertySheet.populateTable(appService);

        repaint();
    }

    // The redundant inner class EventListener has been REMOVED.
}