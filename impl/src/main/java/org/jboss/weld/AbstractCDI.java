/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld;

import static org.jboss.weld.util.reflection.Reflections.cast;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;

import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.logging.BeanManagerLogger;

import com.google.common.collect.ImmutableSet;

/**
 * Abstract implementation of CDI which forwards all Instance methods to a delegate. Furthermore, it allows the calling class to be identified
 * using the {@link #getCallingClassName()} method.
 *
 * @author Jozef Hartinger
 *
 * @param <T>
 */
public abstract class AbstractCDI<T> extends CDI<T> {

    // used for caller detection
    protected final Set<String> subclassNames;

    public AbstractCDI() {
        Set<String> names = new HashSet<String>();
        for (Class<?> clazz = getClass(); clazz != CDI.class; clazz = clazz.getSuperclass()) {
            names.add(clazz.getName());
        }
        this.subclassNames = ImmutableSet.copyOf(names);
    }

    @Override
    public Iterator<T> iterator() {
        return getInstance().iterator();
    }

    @Override
    public T get() {
        return getInstance().get();
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        return getInstance().select(qualifiers);
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        return getInstance().select(subtype, qualifiers);
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        return getInstance().select(subtype, qualifiers);
    }

    @Override
    public boolean isUnsatisfied() {
        return getInstance().isUnsatisfied();
    }

    @Override
    public boolean isAmbiguous() {
        return getInstance().isAmbiguous();
    }

    @Override
    public void destroy(T instance) {
        getInstance().destroy(instance);
    }

    /**
     * Examines {@link StackTraceElement}s to figure out which class invoked a method on {@link CDI}.
     */
    protected String getCallingClassName() {
        boolean outerSubclassReached = false;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            // the method call that leads to the first invocation of this class or its subclass is considered the caller
            if (!subclassNames.contains(element.getClassName())) {
                if (outerSubclassReached) {
                    return element.getClassName();
                }
            } else {
                outerSubclassReached = true;
            }
        }
        throw BeanManagerLogger.LOG.unableToIdentifyBeanManager();
    }

    private Instance<T> getInstance() {
        return cast(BeanManagerProxy.unwrap(getBeanManager()).instance());
    }
}
