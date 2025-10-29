package com.gabriel.draw.view;

import com.gabriel.draw.component.PropertySheet;
import com.gabriel.draw.controller.ActionController;
import com.gabriel.draw.controller.DrawingController;
import com.gabriel.draw.controller.DrawingWindowController;
import com.gabriel.draw.service.DrawingAppService;
import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.drawfx.command.CommandService; // Keep CommandService import
import com.gabriel.property.PropertyOptions;
import com.gabriel.draw.controller.PropertyEventListener; // Keep this listener import

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent; // <<< ADDED Missing Import

public class DrawingFrame extends JFrame {

    // <<< --- RESTORED FIELD DECLARATIONS --- >>>
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
    // <<< --- END RESTORED FIELD DECLARATIONS --- >>>

    public DrawingFrame() {
        setTitle("GoDraw Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Set early

        // --- Model and Service Initialization ---
        drawingAppService = new DrawingAppService();
        appService = DrawingCommandAppService.getInstance(drawingAppService);

        // --- Basic Frame Setup ---
        pane = getContentPane();
        // Use BorderLayout with gaps
        setLayout(new BorderLayout(5, 5));
        // Add padding around the entire frame content
        ((JPanel) pane).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // --- Controller Initialization ---
        actionListener = new ActionController(appService);
        // Defer setting view/toolbar/propertySheet until they are created
        drawingController = new DrawingController(appService, null);
        drawingWindowController = new DrawingWindowController(appService);

        // --- UI Component Initialization & Layout (Order Matters) ---

        // 1. Menu Bar
        drawingMenuBar = new DrawingMenuBar(actionListener);
        setJMenuBar(drawingMenuBar);

        // 2. Tool Bar (North)
        drawingToolBar = new DrawingToolBar(actionListener);
        pane.add(drawingToolBar, BorderLayout.NORTH);

        // 3. Status Bar (South)
        drawingStatusPanel = new DrawingStatusPanel();
        pane.add(drawingStatusPanel, BorderLayout.SOUTH);
        drawingController.setDrawingStatusPanel(drawingStatusPanel); // Link controller

        // 4. Drawing View & Scroll Pane (Center)
        drawingView = new DrawingView(appService);
        drawingView.setPreferredSize(new Dimension(2000, 1500)); // Large canvas size
        drawingScrollPane = new JScrollPane(drawingView);
        // Configure scroll pane behavior
        drawingScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        drawingScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        drawingScrollPane.getVerticalScrollBar().setUnitIncrement(20); // Faster scrolling
        drawingScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        pane.add(drawingScrollPane, BorderLayout.CENTER);

        // 5. Property Sheet & Scroll Pane (East) - Build fully before adding
        buildPropertyTableAndScrollPane(); // Helper does full setup
        pane.add(propertyScrollPane, BorderLayout.EAST); // Add the configured scroll pane

        // --- Link Controllers to Components (AFTER creation) ---
        drawingView.setDrawingController(drawingController); // Link View -> Controller
        drawingController.setDrawingView(drawingView);     // Link Controller -> View
        drawingController.setPropertySheet(propertySheet); // Link Controller -> PropertySheet
        actionListener.setToolBar(drawingToolBar);
        actionListener.setDrawingController(drawingController); // Link controllers
        actionListener.setComponent(drawingView); // For dialog parent
        actionListener.setFrame(this);            // For dialog parent

        // Link Drawing Service to View (for repaint trigger)
        drawingAppService.setDrawingView(drawingView);

        // --- Add Window Listeners ---
        this.addWindowListener(drawingWindowController);
        this.addWindowFocusListener(drawingWindowController);
        this.addWindowStateListener(drawingWindowController);

        // --- Command Service Listener ---
        CommandService.addListener((canUndo, canRedo) -> {
            Runnable updateTask = () -> {
                if (drawingToolBar != null) drawingToolBar.updateUndoRedoState(canUndo, canRedo);
                if (drawingMenuBar != null) drawingMenuBar.updateUndoRedoState(canUndo, canRedo);
                // Populate table AFTER command finishes to reflect new state
                if (propertySheet != null) propertySheet.populateTable(appService);
                if (drawingView != null) drawingView.repaint();
            };
            if (SwingUtilities.isEventDispatchThread()) {
                updateTask.run();
            } else {
                SwingUtilities.invokeLater(updateTask);
            }
        });

        // --- Final Frame Configuration ---
        // Set size AFTER adding components if not using pack()
        setSize(1200, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null); // Center on screen

        // --- Set Initial Tool (After everything is linked) ---
        SwingUtilities.invokeLater(() -> { // Ensure initial setup happens on EDT
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, com.gabriel.drawfx.ActionCommand.SELECT));
            // Force an initial population of the property table
            if (propertySheet != null) {
                propertySheet.populateTable(appService);
            }
        });

    }

    // Helper: Builds PropertySheet AND its JScrollPane
    void buildPropertyTableAndScrollPane() {
        PropertyOptions options = new PropertyOptions.Builder().build();

        // 1. Create the PropertySheet instance (constructor builds structure)
        propertySheet = new PropertySheet(options);

        // 2. Add the correct event listener instance
        propertySheet.addEventListener(new PropertyEventListener(appService)); // Use the command-handling listener

        // 3. Create the JScrollPane *containing* the propertySheet
        propertyScrollPane = new JScrollPane(propertySheet);
        propertyScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        propertyScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // 4. Set preferred and minimum size on the SCROLL PANE
        propertyScrollPane.setPreferredSize(new Dimension(280, 500)); // Hint for BorderLayout
        propertyScrollPane.setMinimumSize(new Dimension(220, 300));   // Minimum width for the panel

        // Optional: Set viewport background
        propertyScrollPane.getViewport().setBackground(Color.WHITE);
    }

    // Main method for testing
    public static void main(String[] args) {
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

        SwingUtilities.invokeLater(() -> {
            DrawingFrame frame = new DrawingFrame();
            frame.setVisible(true);
        });
    }
}