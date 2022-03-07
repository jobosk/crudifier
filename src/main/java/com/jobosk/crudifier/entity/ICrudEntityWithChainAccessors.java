package com.jobosk.crudifier.entity;

public interface ICrudEntityWithChainAccessors<ID> extends IEntityWithId<ID> {

    ICrudEntityWithChainAccessors<ID> setId(ID id);
}
