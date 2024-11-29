package com.jobosk.crudifier.entity;

import com.jobosk.crudifier.util.ModelUtil;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.Optional;

public interface IHasParent<ID> extends ICrudEntity<ID> {

    default IHasParent<ID> getMeAsChild() {
        return (IHasParent<ID>) getThis();
    }

    default <Parent extends IHasChild<ID>> void setParent(final Parent parent) {
        setParent(parent, true);
    }

    default <Parent extends IHasChild<ID>> void setParent(final Parent parent, final boolean reverse) {
        final BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(getMeAsChild());
        Optional.ofNullable(wrapper.getPropertyValue("parent"))
                .filter(IHasChild.class::isInstance)
                .map(IHasChild.class::cast)
                .ifPresent(currentParent -> {
                    ModelUtil.setFromOne(getMeAsChild(), currentParent, parent, reverse, IHasChild::addChild);
                    wrapper.setPropertyValue("parent", parent);
                });
    }
}
