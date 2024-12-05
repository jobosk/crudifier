package com.jobosk.crudifier.entity;

import com.jobosk.crudifier.exception.CrudException;

@FunctionalInterface
public interface PrePersistAction<Entity> {
    void accept(Entity entity) throws CrudException;
}
