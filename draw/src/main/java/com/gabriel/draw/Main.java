package com.gabriel.draw;

import com.gabriel.draw.view.Splash;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
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
                ex.printStackTrace();
            }
        }
        
        SwingUtilities.invokeLater(() -> {
            JFrame splashFrame = new JFrame("GoDraw");
            Splash splashPanel = new Splash();
            splashFrame.add(splashPanel);
            splashFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            splashFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            splashFrame.setUndecorated(false);
            splashFrame.setVisible(true);
        });
    }
}