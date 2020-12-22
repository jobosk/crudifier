package com.jobosk.crudifier.controller;

import com.jobosk.crudifier.constant.CrudConstant;
import com.jobosk.crudifier.service.ICrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public abstract class CrudController<Entity> {

    @Autowired
    private ICrudService<Entity> service;

    @GetMapping()
    public @ResponseBody
    Collection<Entity> findAll(
            @RequestParam Map<String, Object> parameters
            , final HttpServletResponse response
    ) {
        Collection<Entity> result;
        final Sort sort = getSort(getParameter(parameters, "order"));
        final Object page = getParameter(parameters, "page");
        final Object size = getParameter(parameters, "size");
        if (page instanceof Integer && size instanceof Integer) {
            final Pageable pageRequest = getPageRequest((Integer) page, (Integer) size, sort);
            Page<Entity> pagedResult = service.find(parameters, pageRequest);
            response.addHeader(CrudConstant.Http.Header.TOTAL_COUNT, String.valueOf(pagedResult.getTotalElements()));
            response.addHeader(CrudConstant.Http.Header.EXPOSE_HEADER, CrudConstant.Http.Header.TOTAL_COUNT);
            result = pagedResult.getContent();
        } else {
            result = sort != null ? service.find(parameters, sort) : service.find(parameters);
        }
        return result;
    }

    private Object getParameter(final Map<String, Object> parameters, final String key) {
        if (key == null || parameters == null) {
            return null;
        }
        final Object result = parameters.get(key);
        parameters.entrySet().removeIf(e -> key.equals(e.getKey()));
        return result;
    }

    private Sort getSort(final Object sort) {
        if (sort == null) {
            return null;
        }
        final String[] list = sort.toString().split(",");
        if (list.length == 0) {
            return null;
        }
        Sort result = getDirectionSort(list[0]);
        for (int i = 1; i < list.length; i++) {
            result = result.and(getDirectionSort(list[i]));
        }
        return result;
    }

    private Sort getDirectionSort(final String value) {
        final boolean isDesc;
        final String sort;
        if (value != null && value.charAt(0) == '-') {
            isDesc = true;
            sort = value.substring(1);
        } else {
            isDesc = false;
            sort = value;
        }
        return Sort.by((isDesc ? Sort.Direction.DESC : Sort.Direction.ASC), sort);
    }

    private Pageable getPageRequest(final int page, final int size, final Sort sort) {
        return sort != null ? PageRequest.of(page, size, sort) : PageRequest.of(page, size);
    }

    @PostMapping
    public @ResponseBody
    Entity create(@RequestBody final @Valid Entity entity) {
        return service.create(entity);
    }

    @PutMapping(path = "{" + CrudConstant.Http.Param.ID + "}")
    public @ResponseBody
    Entity update(@PathVariable(value = CrudConstant.Http.Param.ID) final UUID id, @RequestBody final Map<String, Object> fields) {
        return service.update(id, fields);
    }

    @DeleteMapping(path = "{" + CrudConstant.Http.Param.ID + "}")
    public void delete(@PathVariable(value = CrudConstant.Http.Param.ID) final UUID id) {
        service.delete(id);
    }

    @GetMapping(path = "{" + CrudConstant.Http.Param.ID + "}")
    public @ResponseBody
    Entity findOne(@PathVariable(CrudConstant.Http.Param.ID) final UUID id) {
        return service.find(id);
    }
}
