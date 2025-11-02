package com.gabriel.draw.model;

import com.gabriel.draw.renderer.PictureRenderer;
import com.gabriel.drawfx.model.Shape;
import lombok.Data;

import java.awt.*;

@Data
public class Picture extends Shape {
    private String imageFilename;
    public Picture(Point start, Point end, String imageFilename){
        super(start, end );
        this.imageFilename = imageFilename;
        setRendererService(new PictureRenderer());
    }

    public String getImageFilename() {
        return this.imageFilename;
    }
}