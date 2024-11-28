package com.jobosk.crudifier.exception;

import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public class CrudException extends Exception {
    private HttpStatus status;
    private String code;
    private Throwable original;
    private List<Object> params;

    public CrudException(final HttpStatus status, final String code, final Throwable original, final List<Object> params) {
        this.status = status;
        this.code = code;
        this.original = original;
        this.params = params;
    }

    public CrudException(final HttpStatus status, final String code, final Throwable original) {
        this(status, code, original, new ArrayList<>());
    }

    public CrudException(final HttpStatus status, final String code, final List<Object> params) {
        this(status, code, null, params);
    }

    public CrudException(final HttpStatus status, final String code) {
        this(status, code, null, new ArrayList<>());
    }

    public CrudException(final String code, final Throwable original) {
        this(HttpStatus.INTERNAL_SERVER_ERROR, code, original, new ArrayList<>());
    }

    public CrudException(final String code, final List<Object> params) {
        this(HttpStatus.INTERNAL_SERVER_ERROR, code, null, params);
    }

    public CrudException(final String code) {
        this(HttpStatus.INTERNAL_SERVER_ERROR, code, null, new ArrayList<>());
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Throwable getOriginal() {
        return original;
    }

    public void setOriginal(Throwable original) {
        this.original = original;
    }

    public List<Object> getParams() {
        return params;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }
}
