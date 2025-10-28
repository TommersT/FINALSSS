// tommerst/finalsss/FINALSSS-88c0dd560a5808d9ed1c9bce7a17f6f1ba863bfb/draw/src/main/java/com/gabriel/draw/view/DrawingFrame.java
package com.gabriel.draw.view;

import com.gabriel.draw.component.PropertySheet;
import com.gabriel.draw.controller.ActionController;
import com.gabriel.draw.controller.DrawingController;
import com.gabriel.draw.controller.DrawingWindowController;
import com.gabriel.draw.service.DrawingAppService;
import com.gabriel.draw.service.DrawingCommandAppService;
// Removed unused ShapeMode import
import com.gabriel.drawfx.model.Drawing;
// Removed unused Shape import
import com.gabriel.drawfx.service.AppService;
import com.gabriel.drawfx.command.CommandService;
import com.gabriel.property.PropertyOptions;
// Removed unused PropertyEventAdapter and Property imports

import javax.swing.*;
import java.awt.*;
// Removed unused ActionListener import

public class DrawingFrame extends JFrame {

    // Keep fields package-private or private unless needed otherwise
    Drawing drawing;
    DrawingAppService drawingAppService; // Base service implementation
    AppService appService; // Command-wrapped service
    // DrawingFrame drawingFrame; // Self-reference usually not needed
    Container pane;
    private PropertySheet propertySheet;
    ActionController actionListener; // For menu/toolbar actions
    DrawingMenuBar drawingMenuBar;
    DrawingToolBar drawingToolBar;
    DrawingView drawingView;
    DrawingController drawingController; // For canvas interactions
    JScrollPane jScrollPane;
    DrawingStatusPanel drawingStatusPanel;
    DrawingWindowController drawingWindowController; // For window events

    public DrawingFrame() {
        setTitle("GoDraw Application"); // Set a title

        // --- Model and Service Initialization ---
        drawing = new Drawing(); // Should ideally be managed by the service
        drawingAppService = new DrawingAppService(drawing); // Pass the model to the service
        // Wrap the base service with the command service decorator
        appService = DrawingCommandAppService.getInstance(drawingAppService);

        // --- Basic Frame Setup ---
        pane = getContentPane();
        // BorderLayout is good for this structure
        setLayout(new BorderLayout(5, 5)); // Add small gaps

        // --- Controller Initialization ---
        actionListener = new ActionController(appService); // For menu/toolbar
        drawingController = new DrawingController(appService, null); // Create controller (view set later)
        drawingWindowController = new DrawingWindowController(appService); // For window events


        // --- UI Component Initialization ---
        drawingMenuBar = new DrawingMenuBar(actionListener);
        setJMenuBar(drawingMenuBar);

        drawingToolBar = new DrawingToolBar(actionListener);
        actionListener.setToolBar(drawingToolBar); // Link action controller to toolbar

        drawingView = new DrawingView(appService);
        drawingView.setDrawingController(drawingController); // <<< Link View to Controller
        drawingController.setDrawingView(drawingView);     // <<< Link Controller to View

        // Setup DrawingView within a ScrollPane
        drawingView.setPreferredSize(new Dimension(2000, 1500)); // Set a large preferred size for scrolling
        jScrollPane = new JScrollPane(drawingView);
        // Optional: Set scroll increments for smoother scrolling
        jScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane.getHorizontalScrollBar().setUnitIncrement(16);


        drawingStatusPanel = new DrawingStatusPanel();
        drawingController.setDrawingStatusPanel(drawingStatusPanel); // Link controller to status panel

        // --- Property Sheet Initialization ---
        buildPropertyTable(); // Create and configure the property sheet
        drawingController.setPropertySheet(propertySheet); // Link controller to property sheet
        JScrollPane propertyScrollPane = new JScrollPane(propertySheet);
        propertyScrollPane.setPreferredSize(new Dimension(250, 0)); // Set preferred width


        // --- Layout Components ---
        pane.add(drawingToolBar, BorderLayout.NORTH);
        pane.add(jScrollPane, BorderLayout.CENTER);
        pane.add(propertyScrollPane, BorderLayout.EAST); // Add property sheet to the right
        pane.add(drawingStatusPanel, BorderLayout.SOUTH);

        // --- Link Services and Listeners ---
        drawingAppService.setDrawingView(drawingView); // Link base service to view for repaints
        actionListener.setComponent(drawingView); // Set parent component for dialogs
        actionListener.setFrame(this); // Set frame reference if needed by actions


        // Window Listeners
        this.addWindowListener(drawingWindowController);
        this.addWindowFocusListener(drawingWindowController);
        this.addWindowStateListener(drawingWindowController);

        // Command Service Listener for Undo/Redo button state
        CommandService.addListener((canUndo, canRedo) -> {
            drawingToolBar.updateUndoRedoState(canUndo, canRedo);
            // Optionally update menu items too
            drawingMenuBar.updateUndoRedoState(canUndo, canRedo);
        });

        // --- Final Frame Configuration ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // setSize(800, 600); // Initial size, pack might override
        pack(); // Size the frame based on component preferred sizes
        setMinimumSize(new Dimension(600, 400)); // Set a reasonable minimum size
        setLocationRelativeTo(null); // Center on screen
        // setVisible(true); // Moved to Main/Splash to show after setup

        // Set initial tool after everything is set up
        drawingToolBar.setActiveTool(com.gabriel.drawfx.ActionCommand.SELECT);
        appService.setToolMode(com.gabriel.drawfx.ToolMode.SELECT); // Ensure service matches
        drawingController.updateStatusBarTool("Select"); // Update status bar
    }

    // Renamed from buildGUI for clarity
    void buildPropertyTable() {
        // Configure property sheet options (can be simplified if defaults are okay)
        PropertyOptions options = new PropertyOptions.Builder()
                // .setHeaders("Attribute", "Value") // Example custom headers
                // .setRowHeight(25)              // Example custom row height
                .build();

        propertySheet = new PropertySheet(options);

        // Add the single, centralized event listener
        // Ensure this listener uses the *command-wrapped* appService
        propertySheet.addEventListener(new com.gabriel.draw.controller.PropertyEventListener(appService));

        // Initial population (likely empty or showing defaults)
        propertySheet.populateTable(appService);
    }

    // Removed the redundant inner EventListener class

    // Optional main method for testing just the frame
    public static void main(String[] args) {
        // Set Look and Feel early
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
                // Fallback
            }
        }

        SwingUtilities.invokeLater(() -> {
            DrawingFrame frame = new DrawingFrame();
            frame.setVisible(true);
        });
    }
}