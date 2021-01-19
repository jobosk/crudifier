package com.jobosk.crudifier.exception;

public class CrudException extends RuntimeException {

    private Throwable original;
    private String description;

    public CrudException(final Throwable throwable, final String description) {
        this.original = throwable;
        this.description = description;
    }

    public Throwable getOriginal() {
        return original;
    }

    public void setOriginal(Throwable original) {
        this.original = original;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
