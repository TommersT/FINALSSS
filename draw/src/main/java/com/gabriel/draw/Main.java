package com.gabriel.draw;

import com.gabriel.draw.view.Splash;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
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