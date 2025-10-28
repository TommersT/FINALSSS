package com.gabriel.draw.view;

import com.gabriel.draw.util.ImageLoader;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Splash extends JPanel implements MouseListener, MouseMotionListener {
    private BufferedImage backgroundImage;
    private BufferedImage buttonImage;
    private ImageLoader imageLoader;
    private Rectangle buttonBounds;
    private boolean buttonHover = false;
    
    public Splash() {
        backgroundImage = createFallbackBackground();
        buttonImage = createFallbackButton();
        
        setLayout(null);
        setBackground(new Color(45, 55, 80));
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    
    private BufferedImage createFallbackBackground() {
        BufferedImage img = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        GradientPaint gradient = new GradientPaint(0, 0, new Color(45, 55, 80), 
                                                    0, 1080, new Color(80, 60, 120));
        g.setPaint(gradient);
        g.fillRect(0, 0, 1920, 1080);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.drawString("GoDraw", 800, 200);
        g.dispose();
        return img;
    }
    
    private BufferedImage createFallbackButton() {
        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(60, 140, 220));
        g.fillRoundRect(50, 100, 300, 100, 20, 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.drawString("START DRAWING", 80, 165);
        g.dispose();
        return img;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, width, height, this);
        }
        
        if (buttonImage != null) {
            int buttonWidth = Math.min(400, width / 3);
            int buttonHeight = (int) (buttonWidth * buttonImage.getHeight() / (double) buttonImage.getWidth());
            
            int buttonX = width - buttonWidth - (width / 8);
            int buttonY = (height - buttonHeight) / 2;
            
            buttonBounds = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
            
            if (buttonHover) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
                g2d.setColor(new Color(255, 255, 255, 100));
                g2d.fillRoundRect(buttonX - 10, buttonY - 10, buttonWidth + 20, buttonHeight + 20, 15, 15);
            }
            
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2d.drawImage(buttonImage, buttonX, buttonY, buttonWidth, buttonHeight, this);
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        if (buttonBounds != null && buttonBounds.contains(e.getPoint())) {
            launchMainApplication();
        }
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        buttonHover = false;
        repaint();
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        boolean wasHover = buttonHover;
        buttonHover = (buttonBounds != null && buttonBounds.contains(e.getPoint()));
        
        if (buttonHover) {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        
        if (wasHover != buttonHover) {
            repaint();
        }
    }
    
    private void launchMainApplication() {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        
        DrawingFrame mf = new DrawingFrame();
        mf.setExtendedState(mf.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        mf.setVisible(true);
        
        topFrame.dispose();
    }
}
