package com.jobosk.crudifier.entity;

public interface ICrudChainAccessorEntity<ID> extends IHasIdentifier<ID> {

    ICrudChainAccessorEntity<ID> setId(ID id);
}
