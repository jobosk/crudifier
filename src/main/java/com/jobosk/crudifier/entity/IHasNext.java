package com.jobosk.crudifier.entity;

import com.jobosk.crudifier.util.ModelUtil;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.Optional;

public interface IHasNext<ID> extends ICrudEntity<ID> {

    default IHasNext<ID> getMeAsPrevious() {
        return (IHasNext<ID>) getThis();
    }

    default <Next extends IHasPrevious<ID>> void setNext(final Next next) {
        setNext(next, true);
    }

    default <Next extends IHasPrevious<ID>> void setNext(final Next next, final boolean reverse) {
        if (next != null) {
            final BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(getMeAsPrevious());
            Optional.ofNullable(wrapper.getPropertyValue("next"))
                    .filter(IHasPrevious.class::isInstance)
                    .map(IHasPrevious.class::cast)
                    .ifPresent(currentNext -> {
                        ModelUtil.setFromOne(getMeAsPrevious(), currentNext, next, reverse, IHasPrevious::setPrevious);
                        wrapper.setPropertyValue("next", next);
                    });
        }
    }
}
