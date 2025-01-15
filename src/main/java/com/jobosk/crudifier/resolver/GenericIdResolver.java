package com.jobosk.crudifier.resolver;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public abstract class GenericIdResolver<Entity, Id> implements ObjectIdResolver {

    private final JpaRepository<Entity, Id> repository;

    public GenericIdResolver(final JpaRepository<Entity, Id> repository) {
        this.repository = repository;
    }

    @Override
    public void bindItem(final ObjectIdGenerator.IdKey id, final Object pojo) {
    }

    @Override
    public Entity resolveId(final ObjectIdGenerator.IdKey idKey) {
        final Id id = getId(idKey);
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cannot find entity with ID: " + id));
    }

    protected Id getId(final ObjectIdGenerator.IdKey idKey) {
        return Optional.ofNullable(idKey)
                .map(idk -> (Id) idk.key)
                .orElseThrow(() -> new RuntimeException("Missing ID from key: " + idKey));
    }

    @Override
    public ObjectIdResolver newForDeserialization(final Object context) {
        return this;
    }

    @Override
    public boolean canUseFor(final ObjectIdResolver resolverType) {
        return false;
    }
}
