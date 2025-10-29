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
        drawingScrollPane = new JScrollPane(drawingView);
        drawingScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        drawingScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        drawingScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        drawingScrollPane.getHorizontalScrollBar().setUnitIncrement(16);


        drawingStatusPanel = new DrawingStatusPanel();
        drawingController.setDrawingStatusPanel(drawingStatusPanel); // Link Controller -> StatusPanel

        // --- Property Sheet Initialization & Layout ---
        buildPropertyTable(); // Creates and fully configures 'propertySheet'
        drawingController.setPropertySheet(propertySheet); // Link Controller -> PropertySheet

        // Create the JScrollPane AFTER propertySheet is fully built and configured
        propertyScrollPane = new JScrollPane(propertySheet);
        propertyScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        propertyScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        propertyScrollPane.setPreferredSize(new Dimension(280, 500)); // Size hint for BorderLayout
        propertyScrollPane.setMinimumSize(new Dimension(200, 300));
        propertyScrollPane.getViewport().setBackground(Color.WHITE);

        // --- Layout Components ---
        pane.add(drawingToolBar, BorderLayout.NORTH);
        pane.add(drawingScrollPane, BorderLayout.CENTER);
        pane.add(propertyScrollPane, BorderLayout.EAST); // Add the scroll pane containing the configured table
        pane.add(drawingStatusPanel, BorderLayout.SOUTH);

        // --- Link Services and Listeners ---
        drawingAppService.setDrawingView(drawingView);
        actionListener.setComponent(drawingView);
        actionListener.setFrame(this);

        // Window Listeners
        this.addWindowListener(drawingWindowController);
        this.addWindowFocusListener(drawingWindowController);
        this.addWindowStateListener(drawingWindowController);

        // Command Service Listener (ensure updates run on EDT)
        CommandService.addListener((canUndo, canRedo) -> {
            SwingUtilities.invokeLater(() -> {
                if (drawingToolBar != null) drawingToolBar.updateUndoRedoState(canUndo, canRedo);
                if (drawingMenuBar != null) drawingMenuBar.updateUndoRedoState(canUndo, canRedo);
                if (propertySheet != null) propertySheet.populateTable(appService);
                if (drawingView != null) drawingView.repaint();
            });
        });

        // --- Final Frame Configuration ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800); // Set desired size
        setMinimumSize(new Dimension(900, 600)); // Set minimum size
        setLocationRelativeTo(null); // Center on screen
        // Layout happens implicitly when setVisible(true) is called

        // --- Set Initial Tool ---
        if (drawingToolBar != null) {
            drawingToolBar.setActiveTool(com.gabriel.drawfx.ActionCommand.SELECT);
        }
        appService.setToolMode(com.gabriel.drawfx.ToolMode.SELECT);
        if (drawingController != null) {
            drawingController.updateStatusBarTool("Select");
        }
    }

    // Helper to build and configure the property sheet
    void buildPropertyTable() {
        PropertyOptions options = new PropertyOptions.Builder().build();
        // 1. Create the PropertySheet (constructor calls initializeProperties)
        propertySheet = new PropertySheet(options);

        // 2. Configure the table ITSELF *after* properties are initialized
        propertySheet.setPreferredScrollableViewportSize(new Dimension(260, 400)); // Suggest viewport size to scroll pane
        propertySheet.setFillsViewportHeight(true); // Allow table to use vertical space
        propertySheet.setMinimumSize(new Dimension(200, 300)); // Minimum size for the table
        propertySheet.setBackground(Color.WHITE); // Make sure it's visible
        propertySheet.setOpaque(true);
        propertySheet.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS); // Sensible default resize mode

        // 3. Force the table to calculate its layout based on the model/columns
        propertySheet.revalidate(); // *** ADDED: Explicit revalidation ***

        // 4. Add listener and populate (redundant populate, initializeProperties did it, but safe)
        propertySheet.addEventListener(new PropertyEventListener(appService));
        propertySheet.populateTable(appService); // Ensure values are current
    }

    // Main method for testing this frame directly
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
            frame.setVisible(true); // Make visible - this triggers the layout process
        });
    }
}