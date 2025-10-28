package com.gabriel.draw.view;

import com.gabriel.draw.util.ImageLoader;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Splash extends JPanel implements MouseListener {
    private BufferedImage image;
    private GPanel gPanel;
    ImageLoader imageLoader;
    int width;
    int height;
    public Splash() {
        try {
            imageLoader = new ImageLoader();
            image = imageLoader.loadImage("/splash.png");
            height = image.getHeight();
            width = image.getWidth();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        setSize(width, height);
        setLayout(new BorderLayout());
        setBackground(new Color(100, 120, 200));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        addMouseListener(this);
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this); // see javadoc for more info on the parameters
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);

        DrawingFrame mf = new DrawingFrame();
        mf.setExtendedState(mf.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        mf.setVisible(true);

        topFrame.dispose();
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
