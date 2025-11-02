package com.gabriel.property.validator.longNumber;

import com.gabriel.property.validator.Validator;

public class LongValidator implements Validator {

    @Override
    public boolean validate(Object object) {
        return isLong(object);
    }

    protected boolean isLong(Object object) {
        try {
            Long.parseLong((String) object);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}
