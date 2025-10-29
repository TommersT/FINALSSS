package com.gabriel.draw.view;

import com.gabriel.draw.component.PropertySheet;
import com.gabriel.draw.controller.ActionController;
import com.gabriel.draw.controller.DrawingController;
import com.gabriel.draw.controller.DrawingWindowController;
import com.gabriel.draw.service.DrawingAppService;
import com.gabriel.draw.service.DrawingCommandAppService;
// No longer importing Drawing directly, get from service
//import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.drawfx.command.CommandService;
import com.gabriel.property.PropertyOptions;
import com.gabriel.draw.controller.PropertyEventListener; // Import the listener

import javax.swing.*;
import java.awt.*;

public class DrawingFrame extends JFrame {

    // Keep fields package-private or private unless needed otherwise
    DrawingAppService drawingAppService; // Base service implementation
    AppService appService; // Command-wrapped service
    Container pane;
    PropertySheet propertySheet; // Made accessible for listener update
    ActionController actionListener;
    DrawingMenuBar drawingMenuBar;
    DrawingToolBar drawingToolBar;
    DrawingView drawingView; // Made accessible for listener update
    DrawingController drawingController;
    JScrollPane drawingScrollPane; // Renamed for clarity
    DrawingStatusPanel drawingStatusPanel;
    DrawingWindowController drawingWindowController;
    JScrollPane propertyScrollPane; // Keep reference

    public DrawingFrame() {
        setTitle("GoDraw Application");

        // --- Model and Service Initialization ---
        drawingAppService = new DrawingAppService();
        appService = DrawingCommandAppService.getInstance(drawingAppService);

        // --- Basic Frame Setup ---
        pane = getContentPane();
        setLayout(new BorderLayout(5, 5)); // Use BorderLayout

        // --- Controller Initialization ---
        actionListener = new ActionController(appService);
        drawingController = new DrawingController(appService, null); // View set later
        drawingWindowController = new DrawingWindowController(appService);

        // --- UI Component Initialization ---
        drawingMenuBar = new DrawingMenuBar(actionListener);
        setJMenuBar(drawingMenuBar);

        drawingToolBar = new DrawingToolBar(actionListener);
        actionListener.setToolBar(drawingToolBar);
        actionListener.setDrawingController(drawingController); // Link controllers

        drawingView = new DrawingView(appService);
        drawingView.setDrawingController(drawingController); // Link View -> Controller
        drawingController.setDrawingView(drawingView);     // Link Controller -> View

        drawingView.setPreferredSize(new Dimension(2000, 1500)); // Canvas size
        drawingScrollPane = new JScrollPane(drawingView); // Use new name
        drawingScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        drawingScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        drawingScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        drawingScrollPane.getHorizontalScrollBar().setUnitIncrement(16);


        drawingStatusPanel = new DrawingStatusPanel();
        drawingController.setDrawingStatusPanel(drawingStatusPanel); // Link Controller -> StatusPanel

        // --- Property Sheet Initialization ---
        buildPropertyTable(); // Creates and sets 'propertySheet' field
        drawingController.setPropertySheet(propertySheet); // Link Controller -> PropertySheet
        propertyScrollPane = new JScrollPane(propertySheet); // Assign to field
        propertyScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // --- FIX: Set a fixed preferred width and let height be flexible ---
        // Give it a definite preferred size that BorderLayout can work with initially.
        // Height will still be determined by BorderLayout in the EAST position.
        propertyScrollPane.setPreferredSize(new Dimension(250, 400)); // Set width and a reasonable initial height
        propertyScrollPane.setMinimumSize(new Dimension(150, 100)); // Prevent it from becoming too small
        // --- END FIX ---


        // --- Layout Components ---
        pane.add(drawingToolBar, BorderLayout.NORTH);
        pane.add(drawingScrollPane, BorderLayout.CENTER); // Use new name
        pane.add(propertyScrollPane, BorderLayout.EAST);
        pane.add(drawingStatusPanel, BorderLayout.SOUTH);

        // --- Link Services and Listeners ---
        drawingAppService.setDrawingView(drawingView);
        actionListener.setComponent(drawingView);
        actionListener.setFrame(this);

        // Window Listeners
        this.addWindowListener(drawingWindowController);
        this.addWindowFocusListener(drawingWindowController);
        this.addWindowStateListener(drawingWindowController);

        // Command Service Listener
        CommandService.addListener((canUndo, canRedo) -> {
            if (drawingToolBar != null) drawingToolBar.updateUndoRedoState(canUndo, canRedo);
            if (drawingMenuBar != null) drawingMenuBar.updateUndoRedoState(canUndo, canRedo);
            if (propertySheet != null) SwingUtilities.invokeLater(() -> propertySheet.populateTable(appService));
            if (drawingView != null) SwingUtilities.invokeLater(() -> drawingView.repaint());
        });

        // --- Final Frame Configuration ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768); // Set a good default size
        setMinimumSize(new Dimension(800, 600)); // Increase minimum size
        setLocationRelativeTo(null); // Center on screen

        // --- Ensure Layout is Validated ---
        pane.revalidate(); // Re-calculate layout
        pane.repaint(); // Redraw
        // --- END Ensure ---


        // Set initial tool after setup is complete
        if (drawingToolBar != null) {
            drawingToolBar.setActiveTool(com.gabriel.drawfx.ActionCommand.SELECT);
        }
        appService.setToolMode(com.gabriel.drawfx.ToolMode.SELECT);
        drawingController.updateStatusBarTool("Select");
    }

    // Helper to build and configure the property sheet
    void buildPropertyTable() {
        PropertyOptions options = new PropertyOptions.Builder()
                .build();
        propertySheet = new PropertySheet(options);
        // --- FIX: Ensure table has a minimum size ---
        propertySheet.setMinimumSize(new Dimension(150, 300)); // Prevent table itself from collapsing
        // --- END FIX ---
        propertySheet.addEventListener(new PropertyEventListener(appService));
        propertySheet.populateTable(appService);
    }

    // Main method for testing this frame directly (optional)
    public static void main(String[] args) {
        // Apply Look and Feel early
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ex) { System.err.println("Failed to set LookAndFeel."); }
        }

        // Run GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            DrawingFrame frame = new DrawingFrame();
            frame.setVisible(true); // Make visible here for testing
        });
    }
}

