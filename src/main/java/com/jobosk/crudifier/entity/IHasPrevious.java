package com.jobosk.crudifier.entity;

import com.jobosk.crudifier.util.ModelUtil;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.Optional;

public interface IHasPrevious<ID> extends ICrudEntity<ID> {

    default IHasPrevious<ID> getMeAsNext() {
        return (IHasPrevious<ID>) getThis();
    }

    default <Previous extends IHasNext<ID>> void setPrevious(final Previous previous) {
        setPrevious(previous, true);
    }

    default <Previous extends IHasNext<ID>> void setPrevious(final Previous previous, final boolean reverse) {
        if (previous != null) {
            final BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(getMeAsNext());
            Optional.ofNullable(wrapper.getPropertyValue("previous"))
                    .filter(IHasNext.class::isInstance)
                    .map(IHasNext.class::cast)
                    .ifPresent(currentPrevious -> {
                        ModelUtil.setFromOne(getMeAsNext(), currentPrevious, previous, reverse, IHasNext::setNext);
                        wrapper.setPropertyValue("previous", previous);
                    });
        }
    }
}
