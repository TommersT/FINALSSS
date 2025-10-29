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

## Recent Changes (October 29, 2025)

### Property Table Fix - CRITICAL BUG RESOLVED (October 29, 2025)

**Problem**: The property table on the right side of the application was completely empty - no rows were showing despite 17 properties being defined.

**Root Cause**: PropertySheet class had override methods that were intentionally ignoring all calls to `addProperty()`, preventing any rows from being added to the table during initialization.

**Solution**:
1. Removed the blocking `@Override` methods in PropertySheet.java (lines 432-433) that were doing nothing
2. These overrides were originally intended to prevent external additions after initialization, but they were also blocking the internal initialization process
3. The fix allows the superclass `PropertyPanel.addProperty()` methods to execute properly, adding all 17 property rows to the table

**Result**: Property table now displays all 17 rows correctly:
- Object Type
- Fore Color, Fill Color, Start Color, End Color
- Use Gradient, Visible
- X Location, Y Location, Width, Height
- Line Thickness
- Text, Image
- Font Family, Font Style, Font Size

### Previous Update - Critical Infinite Recursion Bug Fixes (October 28, 2025)

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

### Previous Update - UI/UX Modernization (October 28, 2025)

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
- **Property Sheet**: **NOW WORKING** - View and edit shape properties with real-time bidirectional synchronization. All 17 property rows are visible and functional.
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
