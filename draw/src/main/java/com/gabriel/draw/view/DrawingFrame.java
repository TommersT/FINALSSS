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
    JScrollPane jScrollPane;
    DrawingStatusPanel drawingStatusPanel;
    DrawingWindowController drawingWindowController;

    public DrawingFrame() {
        setTitle("GoDraw Application");

        // --- Model and Service Initialization ---
        // Service manages its own drawing instance now
        drawingAppService = new DrawingAppService();
        appService = DrawingCommandAppService.getInstance(drawingAppService);

        // --- Basic Frame Setup ---
        pane = getContentPane();
        setLayout(new BorderLayout(5, 5));

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
        jScrollPane = new JScrollPane(drawingView);
        jScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        drawingStatusPanel = new DrawingStatusPanel();
        drawingController.setDrawingStatusPanel(drawingStatusPanel); // Link Controller -> StatusPanel

        // --- Property Sheet Initialization ---
        buildPropertyTable(); // Creates and sets 'propertySheet' field
        drawingController.setPropertySheet(propertySheet); // Link Controller -> PropertySheet
        JScrollPane propertyScrollPane = new JScrollPane(propertySheet);
        propertyScrollPane.setPreferredSize(new Dimension(250, 0)); // Width for property sheet


        // --- Layout Components ---
        pane.add(drawingToolBar, BorderLayout.NORTH);
        pane.add(jScrollPane, BorderLayout.CENTER);
        pane.add(propertyScrollPane, BorderLayout.EAST);
        pane.add(drawingStatusPanel, BorderLayout.SOUTH);

        // --- Link Services and Listeners ---
        drawingAppService.setDrawingView(drawingView); // Link base service -> View (needed for triggerRepaint)
        actionListener.setComponent(drawingView); // Parent component for dialogs
        actionListener.setFrame(this); // Frame reference

        // Window Listeners
        this.addWindowListener(drawingWindowController);
        this.addWindowFocusListener(drawingWindowController);
        this.addWindowStateListener(drawingWindowController);

        // Command Service Listener for Undo/Redo button state AND UI Updates
        CommandService.addListener((canUndo, canRedo) -> {
            // Update buttons and menu items state
            if (drawingToolBar != null) {
                drawingToolBar.updateUndoRedoState(canUndo, canRedo);
            }
            if (drawingMenuBar != null) {
                drawingMenuBar.updateUndoRedoState(canUndo, canRedo);
            }

            // *** CRUCIAL: Update Property Sheet after Undo/Redo ***
            if (propertySheet != null) {
                // Populate based on the current model state AFTER undo/redo finished
                // Ensure this runs on the Event Dispatch Thread (EDT) for safety
                SwingUtilities.invokeLater(() -> propertySheet.populateTable(appService));
            }

            // *** CRUCIAL: REPAINT the view after Undo/Redo ***
            // Ensures selection handles, shape positions/colors, etc., are correct
            if (drawingView != null) {
                // Ensure repaint also runs on the EDT
                SwingUtilities.invokeLater(() -> drawingView.repaint());
            }
        });

        // --- Final Frame Configuration ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack(); // Adjust frame size to fit components
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(null); // Center on screen
        // setVisible(true); // Should be done by Main or Splash screen

        // Set initial tool after setup is complete
        drawingToolBar.setActiveTool(com.gabriel.drawfx.ActionCommand.SELECT);
        appService.setToolMode(com.gabriel.drawfx.ToolMode.SELECT); // Sync service state
        drawingController.updateStatusBarTool("Select"); // Update status bar text
    }

    // Helper to build and configure the property sheet
    void buildPropertyTable() {
        PropertyOptions options = new PropertyOptions.Builder()
                // Add any custom options here if needed
                .build();
        propertySheet = new PropertySheet(options);
        // Link the listener that creates commands when properties are edited *in the sheet*
        propertySheet.addEventListener(new PropertyEventListener(appService));
        // Initial population (shows global properties or empty if nothing selected)
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
            frame.setVisible(true);
        });
    }
}