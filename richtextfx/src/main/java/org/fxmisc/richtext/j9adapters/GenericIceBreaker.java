package org.fxmisc.richtext.j9adapters;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;

public class GenericIceBreaker implements InvocationHandler {
    private final Object delegate;

    public GenericIceBreaker(Object delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method delegateMethod = getDeclaredMethod(delegate.getClass(), method.getName(), method.getParameterTypes());
//TODO        if (!delegateMethod.canAccess(delegate)) {
            delegateMethod.setAccessible(true);
//        }

        Object delegateMethodReturn = null;
        try {
            delegateMethodReturn = delegateMethod.invoke(delegate, args);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("problems invoking " + method.getName());
        }
        if (delegateMethodReturn == null) {
            return null;
        }

        if (method.getReturnType().isArray()) {
            if (method.getReturnType().getComponentType().isInterface()
                    && !method.getReturnType().getComponentType().equals(delegateMethod.getReturnType().getComponentType())) {

                int arrayLength = Array.getLength(delegateMethodReturn);
                Object retArray = Array.newInstance(method.getReturnType().getComponentType(), arrayLength);
                for (int i = 0; i < arrayLength; i++) {
                    Array.set(retArray,
                            i,
                            proxy(
                                    method.getReturnType().getComponentType(),
                                    Array.get(delegateMethodReturn, i)));
                }

                return retArray;
            }
        }

        if (method.getReturnType().isInterface()
                && !method.getReturnType().equals(delegateMethod.getReturnType())) {
            return proxy(method.getReturnType(), delegateMethodReturn);
        }

        return delegateMethodReturn;
    }

    public static <T> T proxy(Class<T> iface, Object delegate) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class[]{iface},
                new GenericIceBreaker(delegate));
    }

    /* ********************************************************************** *
     *                                                                        *
     * Method cache                                                           *
     *                                                                        *
     * ********************************************************************** */

    private static final HashMap<MethodCacheKey, Method> declaredMethodCache = new HashMap<>();

    private static synchronized Method getDeclaredMethod(Class<?> cls, String name, Class<?>... paramTypes)
            throws NoSuchMethodException, SecurityException
    {
        MethodCacheKey methodCacheKey = new MethodCacheKey(cls, name, paramTypes);

        Method m = declaredMethodCache.get(methodCacheKey);
        if (m == null) {
            m = cls.getDeclaredMethod(name, paramTypes);
            declaredMethodCache.put(methodCacheKey, m);
        }
        return m;
    }

    private static class MethodCacheKey {
        final Class<?> cls;
        final String name;
        final Class<?>[] paramTypes;

        public MethodCacheKey(Class<?> cls, String name, Class<?>... paramTypes) {
            this.cls = cls;
            this.name = name;
            this.paramTypes = paramTypes;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodCacheKey))
                return false;

            MethodCacheKey key2 = (MethodCacheKey) obj;
            return cls == key2.cls && name.equals(key2.name) && Arrays.equals(paramTypes, key2.paramTypes);
        }

        @Override
        public int hashCode() {
            return cls.hashCode() + name.hashCode() + Arrays.hashCode(paramTypes);
        }
    }
}
