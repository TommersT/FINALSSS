package com.gabriel.draw.view;

import com.gabriel.draw.util.ImageLoader;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class Splash extends JPanel implements MouseListener {
    private BufferedImage backgroundImage;
    private BufferedImage tapTextImage;
    private Timer animationTimer;
    private float textAlpha = 1.0f;
    private boolean fadingOut = true;

    public Splash() {
        backgroundImage = loadImageResource("/com/gabriel/draw/view/images/1.png");
        tapTextImage = loadImageResource("/com/gabriel/draw/view/images/2.png");

        if (backgroundImage == null) {
            backgroundImage = createFallbackBackground();
            System.err.println("Warning: Failed to load 1.png, using fallback background.");
        }
        if (tapTextImage == null) {
            tapTextImage = createFallbackTapText();
            System.err.println("Warning: Failed to load 2.png, using fallback tap text.");
        }

        setLayout(null);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        addMouseListener(this);

        animationTimer = new Timer(50, e -> updateAnimation());
        animationTimer.start();
    }

    private BufferedImage loadImageResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("Resource not found: " + path);
                return null;
            }
            return ImageIO.read(is);
        } catch (IOException e) {
            System.err.println("Error loading image resource " + path + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IllegalArgumentException e) {
            System.err.println("Error reading image resource " + path + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private BufferedImage createFallbackBackground() {
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        GradientPaint gradient = new GradientPaint(0, 0, new Color(45, 55, 80), 0, 600, new Color(80, 60, 120));
        g.setPaint(gradient);
        g.fillRect(0, 0, 800, 600);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.drawString("GoDraw", 300, 150);
        g.dispose();
        return img;
    }

    private BufferedImage createFallbackTapText() {
        BufferedImage img = new BufferedImage(300, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(255, 255, 150));
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Tap Anywhere to Draw!", 10, 35);
        g.dispose();
        return img;
    }

    private void updateAnimation() {
        if (fadingOut) {
            textAlpha -= 0.05f;
            if (textAlpha <= 0.1f) {
                textAlpha = 0.1f;
                fadingOut = false;
            }
        } else {
            textAlpha += 0.05f;
            if (textAlpha >= 1.0f) {
                textAlpha = 1.0f;
                fadingOut = true;
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int width = getWidth();
        int height = getHeight();

        if (backgroundImage != null) {
            double imgAspect = (double) backgroundImage.getWidth() / backgroundImage.getHeight();
            double panelAspect = (double) width / height;
            int drawWidth, drawHeight, drawX, drawY;

            if (imgAspect > panelAspect) {
                drawWidth = width;
                drawHeight = (int) (width / imgAspect);
                drawX = 0;
                drawY = (height - drawHeight) / 2;
            } else {
                drawHeight = height;
                drawWidth = (int) (height * imgAspect);
                drawY = 0;
                drawX = (width - drawWidth) / 2;
            }
            try {
                g2d.drawImage(backgroundImage, drawX, drawY, drawWidth, drawHeight, this);
            } catch (Exception e) {
                System.err.println("Error drawing background image: " + e.getMessage());
                g2d.setColor(Color.DARK_GRAY);
                g2d.fillRect(0, 0, width, height);
                g2d.setColor(Color.WHITE);
                g2d.drawString("Error loading background", 50, 50);
            }
        } else {
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(0, 0, width, height);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Background image not loaded", 50, 50);
        }

        if (tapTextImage != null) {
            double scaleFactor = 0.15;
            int textImgWidth = (int) (tapTextImage.getWidth() * scaleFactor);
            int textImgHeight = (int) (tapTextImage.getHeight() * scaleFactor);
            int rightMargin = 70;
            int textX = width - textImgWidth - rightMargin;
            int textY = (height - textImgHeight) / 2;

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha));
            try {
                g2d.drawImage(tapTextImage, textX, textY, textImgWidth, textImgHeight, this);
            } catch (Exception e) {
                System.err.println("Error drawing tap text image: " + e.getMessage());
            }
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        } else {
            g2d.setColor(Color.YELLOW);
            g2d.drawString("Tap to start (image error)", width - 200, height / 2);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        launchMainApplication();
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    private void launchMainApplication() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (topFrame == null) return;

        SwingUtilities.invokeLater(() -> {
            DrawingFrame mf = new DrawingFrame();
            mf.setExtendedState(mf.getExtendedState() | JFrame.MAXIMIZED_BOTH);
            mf.setVisible(true);
            topFrame.dispose();
        });
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }
}
