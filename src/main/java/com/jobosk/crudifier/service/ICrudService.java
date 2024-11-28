package com.jobosk.crudifier.service;

import com.jobosk.crudifier.exception.CrudException;
import com.jobosk.crudifier.validator.CrudEntityValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ICrudService<Entity, Id> {

    List<Entity> find(Map<String, String> filters);

    List<Entity> find(Map<String, String> filters, Sort sort);

    Page<Entity> find(Map<String, String> filters, Pageable pageable);

    Collection<Entity> findAll(Map<String, String> filters, HttpServletResponse response);

    Optional<Entity> find(Id id);

    Entity create(Entity entity) throws CrudException;

    Entity create(Entity entity, CrudEntityValidator<Entity> validator) throws CrudException;

    Entity update(Entity entity, Map<String, Object> fields) throws CrudException;

    Entity update(Entity entity, Map<String, Object> fields, CrudEntityValidator<Entity> validator) throws CrudException;

    boolean delete(Id id);
}
