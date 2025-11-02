package com.gabriel.draw.command;

import com.gabriel.draw.service.DrawingCommandAppService;
import com.gabriel.drawfx.ShapeMode;
import com.gabriel.drawfx.command.Command;
import com.gabriel.drawfx.service.AppService;

public class SetShapeCommand implements Command {
    private AppService appService;
    private ShapeMode oldValue;
    private ShapeMode newValue;

    public SetShapeCommand(AppService appService, ShapeMode oldValue, ShapeMode newValue) {
        this.appService = appService;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public void execute() {
        getUnderlyingService().setShapeMode(newValue);
    }

    @Override
    public void undo() {
        getUnderlyingService().setShapeMode(oldValue);
    }

    @Override
    public void redo() {
        execute();
    }

    private AppService getUnderlyingService() {
        if (appService instanceof DrawingCommandAppService) {
            return ((DrawingCommandAppService) appService).getUnderlyingAppService();
        }
        return appService;
    }
}
