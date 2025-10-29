package com.gabriel.draw;

import com.gabriel.draw.view.DrawingFrame; // Import DrawingFrame
import com.gabriel.draw.view.Splash;

import javax.swing.*;
import java.awt.*;

public class Main {
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
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace(); // Keep logging errors
            }
        }

        // --- Splash Screen Setup ---
        // We need to pass a reference or use a callback to launch DrawingFrame later
        // For simplicity now, let's assume Splash handles launching DrawingFrame
        // Modify Splash.java's launchMainApplication if needed to include revalidate/repaint

        SwingUtilities.invokeLater(() -> {
            // --- This part shows the Splash Screen ---
            JFrame splashFrame = new JFrame("GoDraw Splash"); // Give it a title
            Splash splashPanel = new Splash(); // Splash now handles launching DrawingFrame
            splashFrame.add(splashPanel);
            splashFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Dispose splash when closed
            splashFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            // splashFrame.setUndecorated(true); // Maybe keep decoration for debugging
            splashFrame.setUndecorated(false);
            splashFrame.setVisible(true);

            // --- The DrawingFrame launch logic should be inside Splash.java's mouseClicked ---
            // --- Ensure the logic IN Splash.java looks like this: ---
            /*
            // Inside Splash.java -> launchMainApplication() -> SwingUtilities.invokeLater runnable:

            DrawingFrame mf = new DrawingFrame();
            mf.setExtendedState(mf.getExtendedState() | JFrame.MAXIMIZED_BOTH);

            // Revalidate and Repaint *before* setting visible
            mf.revalidate();
            mf.repaint();

            mf.setVisible(true);
            topFrame.dispose(); // Close splash screen AFTER main frame is ready
            */

        });
    }
}