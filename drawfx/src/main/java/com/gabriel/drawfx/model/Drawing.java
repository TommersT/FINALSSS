package com.gabriel.drawfx.model;

import com.gabriel.drawfx.DrawMode;
import com.gabriel.drawfx.ShapeMode;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Drawing {
    Point location;
    private String filename;
    private String imageFilename;
    private Color color;
    private Color fill;
    int thickness  = 1;
    private ShapeMode shapeMode = ShapeMode.Select;
    private DrawMode drawMode = DrawMode.Idle;
    private int SearchRadius = 5;
    private Font font;
    int width;
    int height;
    List<Shape> shapes;
    Shape selectedShape;
    String text = "Default text";

    private Color startColor = Color.CYAN;
    private Color endColor = Color.MAGENTA;
    private boolean useGradient = false;

    public Drawing(){
        location  = new Point(0,0);
        color = Color.RED;
        fill = Color.WHITE;
        font = new Font("SansSerif", Font.PLAIN, 12);
        shapes = new ArrayList<>();
        imageFilename = null;
    }
}
