package com.gabriel.property.validator.floatNumber;

import com.gabriel.property.validator.Validator;

public class FloatValidator implements Validator {

    @Override
    public boolean validate(Object object) {
        return isFloat(object);
    }

    protected boolean isFloat(Object object) {
        try {
            Float.parseFloat((String) object);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}
