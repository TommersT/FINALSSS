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

## Recent Changes (October 27, 2025)

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
- **Selection Tool**: Select and manipulate existing shapes
- **Color Selection**: Choose colors for outlines and fills
- **File Operations**: Save and load drawings in XML format
- **Undo/Redo**: Full undo/redo support for all drawing operations
- **Property Sheet**: View and edit shape properties

## Known Limitations
- **Font Selection**: The advanced font chooser dialog is currently unavailable due to a missing dependency. Text uses the default system font.
- **Font Module**: The `fontchooser` module is not included in this repository and has been removed from the build configuration.

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
