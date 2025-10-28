package com.gabriel.draw.view;

import javax.swing.*;
import java.awt.*;

public class TextInputDialog extends JDialog {
    private JTextArea textArea;
    private JComboBox<String> fontFamilyCombo;
    private JComboBox<String> fontStyleCombo;
    private JSpinner fontSizeSpinner;
    private boolean okClicked = false;
    
    public TextInputDialog(Frame parent) {
        super(parent, "Text Input", true);
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel textLabel = new JLabel("Enter text:");
        textArea = new JTextArea(4, 30);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        JPanel textPanel = new JPanel(new BorderLayout(5, 5));
        textPanel.add(textLabel, BorderLayout.NORTH);
        textPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel fontPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        fontPanel.setBorder(BorderFactory.createTitledBorder("Font Options"));
        
        fontPanel.add(new JLabel("Font Family:"));
        String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontFamilyCombo = new JComboBox<>(fontNames);
        fontFamilyCombo.setSelectedItem("SansSerif");
        fontPanel.add(fontFamilyCombo);
        
        fontPanel.add(new JLabel("Font Style:"));
        String[] fontStyles = {"Plain", "Bold", "Italic", "Bold Italic"};
        fontStyleCombo = new JComboBox<>(fontStyles);
        fontPanel.add(fontStyleCombo);
        
        fontPanel.add(new JLabel("Font Size:"));
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 72, 1));
        fontPanel.add(fontSizeSpinner);
        
        mainPanel.add(textPanel, BorderLayout.CENTER);
        mainPanel.add(fontPanel, BorderLayout.SOUTH);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            okClicked = true;
            dispose();
        });
        
        cancelButton.addActionListener(e -> {
            okClicked = false;
            dispose();
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(getParent());
    }
    
    public String getText() {
        return textArea.getText();
    }
    
    public void setText(String text) {
        textArea.setText(text);
    }
    
    public Font getSelectedFont() {
        String family = (String) fontFamilyCombo.getSelectedItem();
        String styleStr = (String) fontStyleCombo.getSelectedItem();
        int style = Font.PLAIN;
        
        if ("Bold".equals(styleStr)) {
            style = Font.BOLD;
        } else if ("Italic".equals(styleStr)) {
            style = Font.ITALIC;
        } else if ("Bold Italic".equals(styleStr)) {
            style = Font.BOLD | Font.ITALIC;
        }
        
        int size = (Integer) fontSizeSpinner.getValue();
        return new Font(family, style, size);
    }
    
    public void setSelectedFont(Font font) {
        if (font != null) {
            fontFamilyCombo.setSelectedItem(font.getFamily());
            
            int style = font.getStyle();
            if ((style & Font.BOLD) != 0 && (style & Font.ITALIC) != 0) {
                fontStyleCombo.setSelectedItem("Bold Italic");
            } else if ((style & Font.BOLD) != 0) {
                fontStyleCombo.setSelectedItem("Bold");
            } else if ((style & Font.ITALIC) != 0) {
                fontStyleCombo.setSelectedItem("Italic");
            } else {
                fontStyleCombo.setSelectedItem("Plain");
            }
            
            fontSizeSpinner.setValue(font.getSize());
        }
    }
    
    public boolean isOkClicked() {
        return okClicked;
    }
    
    public static TextInputDialog showDialog(Frame parent, String initialText, Font initialFont) {
        TextInputDialog dialog = new TextInputDialog(parent);
        if (initialText != null) {
            dialog.setText(initialText);
        }
        if (initialFont != null) {
            dialog.setSelectedFont(initialFont);
        }
        dialog.setVisible(true);
        return dialog;
    }
}
