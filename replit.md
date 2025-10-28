# GoDraw - Java Drawing Application

## Overview
GoDraw is a Java Swing-based desktop drawing application that allows users to create and manipulate various shapes including lines, rectangles, ellipses, text, and images. The application features a graphical user interface with tools for drawing, editing, and saving drawings.

## Project Information
- **Project Name**: Rubicon/GoDraw
- **Language**: Java 17
- **Build Tool**: Maven 3.8.6
- **UI Framework**: Java Swing
- **Display**: VNC (Virtual Network Computing) for GUI

## Project Structure
This is a multi-module Maven project with the following modules:
- **drawfx**: Core drawing framework and models
- **draw**: Main application module with GUI components
- **prop**: Property management and validation
- **batik**: Apache Batik integration for SVG support

## Recent Changes (October 28, 2025)

### Latest Update - Critical Infinite Recursion Bug Fixes (October 28, 2025 - Latest)

1. **Fixed Infinite Recursion in Command Pattern**:
   - **Root Cause**: Commands (AddShapeCommand, DeleteShapeCommand, MoveCommand, ScaleCommand) were calling the wrapped `DrawingCommandAppService`, which would create another command, leading to infinite recursion and stack overflow
   - **Solution**: Modified all command classes to call the underlying `DrawingAppService` directly instead of the wrapper
   - Added `getUnderlyingAppService()` method to `DrawingCommandAppService` to expose the base service
   - Updated `AddShapeCommand`, `DeleteShapeCommand`, `MoveCommand`, and `ScaleCommand` to extract and use the underlying service
   - This fix ensures shapes can now be added to the canvas without infinite loops
   - Undo/redo operations now work correctly without creating recursive command chains

2. **Commands Fixed**:
   - `AddShapeCommand`: Now directly calls underlying service for shape creation
   - `DeleteShapeCommand`: Now directly calls underlying service for shape deletion
   - `MoveCommand`: Now directly calls underlying service for shape movement
   - `ScaleCommand`: Now directly calls underlying service for shape scaling
   - All property commands (SetColorCommand, SetTextCommand, etc.) were already correct

### Previous Update - Critical Bug Fixes (October 28, 2025 - Late Evening)

1. **Fixed Command Pattern for Undo/Redo**:
   - Implemented proper state caching mechanism in DrawingController
   - Store original positions and sizes of all selected shapes on mousePressed
   - Restore original state before creating move/scale commands in mouseReleased
   - This ensures commands capture true original→final state transformations
   - Undo/redo now works correctly for move and scale operations without duplicate transformations

2. **Fixed Splash Screen Image Loading**:
   - Removed corrupted PNG image files (1.png, 2.png)
   - Simplified splash screen to use programmatically-generated graphics
   - Eliminated IOException errors on startup
   - Application now starts cleanly without error messages

### Previous Update - UI/UX Modernization (October 28, 2025 - Evening)

1. **Modern Vector-Based Toolbar Icons**: 
   - Replaced image-based toolbar buttons with programmatically-generated vector icons via ModernIconFactory
   - Added Move and Scale tools as separate toolbar buttons for better discoverability
   - Implemented modern icon designs with gradient fills, rounded corners, and clean styling
   - Added hover effects with light blue background for better visual feedback
   - Icons include: Select, Line, Rectangle, Ellipse, Text, Image, Move, Scale, Color, Fill

2. **Enhanced Status Bar**: 
   - Implemented DrawingStatusPanel showing real-time application state
   - Displays current tool name (e.g., "Tool: Select", "Tool: Line")
   - Shows live mouse coordinates updating as cursor moves (e.g., "Position: (120, 245)")
   - Displays selected shape information when shapes are selected (e.g., "Selected: Rectangle")
   - Professional three-panel layout with proper borders and spacing

3. **Improved Text Input Dialog**: 
   - Created TextInputDialog with comprehensive font selection controls
   - Font family dropdown with all available system fonts
   - Font style selection (Plain, Bold, Italic, Bold+Italic)
   - Font size selector (8-72 points)
   - Live preview showing selected font styling
   - Fixed font persistence bug - selected fonts now properly persist when creating text shapes
   - Double-click text editing also uses the dialog for consistency

4. **Fixed Color Picker Bugs**: 
   - Fixed null pointer exceptions when selecting colors
   - Added null checks and default colors (Black for foreground, White for fill)
   - Fixed color picker cancellation - colors only change if user clicks OK
   - Updated dialog titles to clearly distinguish "Fore Color" vs "Fill Color"

5. **Updated Splash Screen**: 
   - Modernized splash screen with professional design
   - Added "Tap to Enter the Drawing Application" message
   - Fixed image loading with proper resource paths

### Previous Major Enhancements - UI/UX & Performance Improvements

