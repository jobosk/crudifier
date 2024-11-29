package com.jobosk.crudifier.entity;

import com.jobosk.crudifier.util.ModelUtil;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface IHasChild<ID> extends ICrudEntity<ID> {

    default IHasChild<ID> getMeAsParent() {
        return (IHasChild<ID>) getThis();
    }

    default <Child extends IHasParent<ID>> void setChildren(final List<Child> children) {
        final BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(getThis());
        Optional.ofNullable(wrapper.getPropertyValue("children"))
                .filter(Collection.class::isInstance)
                .map(Collection.class::cast)
                .ifPresent(c -> {
                    c.clear();
                    if (children != null) {
                        children.forEach(child -> {
                            child.setParent(getMeAsParent(), false);
                            getChildAction()
                                    .ifPresent(action -> action.accept(child));
                        });
                        c.addAll(children);
                    }
                });
    }

    default <Child extends IHasParent<ID>> void addChild(final Child child) {
        addChild(child, true);
    }

    default <Child extends IHasParent<ID>> void addChild(final Child child, final boolean reverse) {
        if (child != null) {
            final BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(getMeAsParent());
            Optional.ofNullable(wrapper.getPropertyValue("children"))
                    .filter(Collection.class::isInstance)
                    .map(Collection.class::cast)
                    .ifPresent(currentChildren -> {
                        getChildAction()
                                .ifPresent(action -> action.accept(child));
                        ModelUtil.setFromMany(getMeAsParent(), child, reverse, IHasParent::setParent);
                        currentChildren.add(child);
                    });
        }
    }

    default <Child extends IHasParent<ID>> Optional<Consumer<Child>> getChildAction() {
        return Optional.empty(); // By default
    }
}
