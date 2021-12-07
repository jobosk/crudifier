package com.jobosk.crudifier.controller;

import com.jobosk.crudifier.constant.CrudConstant;
import com.jobosk.crudifier.service.ICrudService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Map;

public abstract class CrudController<Entity, Id> {

    protected static final String ID_PATH = "{" + CrudConstant.Http.Param.ID + "}";

    @Autowired
    protected ICrudService<Entity, Id> service;

    public abstract Entity findOne(Id id);

    public abstract Collection<Entity> findAll(Map<String, String> parameters, HttpServletResponse response);

    public abstract Entity create(@Valid Entity entity);

    public abstract Entity update(@Valid Entity entity, Map<String, Object> fields);

    public abstract boolean delete(Id id);
}