1. **Fixed SearchService Selection Logic**: 
   - Implemented proper top-most shape detection by iterating through shapes backwards (last-drawn to first-drawn)
   - Fixed single-click behavior to deselect all other shapes before selecting the clicked shape
   - Implemented Ctrl+click toggle for multi-selection support
   - Fixed empty-space click to deselect all shapes
   - Critical fix: Resolved selection state inconsistency where deselected shapes remained as primary selection

2. **Decoupled Creation from Selection**: 
   - Removed automatic selection of newly created shapes in DrawingController
   - Creation and selection are now separate actions for better user control
   - Only the Select tool can now select shapes

3. **Performance Optimization - Dirty Rectangle Repainting**: 
   - Implemented dirty rectangle repainting in mouseDragged method
   - Significantly improved drag/move/scale performance by only repainting affected areas
   - Added 20-pixel margin around shapes to ensure handlers are repainted

4. **Fixed Text Handle Alignment**: 
   - Corrected TextRenderer to align selection handles with visual text bounds
   - Used graphics context translation to offset handles by font ascent value
   - Text selection handles now appear correctly at the visual top of text

5. **Toolbar Improvements**: 
   - Updated Select tool tooltip to "Select (for Move & Scale)" for better UX clarity
   - Improved button styling with setFocusable(false) and setBorderPainted(true)
   - All toolbar buttons display icons properly

6. **Modern UI Look and Feel**: 
   - Added Nimbus Look and Feel with fallback to system default
   - Significantly improved application appearance and visual consistency
   - Modern, professional appearance across all UI components

7. **Command Pattern Verification**: 
   - Verified all move and scale operations use commands for undo/redo
   - MoveCommand and ScaleCommand properly capture state for undo/redo operations
   - Full undo/redo support for create, delete, move, scale, and property changes

### Previous Changes (October 27, 2025)

### Bug Fixes and Enhancements
1. **Splash Screen Implementation**: Added a personalized splash screen that displays before the main application with proper lifecycle management (DISPOSE_ON_CLOSE) to prevent JVM termination when transitioning to the main DrawingFrame.

2. **Text Input Dialog**: Implemented text input dialog that appears when creating new text shapes, allowing users to enter text content before placing it on the canvas.

3. **Text Renderer Improvements**: Fixed TextRenderer to calculate proper text bounds using FontMetrics for accurate handler display and auto-sizing based on text content instead of drag behavior.

4. **Fill Color Performance**: Removed expensive GradientPaint rendering and switched to solid fills, significantly improving drawing performance.

5. **Drawing Controller Enhancements**: 
   - Fixed shape selection logic for proper shape identification
   - Improved shape movement by replacing XOR rendering with repaint for smoother dragging
   - Enhanced shape scaling with proper handler-based resize detection
   - Added double-click text editing capability

6. **Resource Loading Fix**: Fixed GPanel and Splash image loading with correct resource paths and file extensions (GoDraw.png).

7. **Property Sheet Sync**: Enhanced bidirectional synchronization between canvas and property sheet - changes on canvas update property values and vice versa through propertySheet.populateTable() calls.

### Previous Changes
1. **Removed Missing Dependency**: The `fontchooser` module was referenced but missing from the repository. Removed it from the build to allow compilation.
2. **Simplified Font Selection**: Commented out the custom FontDialog and simplified text mode to use default fonts. Users can still add text, but advanced font selection is currently unavailable.
3. **Configured VNC Workflow**: Set up the application to run in a VNC environment so the GUI can be viewed in Replit.

## How to Run
The application runs automatically through the configured workflow. To manually run:
```bash
cd draw
mvn exec:java
```

The application will launch in the VNC viewer where you can interact with the drawing tools.

## Features
- **Drawing Tools**: Lines, rectangles, ellipses, text, and images
- **Selection Tool**: Select and manipulate existing shapes with dedicated Move and Scale tools
- **Color Selection**: Choose colors for outlines and fills with improved dialogs
- **Text Tool**: Create text with custom fonts, styles, and sizes via interactive dialog
- **File Operations**: Save and load drawings in XML format
- **Undo/Redo**: Full undo/redo support for all drawing operations
- **Property Sheet**: View and edit shape properties with real-time bidirectional synchronization
- **Status Bar**: Real-time display of current tool, mouse position, and selected shape information
- **Modern UI**: Vector-based toolbar icons with hover effects and professional styling

## Architecture
- **MVC Pattern**: The application uses Model-View-Controller architecture
  - Models: Shape classes (Line, Rectangle, Ellipse, Text, Picture)
  - Views: DrawingFrame, DrawingView, DrawingToolBar, DrawingMenuBar
  - Controllers: ActionController, DrawingController, DrawingWindowController
- **Command Pattern**: All drawing operations use commands for undo/redo functionality
- **Service Layer**: AppService provides the main API for drawing operations

## Dependencies
- Project Lombok 1.18.30 (for code generation)
- Internal modules: drawfx, property

## Build Information
- **Java Version**: Compiled with Java 17
- **Maven Version**: 3.8.6
- **Main Class**: `com.gabriel.draw.Main`

## User Preferences
None specified yet.
