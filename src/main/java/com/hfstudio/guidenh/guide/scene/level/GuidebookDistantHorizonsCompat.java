package com.hfstudio.guidenh.guide.scene.level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraftforge.common.MinecraftForge;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Shields guide preview worlds from Distant Horizons world-load hooks.
 */
public final class GuidebookDistantHorizonsCompat {

    public static final String FORGE_CLIENT_PROXY_CLASS_NAME = "com.seibel.distanthorizons.forge.ForgeClientProxy";

    private static final Object SUPPRESSION_LOCK = new Object();

    private static int suppressionDepth;
    private static List<SuspendedTarget> suspendedTargets = Collections.emptyList();

    @Nullable
    private static volatile Field listenersField;
    @Nullable
    private static volatile Field listenerOwnersField;
    @Nullable
    private static volatile Method privateRegisterMethod;
    private static volatile boolean reflectionResolved;

    private GuidebookDistantHorizonsCompat() {}

    public interface SuppressionToken extends AutoCloseable {

        @Override
        void close();
    }

    public static SuppressionToken suppressClientWorldLoadHooks() {
        return suppressClientWorldLoadHooks(MinecraftForge.EVENT_BUS);
    }

    public static SuppressionToken suppressClientWorldLoadHooks(EventBus eventBus) {
        synchronized (SUPPRESSION_LOCK) {
            if (suppressionDepth == 0) {
                suspendedTargets = suspendTargets(eventBus);
            }
            suppressionDepth++;
        }
        return new EventBusSuppressionToken(eventBus);
    }

    public static boolean isDistantHorizonsForgeClientProxy(@Nullable Object target) {
        return target != null && FORGE_CLIENT_PROXY_CLASS_NAME.equals(
            target.getClass()
                .getName());
    }

    private static List<SuspendedTarget> suspendTargets(EventBus eventBus) {
        Map<Object, ?> listeners = getListenersMap(eventBus);
        if (listeners == null || listeners.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Object, ModContainer> listenerOwners = getListenerOwnersMap(eventBus);
        Object[] targets = listeners.keySet()
            .toArray();
        List<SuspendedTarget> removedTargets = new ArrayList<>();
        for (Object target : targets) {
            if (!isDistantHorizonsForgeClientProxy(target)) {
                continue;
            }
            removedTargets.add(new SuspendedTarget(target, listenerOwners != null ? listenerOwners.get(target) : null));
            eventBus.unregister(target);
        }
        return removedTargets;
    }

    private static void restoreTargets(EventBus eventBus, List<SuspendedTarget> targets) {
        if (targets.isEmpty()) {
            return;
        }

        Map<Object, ?> listeners = getListenersMap(eventBus);
        for (SuspendedTarget target : targets) {
            if (listeners != null && listeners.containsKey(target.target)) {
                continue;
            }
            registerTarget(eventBus, target);
        }
    }

    private static void registerTarget(EventBus eventBus, SuspendedTarget target) {
        Method registerMethod = getPrivateRegisterMethod();
        if (registerMethod == null) {
            return;
        }

        Map<Object, ModContainer> listenerOwners = getListenerOwnersMap(eventBus);
        if (listenerOwners != null && target.owner != null) {
            listenerOwners.put(target.target, target.owner);
        }

        for (Method method : target.target.getClass()
            .getMethods()) {
            if (!method.isAnnotationPresent(SubscribeEvent.class)) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1 || !Event.class.isAssignableFrom(parameterTypes[0])) {
                continue;
            }

            try {
                registerMethod.invoke(eventBus, parameterTypes[0], target.target, method, target.owner);
            } catch (ReflectiveOperationException ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Map<Object, ?> getListenersMap(EventBus eventBus) {
        Field field = getListenersField();
        if (field == null) {
            return null;
        }
        try {
            return (Map<Object, ?>) field.get(eventBus);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Map<Object, ModContainer> getListenerOwnersMap(EventBus eventBus) {
        Field field = getListenerOwnersField();
        if (field == null) {
            return null;
        }
        try {
            return (Map<Object, ModContainer>) field.get(eventBus);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    @Nullable
    private static Field getListenersField() {
        ensureReflectionResolved();
        return listenersField;
    }

    @Nullable
    private static Field getListenerOwnersField() {
        ensureReflectionResolved();
        return listenerOwnersField;
    }

    @Nullable
    private static Method getPrivateRegisterMethod() {
        ensureReflectionResolved();
        return privateRegisterMethod;
    }

    private static void ensureReflectionResolved() {
        if (reflectionResolved) {
            return;
        }
        synchronized (GuidebookDistantHorizonsCompat.class) {
            if (reflectionResolved) {
                return;
            }
            listenersField = resolveField("listeners");
            listenerOwnersField = resolveField("listenerOwners");
            privateRegisterMethod = resolvePrivateRegisterMethod();
            reflectionResolved = true;
        }
    }

    @Nullable
    private static Field resolveField(String fieldName) {
        try {
            Field field = EventBus.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Nullable
    private static Method resolvePrivateRegisterMethod() {
        try {
            Method method = EventBus.class
                .getDeclaredMethod("register", Class.class, Object.class, Method.class, ModContainer.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void release(EventBus eventBus) {
        synchronized (SUPPRESSION_LOCK) {
            if (suppressionDepth == 0) {
                return;
            }

            suppressionDepth--;
            if (suppressionDepth == 0) {
                restoreTargets(eventBus, suspendedTargets);
                suspendedTargets = Collections.emptyList();
            }
        }
    }

    private static final class EventBusSuppressionToken implements SuppressionToken {

        private final EventBus eventBus;
        private boolean closed;

        private EventBusSuppressionToken(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        @Override
        public void close() {
            synchronized (SUPPRESSION_LOCK) {
                if (closed) {
                    return;
                }
                closed = true;
            }
            release(eventBus);
        }
    }

    private static final class SuspendedTarget {

        private final Object target;
        @Nullable
        private final ModContainer owner;

        private SuspendedTarget(Object target, @Nullable ModContainer owner) {
            this.target = target;
            this.owner = owner;
        }
    }
}
