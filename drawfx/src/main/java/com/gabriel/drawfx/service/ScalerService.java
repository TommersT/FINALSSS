package com.gabriel.drawfx.service;

import com.gabriel.drawfx.SelectionMode;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;
import java.util.List;
import java.awt.*;


public final class  ScalerService {

    public void scale(Drawing drawing, Point start, Point end) {
        List<Shape > shapes = drawing.getShapes();
        for (Shape shape : shapes){
            scale(shape,start,end);
        }
    }

    public void scale(Shape shape, Point newEnd){
        int dx = newEnd.x-shape.getLocation().x;
        int dy = newEnd.y - shape.getLocation().y;
        shape.setWidth(dx);
        shape.setHeight(dy);
    }
    public void scale(Shape shape, Point start, Point end){
        int dx = end.x - start.x;
        int dy = end.y - start.y;
        int height = shape.getHeight();
        int width = shape.getWidth();
        if(shape.getSelectionMode() == SelectionMode.UpperLeft) {
            shape.getLocation().x += dx;
            shape.getLocation().y += dy;
            shape.setWidth(width - dx);
            shape.setHeight(height - dy);
        } else if(shape.getSelectionMode() == SelectionMode.LowerLeft) {
            shape.getLocation().x += dx;
            shape.setWidth(width - dx);
            shape.setHeight(height + dy);
        } else if(shape.getSelectionMode() == SelectionMode.UpperRight){
            shape.getLocation().y += dy;
            shape.setWidth(width + dx);
            shape.setHeight(height - dy);
        } else if(shape.getSelectionMode() == SelectionMode.LowerRight){
            shape.setWidth(width + dx);
            shape.setHeight(height + dy);
        } else if(shape.getSelectionMode() == SelectionMode.MiddleRight){
            shape.setWidth(width + dx);
        } else if(shape.getSelectionMode() == SelectionMode.MiddleLeft){
            shape.setWidth(width - dx);
            shape.getLocation().x += dx;
        } else if(shape.getSelectionMode() == SelectionMode.MiddleTop) {
            shape.setHeight(height - dy);
            shape.getLocation().y += dy;
        } else if(shape.getSelectionMode() == SelectionMode.MiddleBottom){
            shape.setHeight(height + dy);
        }
    }
}
