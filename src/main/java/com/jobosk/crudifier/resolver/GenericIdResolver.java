package com.jobosk.crudifier.resolver;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import java.util.Optional;

public abstract class GenericIdResolver<Entity, Id> implements ObjectIdResolver {

    @Autowired
    private JpaRepository<Entity, Id> repository;

    public GenericIdResolver() {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }

    @Override
    public void bindItem(final ObjectIdGenerator.IdKey id, final Object pojo) {
    }

    @Override
    public Entity resolveId(final ObjectIdGenerator.IdKey id) {
        final Optional<Entity> entity = repository.findById((Id) id.key);
        return entity.orElseGet(() -> resolveMissingEntity(id));
    }

    protected Entity resolveMissingEntity(final ObjectIdGenerator.IdKey id) {
        throw new RuntimeException("Unable to serialize entity from reference: " + id);
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
