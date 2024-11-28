package com.jobosk.crudifier.validator;

import com.jobosk.crudifier.exception.CrudException;

@FunctionalInterface
public interface CrudEntityValidator<Entity> {
    void accept(Entity entity) throws CrudException;
}
