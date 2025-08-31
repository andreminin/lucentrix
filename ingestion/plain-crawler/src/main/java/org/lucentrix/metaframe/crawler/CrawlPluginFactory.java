package org.lucentrix.metaframe.crawler;

import org.lucentrix.metaframe.runtime.plugin.*;
import org.pf4j.Plugin;
import org.pf4j.PluginFactory;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class CrawlPluginFactory implements PluginFactory {
    private final Logger logger = LoggerFactory.getLogger(CrawlPluginFactory.class);

    private final CrawlContext context;

    public CrawlPluginFactory(CrawlContext context) {
        this.context = context;
    }

    @Override
    public Plugin create(PluginWrapper wrapper) {
        String pluginClassName = wrapper.getDescriptor().getPluginClass();
        String pluginId = wrapper.getDescriptor().getPluginId();
        logger.debug("Creating instance of plugin class '{}'", pluginClassName);

        Class<?> pluginClass;
        try {
            pluginClass = wrapper.getPluginClassLoader().loadClass(pluginClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error instantiating plugin, class not found", e);
        }

        //Detect generics
        Class<?>[] generics = resolveGenerics(pluginClass);

        if (generics == null) {
            throw new RuntimeException("Generics class is not found for class " + pluginClassName);
        }

        if (generics.length < 2) {
            throw new RuntimeException("Generics count is less than 2 in class " + pluginClassName);
        }

        Class<?> configClass = generics[0];
        Class<?> contextClass = generics[1];

        logger.debug("Plugin configuration class: {}, context class: {}", configClass, contextClass);

        try {
            PluginConfig<?> pluginConfig;
            Constructor<?> constructor = findBestMatchingConstructor(configClass, InputStream.class, ConfigEnv.class);
            if(constructor == null) {
                constructor = configClass.getConstructor();
                pluginConfig = (PluginConfig<?>) constructor.newInstance();
            } else {
                Supplier<InputStream> streamSupplier = context.getConfig().getPluginConfig(pluginId);
                Objects.requireNonNull(streamSupplier);
                pluginConfig = (PluginConfig<?>) constructor.newInstance(streamSupplier.get(), context.getConfigEnv());
            }

            PluginContext pluginContext = createPluginContext(pluginClassName, contextClass);

            constructor = findBestMatchingConstructor(pluginClass, configClass, contextClass);
            if(constructor != null) {
                constructor.setAccessible(true);

                try {
                    return (Plugin) constructor.newInstance(pluginConfig, pluginContext);
                } catch (Exception ex) {
                    throw new RuntimeException("Error instantiating plugin id="+pluginId+" from class "+pluginClass +
                            " using arguments: "+pluginConfig +", "+pluginContext, ex);
                }
            }
            //Looking for constructor without config
          /*  for (Constructor<?> constructor : pluginClass.getDeclaredConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 2 &&
                        PluginConfig.class.isAssignableFrom(params[0]) &&
                        PluginContext.class.isAssignableFrom(params[1])) {
                    constructor.setAccessible(true);
                    return (Plugin) constructor.newInstance(wrapper, pluginContext);
                }
            }
            if(constructor != null) {
                constructor.setAccessible(true);
                return (Plugin) constructor.newInstance(wrapper, pluginContext);
            }*/
            throw new RuntimeException("Unable to find matching constructor(" + PluginWrapper.class + "," +
                    PluginContext.class + " in plugin " + pluginClassName);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to instantiate plugin: " + pluginClassName, ex);
        }
    }


    private PluginContext createPluginContext(String pluginId, Class<?> contextClass) {
        if (RetrieverPluginContext.class.isAssignableFrom(contextClass)) {
            return context.createRetrieverPluginContext(pluginId);
        } else if (ConsumerPluginContext.class.isAssignableFrom(contextClass)) {
            return context.createConsumerPluginContext(pluginId);
        } else if (PluginContext.class.isAssignableFrom(contextClass)) {
            return context.createPlainPluginContext(pluginId);
        } else {
            throw new RuntimeException("Unsupported plugin context type: " + contextClass);
        }
    }

    public static Class<?>[] resolveGenerics(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        Type[] genericInterfaces = clazz.getGenericInterfaces();

        Type parameterized = clazz.getGenericSuperclass();

        if (parameterized instanceof ParameterizedType paramType) {
            Type[] genericsTypes = paramType.getActualTypeArguments();

            List<Class<?>> genericClasses = new ArrayList<>();

            for (Type genericsType : genericsTypes) {
                if (genericsType instanceof Class) {
                    genericClasses.add((Class<?>) genericsType);
                }
            }

            return genericClasses.toArray(new Class<?>[0]);
        } else {

            for (Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof ParameterizedType) {
                    Type[] genericsTypes = ((ParameterizedType) genericInterface).getActualTypeArguments();

                    List<Class<?>> generics = new ArrayList<>();
                    for (Type genericsType : genericsTypes) {
                        if (genericsType instanceof Class) {
                            generics.add((Class<?>) genericsType);
                        } else if (genericsType instanceof ParameterizedType parameterizedType) {
                            generics.add((Class<?>) parameterizedType.getRawType());
                            Type[] typeArgs = parameterizedType.getActualTypeArguments();
                            for (Type typeArg : typeArgs) {
                                if (typeArg instanceof Class) {
                                    generics.add((Class<?>) typeArg);
                                }
                            }
                        } else {
                            throw new RuntimeException("Unsupported genetics type: " + genericsType);
                        }
                    }

                    return generics.toArray(new Class[0]);
                }
            }

            Class<?> superClass = clazz.getSuperclass();

            return resolveGenerics(superClass);
        }
    }

    public static Constructor<?> findBestMatchingConstructor(Class<?> clazz, Class<?>... argTypes) {
        Constructor<?>[] constructors = clazz.getConstructors();
        Constructor<?> bestMatch = null;
        int bestScore = Integer.MAX_VALUE;

        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length != argTypes.length) continue;

            int score = getMatchScore(paramTypes, argTypes);
            if (score >= 0 && score < bestScore) {
                bestScore = score;
                bestMatch = constructor;
            }
        }

        return bestMatch;
    }

    private static int getMatchScore(Class<?>[] paramTypes, Class<?>[] argTypes) {
        int score = 0;
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> expected = paramTypes[i];
            Class<?> actual = argTypes[i];

            if (actual == null) {
                // Allow null to match any reference type
                if (expected.isPrimitive()) return -1;
                score += 1; // Prefer exact match over null
            } else if (expected.isAssignableFrom(actual)) {
                if (!expected.equals(actual)) {
                    score += 1; // Add penalty for upcasting
                }
            } else if (isPrimitiveMatch(expected, actual)) {
                score += 1; // Allow primitive boxing/unboxing match
            } else {
                return -1; // Not assignable
            }
        }
        return score;
    }

    private static boolean isPrimitiveMatch(Class<?> expected, Class<?> actual) {
        // Boxed → primitive match (e.g., Integer → int)
        if (expected.isPrimitive()) {
            return
                    (expected == boolean.class && actual == Boolean.class) ||
                            (expected == byte.class && actual == Byte.class) ||
                            (expected == short.class && actual == Short.class) ||
                            (expected == int.class && actual == Integer.class) ||
                            (expected == long.class && actual == Long.class) ||
                            (expected == float.class && actual == Float.class) ||
                            (expected == double.class && actual == Double.class) ||
                            (expected == char.class && actual == Character.class);
        }
        return false;
    }
}