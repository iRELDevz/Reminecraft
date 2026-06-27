package org.reminecraft.gpu;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class NmsBridge {

    private final boolean available;
    private final ConcurrentHashMap<Class<?>, Method> handleMethods = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Method> navMethods = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Method> doneMethods = new ConcurrentHashMap<>();

    private NmsBridge(boolean available) {
        this.available = available;
    }

    public static NmsBridge probe(Logger logger) {
        try {
            Class.forName("org.bukkit.craftbukkit.entity.CraftEntity");
            return new NmsBridge(true);
        } catch (ClassNotFoundException e) {
            logger.info("NMS bridge tidak aktif (CraftEntity tidak ditemukan, mapping berbeda).");
            return new NmsBridge(false);
        }
    }

    public boolean available() {
        return available;
    }

    public boolean isPathfinding(Entity entity) {
        if (!available || !(entity instanceof Mob)) return false;
        try {
            Object handle = handle(entity);
            if (handle == null) return false;
            Object nav = navigation(handle);
            if (nav == null) return false;
            Method done = doneMethods.computeIfAbsent(nav.getClass(), NmsBridge::findDone);
            if (done == null) return false;
            return !((Boolean) done.invoke(nav));
        } catch (Throwable t) {
            return false;
        }
    }

    private Object handle(Entity entity) throws Exception {
        Method m = handleMethods.computeIfAbsent(entity.getClass(), NmsBridge::findHandle);
        return m == null ? null : m.invoke(entity);
    }

    private Object navigation(Object handle) throws Exception {
        Method m = navMethods.computeIfAbsent(handle.getClass(), NmsBridge::findNavigation);
        return m == null ? null : m.invoke(handle);
    }

    private static Method findHandle(Class<?> type) {
        try {
            Method m = type.getMethod("getHandle");
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method findNavigation(Class<?> type) {
        try {
            Method m = type.getMethod("getNavigation");
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method findDone(Class<?> type) {
        try {
            Method m = type.getMethod("isDone");
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
