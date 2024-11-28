package com.jobosk.crudifier.exception.handler;

import com.jobosk.crudifier.exception.CrudException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

public interface ICrudExceptionHandler {

    default String buildMessage(final CrudException e) {
        return buildMessage(e, new ArrayList<>());
    }

    default String buildMessage(final CrudException e, final List<Object> parameters) {
        final StringBuilder builder = new StringBuilder();
        if (e != null) {
            builder.append(e.getCode());
            final Collection<Object> params = getParameters(e.getParams(), parameters);
            if (!params.isEmpty()) {
                builder.append(" |");
                for (final Object param : params) {
                    builder.append(" ").append(param);
                }
            }
            getOriginalCause(e.getOriginal())
                    .ifPresent(cause -> builder.append(" [").append(cause).append("]"));
        }
        return builder.toString();
    }

    default <T> Collection<T> getParameters(final List<T> pms1, final List<T> pms2) {
        final Set<T> result = new HashSet<>();
        Optional.ofNullable(pms1)
                .ifPresent(result::addAll);
        Optional.ofNullable(pms2)
                .ifPresent(result::addAll);
        return result;
    }

    default Optional<String> getOriginalCause(final Throwable throwable) {
        return Optional.ofNullable(throwable)
                .flatMap(t -> getOriginalCause(t.getCause())
                        .or(() -> Optional.ofNullable(t.getMessage()))
                );
    }

    default String getMessage(final CrudException exception) {
        return buildMessage(exception);
    }

    default String getMessage(final CrudException exception, final List<Object> parameters) {
        return buildMessage(exception, parameters);
    }

    void processDomainException(final CrudException ce, final HttpServletResponse response);

    @ExceptionHandler(value = CrudException.class)
    void handleDomainException(final CrudException ce, final HttpServletResponse response);
}