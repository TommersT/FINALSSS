package com.gabriel.drawfx.model;

import com.gabriel.drawfx.SelectionMode;
import com.gabriel.drawfx.renderer.Renderer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;

@Getter
@Setter
public abstract class Shape {
    boolean selected = false;
    private SelectionMode selectionMode = SelectionMode.None;
    private int r = 2;
    private int id = 0;
    private Point location;
    private int width = 0;
    private int height = 0;
    private Color color = Color.RED;
    private Color fill = Color.WHITE;
    private int thickness = 2;
    private float opacity = 1.0f;
    String text;
    Font font;

    private Color startColor = Color.LIGHT_GRAY;
    private Color endColor = Color.DARK_GRAY;
    private boolean useGradient = false;
    private boolean visible = true;

    private Renderer rendererService;

    public Shape(){
    }
    public Shape(Point location){
        this.setLocation(location);
    }
    public Shape(Point location, Point endpoint){
        width = endpoint.x - location.x;
        height = endpoint.y - location.y;
        if(width<0){
            location.x += width;
            width = -width;
        }
        if(height < 0){
            location.y += height;
            height = -height;
        }
        this.setLocation(location);
    }
    public Shape(Point location, int width, int height){
        this.setLocation(location);
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Shape shape = (Shape) obj;
        return id == shape.id && id != 0;
    }

    @Override
    public int hashCode() {
        return id != 0 ? Integer.hashCode(id) : super.hashCode();
    }
}
