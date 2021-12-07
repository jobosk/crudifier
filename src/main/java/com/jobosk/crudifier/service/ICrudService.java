package com.jobosk.crudifier.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ICrudService<Entity, Id> {

    List<Entity> find(Map<String, String> filters);

    List<Entity> find(Map<String, String> filters, Sort sort);

    Page<Entity> find(Map<String, String> filters, Pageable pageable);

    Collection<Entity> findAll(Map<String, String> filters, HttpServletResponse response);

    Entity find(Id id);

    Entity create(Entity obj);

    Entity update(Entity obj, Map<String, Object> fields);

    boolean delete(Id id);
}
