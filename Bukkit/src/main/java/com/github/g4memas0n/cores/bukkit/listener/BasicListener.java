package com.github.g4memas0n.cores.bukkit.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Method;

public abstract class BasicListener<T extends JavaPlugin> implements Listener {

    protected T plugin;

    public boolean register(@NotNull final T plugin) {
        if (this.plugin == null) {
            this.plugin = plugin;
            this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
            return true;
        }

        return false;
    }

    public boolean unregister(@NotNull final T plugin) {
        if (this.plugin == plugin) {
            HandlerList.unregisterAll(this);

            this.plugin = null;
            return true;
        }

        return false;
    }

    @Override
    public @NotNull String toString() {
        final StringBuilder events = new StringBuilder();
        EventHandler annotation;

        for (final Method method : getClass().getMethods()) {
            annotation = method.getAnnotation(EventHandler.class);

            if (annotation != null && method.getParameterCount() == 1) {
                events.append("{class='").append(method.getParameterTypes()[0].getSimpleName()).append("', ");
                events.append("priority='").append(annotation.priority()).append("'},");
            }
        }

        return getClass().getSimpleName() + "{events=[" + events.deleteCharAt(events.length()) + "]}";
    }
}
