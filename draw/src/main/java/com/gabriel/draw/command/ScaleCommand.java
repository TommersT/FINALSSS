package com.gabriel.draw.command;

import com.gabriel.draw.model.Text;
import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;
import com.gabriel.drawfx.SelectionMode;
import java.awt.*;

public class ScaleCommand implements Command {
    private AppService appService;
    private AppService underlyingAppService;
    private Shape shape;

    private Point originalLocation;
    private Dimension originalSize;

    private Point finalLocation;
    private Dimension finalSize;

    public ScaleCommand(AppService appService, Shape shape, Point start, Point end) {
        this.appService = appService;
        if (appService instanceof DrawingCommandAppService) {
            this.underlyingAppService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
        } else {
            this.underlyingAppService = appService;
        }
        this.shape = shape;

        this.originalLocation = new Point(shape.getLocation());
        this.originalSize = new Dimension(shape.getWidth(), shape.getHeight());

        calculateFinalState(start, end);
    }

    private void calculateFinalState(Point start, Point end) {
        Point tempLoc = new Point(originalLocation);
        Dimension tempSize = new Dimension(originalSize);

        int dx = end.x - start.x;
        int dy = end.y - start.y;
        SelectionMode selMode = shape.getSelectionMode();

        if(selMode == SelectionMode.UpperLeft) {
            tempLoc.translate(dx, dy);
            tempSize.width -= dx;
            tempSize.height -= dy;
        } else if(selMode == SelectionMode.LowerLeft) {
            tempLoc.translate(dx, 0);
            tempSize.width -= dx;
            tempSize.height += dy;
        } else if(selMode == SelectionMode.UpperRight){
            tempLoc.translate(0, dy);
            tempSize.width += dx;
            tempSize.height -= dy;
        } else if(selMode == SelectionMode.LowerRight){
            tempSize.width += dx;
            tempSize.height += dy;
        } else if(selMode == SelectionMode.MiddleRight){
            tempSize.width += dx;
        } else if(selMode == SelectionMode.MiddleLeft){
            tempLoc.translate(dx, 0);
            tempSize.width -= dx;
        } else if(selMode == SelectionMode.MiddleTop) {
            tempLoc.translate(0, dy);
            tempSize.height -= dy;
        } else if(selMode == SelectionMode.MiddleBottom){
            tempSize.height += dy;
        }

        if (tempSize.width < 0) {
            tempLoc.x += tempSize.width;
            tempSize.width *= -1;
        }
        if (tempSize.height < 0) {
            tempLoc.y += tempSize.height;
            tempSize.height *= -1;
        }

        this.finalLocation = tempLoc;
        this.finalSize = tempSize;
    }

    @Override
    public void execute() {
        shape.setLocation(new Point(finalLocation));
        shape.setWidth(finalSize.width);
        shape.setHeight(finalSize.height);
        triggerRepaint();
    }

    @Override
    public void undo() {
        shape.setLocation(new Point(originalLocation));
        shape.setWidth(originalSize.width);
        shape.setHeight(originalSize.height);
        triggerRepaint();
    }

    @Override
    public void redo() {
        execute();
    }

    private void triggerRepaint() {
        if (underlyingAppService instanceof com.gabriel.draw.service.DrawingAppService) {
            ((com.gabriel.draw.service.DrawingAppService)underlyingAppService).triggerRepaint();
        } else {
            System.err.println("Warning: Could not trigger repaint from " + this.getClass().getSimpleName());
        }
    }
}
