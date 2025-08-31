package org.lucentrix.metaframe.util;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.LinkedHashSet;

public class ReflectionUtil {

    public static <T> T createInstance(String className, Object... args) {
        try {
            Class<?> clazz = Class.forName(className);

            // If no arguments, try to use default constructor
            if (args == null || args.length == 0) {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                return (T) constructor.newInstance();
            }

            // Get argument types
            Class<?>[] argTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }

            // Try to find a matching constructor
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length != argTypes.length) continue;

                boolean match = true;
                for (int i = 0; i < paramTypes.length; i++) {
                    if (args[i] == null) continue; // null matches any reference type
                    if (!paramTypes[i].isAssignableFrom(argTypes[i])) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    constructor.setAccessible(true);
                    return (T) constructor.newInstance(args);
                }
            }

            throw new NoSuchMethodException("No matching constructor found for " + className);
        } catch (Exception ex) {
            throw new RuntimeException("Error creating instance", ex);
        }
    }

    public static <T> T createInstance(Class<T> clazz, Object... paramValues) {
        Constructor<?>[] constructors = clazz.getConstructors();
        boolean noParamsMode = paramValues == null || paramValues.length == 0;
        try {
            Constructor<?> defaultConstructor = null;

            for (Constructor<?> constructor : constructors) {
                Class<?>[] constructorParams = constructor.getParameterTypes();

                if (constructorParams.length == 0) {
                    defaultConstructor = constructor;
                }

                if (noParamsMode) {
                    if (constructorParams.length == 0) {
                        return (T) constructor.newInstance();
                    }
                } else if (constructorParams.length == paramValues.length) {
                    boolean typesOK = true;
                    for (int paramIndex = 0; paramIndex < paramValues.length; paramIndex++) {
                        Object paramValue = paramValues[paramIndex];
                        if (paramValue != null) {
                            if (!constructorParams[paramIndex].isAssignableFrom(paramValue.getClass())) {
                                typesOK = false;
                                break;
                            }
                        }
                    }

                    if (typesOK) {
                        return (T) constructor.newInstance(paramValues);
                    }
                }
            }

            if (defaultConstructor != null) {
                //using default constructor if no matches found
                return (T) defaultConstructor.newInstance();
            }
        } catch (InvocationTargetException e) {
            String message = "Exception in object constructor for: " + clazz;
            throw new RuntimeException(message, e.getTargetException());
        } catch (Exception e) {
            String message = "Class construction problem for class: " + clazz;
            throw new RuntimeException(message, e);
        }

        throw new RuntimeException("Appropriate or default constructor is not found for class " + clazz + " and parameters: " + Arrays.toString(paramValues));
    }

    public static <T> T buildProxyInstance(Class<? extends T> clazz, Object... params)
    {

        Object componentObject;

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
        try {
            componentObject = createInstance(clazz, params);

            return (T) wrapProxy(componentObject);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    public static <T> T wrapProxy(Object componentObject)
    {
        LinkedHashSet<Class<?>> interfaces = collectInterfaces(componentObject.getClass(), null);

        componentObject = Proxy.newProxyInstance(
                ReflectionUtil.class.getClassLoader(),
                interfaces.toArray(new Class<?>[interfaces.size()]),
                new ObjectProxy(componentObject)
        );

        return (T) componentObject;
    }

    public static class ObjectProxy implements InvocationHandler {

        Object wrapped;

        public ObjectProxy(Object wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

            ClassLoader wrappedClassLoader =  wrapped.getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(wrappedClassLoader);
            try {

                return method.invoke(wrapped, args);
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }

        public Object getWrapped() {
            return wrapped;
        }
    }

    public static LinkedHashSet<Class<?>> collectInterfaces(Class clazz, LinkedHashSet<Class<?>> interfaces) {
        if(interfaces == null) interfaces = new LinkedHashSet<>();

        if(clazz == null) {
            return interfaces;
        }

        Class<?>[] classes = clazz.getInterfaces();

        if(classes != null) {
            interfaces.addAll(Arrays.asList(classes));
        }

        return collectInterfaces(clazz.getSuperclass(), interfaces);
    }
}
