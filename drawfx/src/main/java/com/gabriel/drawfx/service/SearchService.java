package com.gabriel.drawfx.service;

import com.gabriel.drawfx.SelectionMode;
import com.gabriel.drawfx.model.Drawing;
import com.gabriel.drawfx.model.Shape;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
public final class SearchService {

    public void search(AppService appService, Point p) {
        search(appService, p,true);
    }

    public void search(AppService appService, Point p, boolean single) {
        Drawing drawing = appService.getDrawing();
        List<Shape> shapes = drawing.getShapes();
        int r = appService.getSearchRadius();

        Shape foundShape = null;
        
        for (int i = shapes.size() - 1; i >= 0; i--) {
            Shape shape = shapes.get(i);
            Point loc = shape.getLocation();
            int width = shape.getWidth();
            int height = shape.getHeight();
            
            if (p.x > loc.x - r && p.x < loc.x + width + r && p.y > loc.y - r && p.y < loc.y + height + r) {
                foundShape = shape;
                
                if (found(shape, p, loc.x, loc.y, r)) {
                    shape.setSelectionMode(SelectionMode.UpperLeft);
                } else if (found(shape, p, loc.x, loc.y + height / 2, r)) {
                    shape.setSelectionMode(SelectionMode.MiddleLeft);
                } else if (found(shape, p, loc.x, loc.y + height, r)) {
                    shape.setSelectionMode(SelectionMode.LowerLeft);
                } else if (found(shape, p, loc.x + width / 2, loc.y, r)) {
                    shape.setSelectionMode(SelectionMode.MiddleTop);
                } else if (found(shape, p, loc.x + width, loc.y, r)) {
                    shape.setSelectionMode(SelectionMode.UpperRight);
                } else if (found(shape, p, loc.x + width, loc.y + height / 2, r)) {
                    shape.setSelectionMode(SelectionMode.MiddleRight);
                } else if (found(shape, p, loc.x + width, loc.y + height, r)) {
                    shape.setSelectionMode(SelectionMode.LowerRight);
                } else if (found(shape, p, loc.x + width / 2, loc.y + height, r)) {
                    shape.setSelectionMode(SelectionMode.MiddleBottom);
                } else {
                    shape.setSelectionMode(SelectionMode.None);
                }
                
                break;
            }
        }
        
        if (foundShape != null) {
            if (single) {
                for (Shape shape : shapes) {
                    if (shape != foundShape) {
                        shape.setSelected(false);
                    }
                }
                foundShape.setSelected(true);
                drawing.setSelectedShape(foundShape);
            } else {
                foundShape.setSelected(!foundShape.isSelected());
                if (foundShape.isSelected()) {
                    drawing.setSelectedShape(foundShape);
                } else {
                    Shape newSelectedShape = null;
                    for (Shape shape : shapes) {
                        if (shape.isSelected()) {
                            newSelectedShape = shape;
                            break;
                        }
                    }
                    drawing.setSelectedShape(newSelectedShape);
                }
            }
        } else {
            if (single) {
                for (Shape shape : shapes) {
                    shape.setSelected(false);
                }
                drawing.setSelectedShape(null);
            }
        }
    }
    boolean found(Shape shape, Point p, int x, int y, int r){
        return (p.x>x-r && p.x< x+r && p.y>y-r && p.y<y+r);
    }
}