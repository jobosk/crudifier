package com.jobosk.crudifier.controller;

import com.jobosk.crudifier.constant.CrudConstant;
import com.jobosk.crudifier.service.ICrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public abstract class CrudController<Entity> {

    @Autowired
    private ICrudService<Entity> service;

    @InitBinder("searchFilter")
    public static void formatRequestParams(final WebDataBinder binder, final WebRequest webRequest) {
        binder.registerCustomEditor(Date.class, getDateEditor());
    }

    private static PropertyEditorSupport getDateEditor() {
        return new PropertyEditorSupport() {

            @Override
            public void setAsText(final String text) {
                setValue(toDate(text));
            }

            private Date toDate(final String value) {
                if (value == null || value.trim().isEmpty()) {
                    return null;
                }
                return new Date(Long.parseLong(value));
            }
        };
    }

    @GetMapping()
    public @ResponseBody
    Collection<Entity> findAll(
            @ModelAttribute(value = "searchFilter") final Entity entity
            , @RequestParam(value = "page", required = false) final Integer page
            , @RequestParam(value = "size", required = false) final Integer size
            , @RequestParam(value = "order", required = false) final String order
            , final HttpServletResponse response
    ) {
        Collection<Entity> result;
        final Sort sort = getSort(order);
        if (page != null && size != null) {
            final Pageable pageRequest = sort != null ? PageRequest.of(page, size, sort) : PageRequest.of(page, size);
            Page<Entity> pagedResult = service.find(entity, pageRequest);
            response.addHeader(CrudConstant.Http.Header.TOTAL_COUNT, String.valueOf(pagedResult.getTotalElements()));
            response.addHeader(CrudConstant.Http.Header.EXPOSE_HEADER, CrudConstant.Http.Header.TOTAL_COUNT);
            result = pagedResult.getContent();
        } else {
            result = sort != null ? service.find(entity, sort) : service.find(entity);
        }
        return result;
    }

    private Sort getSort(final String sort) {
        if (sort == null) {
            return null;
        }
        final String[] list = sort.split(",");
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
