package com.gabriel.property.validator;


public class NullValidator implements Validator {

    @Override
    public boolean validate(Object object) {
        return true;
    }
}
