package com.gabriel.property.property;

import com.gabriel.property.validator.Validator;

public interface Property<T> {

    String getName();
    T getValue();
    void setValue(T value);
    Validator getValidator();
}
