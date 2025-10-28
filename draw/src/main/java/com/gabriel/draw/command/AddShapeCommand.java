package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.model.Shape;
import com.gabriel.drawfx.service.AppService;

public class AddShapeCommand implements Command{
    Shape shape;
    AppService underlyingAppService;

    public AddShapeCommand(AppService appService, Shape shape){
        this.shape = shape;
        if (appService instanceof DrawingCommandAppService) {
            this.underlyingAppService = ((DrawingCommandAppService) appService).getUnderlyingAppService();
        } else {
            this.underlyingAppService = appService;
        }
    }
    @Override
    public void execute() {
        underlyingAppService.create(shape);
    }

    @Override
    public void undo() {
        underlyingAppService.delete(shape);
    }

    @Override
    public void redo() {
        underlyingAppService.create(shape);
    }
}