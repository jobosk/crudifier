package com.jobosk.crudifier.entity;

public interface ICrudEntity<ID> extends IEntityWithId<ID> {

    void setId(ID id);
}
