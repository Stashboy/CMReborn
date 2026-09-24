package dev.cmreborn;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/** Implements the host's future interface even when its listener member is renamed. */
final class ImmediateFutureFactory {
    private ImmediateFutureFactory() {}

    static Object create(Class<?> contract, Object value) {
        if (!contract.isInterface() || !Future.class.isAssignableFrom(contract)) {
            throw new IllegalArgumentException("Not a Future interface");
        }
        Method listener = null;
        for (Method method : contract.getMethods()) {
            if (method.getDeclaringClass() == Future.class) continue;
            if (method.getReturnType() != void.class || !Arrays.equals(method.getParameterTypes(),
                    new Class<?>[]{Runnable.class, Executor.class}) || listener != null) {
                throw new IllegalArgumentException("Unsupported future contract");
            }
            listener = method;
        }
        if (listener == null) throw new IllegalArgumentException("Missing future listener");
        final Method listenerMethod = listener;
        return Proxy.newProxyInstance(contract.getClassLoader(), new Class<?>[]{contract},
                (proxy, method, args) -> {
                    if (method.equals(listenerMethod)) {
                        Runnable task = (Runnable) Objects.requireNonNull(args[0]);
                        Executor executor = (Executor) Objects.requireNonNull(args[1]);
                        // Match immediate-future semantics: executor rejection does not undo completion.
                        try { executor.execute(task); } catch (RuntimeException ignored) { }
                        return null;
                    }
                    switch (method.getName()) {
                        case "get":
                            if (args != null && args.length == 2) Objects.requireNonNull(args[1]);
                            return value;
                        case "isDone": return Boolean.TRUE;
                        case "isCancelled":
                        case "cancel": return Boolean.FALSE;
                        case "equals": return proxy == args[0];
                        case "hashCode": return System.identityHashCode(proxy);
                        case "toString": return "CMReborn completed future";
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
    }
}
