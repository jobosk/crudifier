package com.jobosk.crudifier.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ICrudService<Entity> {

    List<Entity> find(Entity obj);

    List<Entity> find(Entity obj, Sort sort);

    Page<Entity> find(Entity obj, Pageable pageable);

    Entity find(UUID id);

    Entity create(Entity obj);

    Entity update(UUID id, Map<String, Object> fields);

    void delete(UUID obj);
}
