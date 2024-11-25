package com.jobosk.crudifier.entity;

public interface ICrudChainAccessorEntity<ID> extends IHasCrudId<ID> {

    ICrudChainAccessorEntity<ID> setId(ID id);
}
