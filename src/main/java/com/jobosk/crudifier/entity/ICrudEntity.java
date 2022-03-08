package com.jobosk.crudifier.entity;

public interface ICrudEntity<ID> extends IHasIdentifier<ID> {

    void setId(ID id);
}
