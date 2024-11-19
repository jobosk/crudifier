package com.jobosk.crudifier.util;

import java.util.Optional;
import java.util.UUID;

public class FormatUtil {

    public static Optional<UUID> getUUID(final Object value) {
        if (value instanceof UUID) {
            return Optional.of((UUID) value);
        }
        return getString(value)
                .flatMap(FormatUtil::getUUID);
    }

    public static Optional<UUID> getUUID(final String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<String> getString(final Object value) {
        if (value instanceof String) {
            return Optional.of((String) value);
        }
        return Optional.ofNullable(value)
                .map(String::valueOf);
    }
}
