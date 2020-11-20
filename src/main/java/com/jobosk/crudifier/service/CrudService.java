package com.jobosk.crudifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobosk.crudifier.util.CopyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

//@Primary
//@Service
//@Scope("prototype")
public abstract class CrudService<Entity> implements ICrudService<Entity> {

    private final JpaRepository<Entity, UUID> repository;

    @Autowired
    private ObjectMapper mapper;
    //protected DozerBeanMapper mapper;

    public CrudService(final JpaRepository<Entity, UUID> repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entity> find(final Entity obj) {
        return repository.findAll(getExample(obj));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entity> find(final Entity obj, final Sort sort) {
        return repository.findAll(getExample(obj), sort);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Entity> find(final Entity obj, final Pageable pageable) {
        return repository.findAll(getExample(obj), pageable);
    }

    private Example<Entity> getExample(final Entity obj) {
        return Example.of(
                obj
                , ExampleMatcher
                        .matching()
                        .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Entity find(final UUID id) {
        return repository.getOne(id);
    }

    @Override
    @Transactional
    public Entity create(final Entity obj) {
        return repository.save(obj);
    }

    @Override
    @Transactional
    public Entity update(final UUID id, final Map<String, Object> fields) {
        Entity entity = repository.getOne(id);
        CopyUtil.copyProperties(entity, fields, mapper);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public void delete(final UUID id) {
        repository.deleteById(id);
    }
}
