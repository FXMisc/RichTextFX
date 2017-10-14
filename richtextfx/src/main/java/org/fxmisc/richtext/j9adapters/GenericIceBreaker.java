package org.fxmisc.richtext.j9adapters;

import java.lang.reflect.*;

public class GenericIceBreaker implements InvocationHandler {
    private final Object delegate;

    public GenericIceBreaker(Object delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method delegateMethod = delegate.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
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
}
