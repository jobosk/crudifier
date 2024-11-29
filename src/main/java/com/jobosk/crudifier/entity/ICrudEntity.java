package com.jobosk.crudifier.entity;

public interface ICrudEntity<ID> extends IHasThis, IHasCrudId<ID> {

    void setId(ID id);
}
