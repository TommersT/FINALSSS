package com.gabriel.draw.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class GPanel extends JPanel {
    private BufferedImage image;

    public GPanel(String imgFile){
            try (InputStream is = getClass().getResourceAsStream("/" + imgFile)) {
                if (is == null) {
                    throw new IOException("Image not found: " + imgFile);
                }
                image = ImageIO.read(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
        g.drawImage(image, 0, 0,image.getWidth(),image.getHeight(),  this);
    }
}
