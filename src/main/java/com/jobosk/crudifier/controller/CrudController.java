package com.jobosk.crudifier.controller;

import com.jobosk.crudifier.exception.CrudException;
import com.jobosk.crudifier.service.ICrudService;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class CrudController<Entity, Id> {

    private final ICrudService<Entity, Id> service;

    public CrudController(final ICrudService<Entity, Id> service) {
        this.service = service;
    }

    public Entity findOne(Id id) throws CrudException {
        return service.find(id)
                .orElseThrow(() -> new CrudException(
                        HttpStatus.NOT_FOUND
                        , "missing_id"
                        , List.of(id)
                ));
    }

    public Collection<Entity> findAll(Map<String, String> parameters, HttpServletResponse response) throws CrudException {
        return service.findAll(parameters, response);
    }

    public Entity create(@Valid Entity entity) throws CrudException {
        return service.create(entity);
    }

    public Entity update(@Valid Entity entity, Map<String, Object> fields) throws CrudException {
        return service.update(entity, fields);
    }

    public boolean delete(Id id) throws CrudException {
        return service.delete(id);
    }
}
