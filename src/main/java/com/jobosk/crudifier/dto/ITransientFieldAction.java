package com.jobosk.crudifier.dto;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface ITransientFieldAction<T, R> {
    String getTransientField();

    Function<Object, Optional<R>> getGetter();

    BiConsumer<T, R> getSetter();
}
