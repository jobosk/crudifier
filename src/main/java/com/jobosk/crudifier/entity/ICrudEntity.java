package com.jobosk.crudifier.entity;

public interface ICrudEntity<ID> extends IHasCrudId<ID> {

    void setId(ID id);
}
