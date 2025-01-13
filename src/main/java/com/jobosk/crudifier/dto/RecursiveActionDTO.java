package com.jobosk.crudifier.dto;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RecursiveActionDTO<T, R> {
    public String field;
    public Function<T, List<R>> getter;
    public Collection<ITransientFieldAction> actions;
    public BiConsumer<T, R> setter;
    public Class<R> type;
    public Supplier<R> builder;
    public boolean selfRecursive;

    public RecursiveActionDTO(
            final String field
            , final Function<T, List<R>> getter
            , final Collection<ITransientFieldAction> actions
            , final BiConsumer<T, R> setter
            , final Class<R> type
            , final Supplier<R> builder
            , final boolean selfRecursive
    ) {
        this.field = field;
        this.getter = getter;
        this.actions = actions;
        this.setter = setter;
        this.type = type;
        this.builder = builder;
        this.selfRecursive = selfRecursive;
    }
}
