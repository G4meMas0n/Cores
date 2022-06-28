package com.github.g4memas0n.cores.bukkit.listener;

import com.github.g4memas0n.cores.bukkit.Registrable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class BasicListener<T extends JavaPlugin> implements Listener, Registrable<T> {

    protected T plugin;

    @Override
    public boolean register(@NotNull final T plugin) {
        if (this.plugin == null) {
            this.plugin = plugin;
            this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
            return true;
        }

        return false;
    }

    @Override
    public boolean unregister() {
        if (this.plugin != null) {
            HandlerList.unregisterAll(this);

            this.plugin = null;
            return true;
        }

        return false;
    }

    @Override
    public @NotNull String toString() {
        final List<String> events = new ArrayList<>();

        for (final Method method : this.getClass().getMethods()) {
            if (method.getAnnotation(EventHandler.class) == null) {
                continue;
            }

            if (method.getParameterCount() == 1) {
                final Class<?> type = method.getParameterTypes()[0];

                if (Event.class.isAssignableFrom(type)) {
                    events.add(type.getSimpleName());
                }
            }
        }

        return this.getClass().getSimpleName() + "{events=[" + String.join(",", events) + "]}";
    }
}
