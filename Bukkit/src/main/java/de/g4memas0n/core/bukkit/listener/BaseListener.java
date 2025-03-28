package de.g4memas0n.core.bukkit.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Method;

/**
 * An abstract listener class to extend for listening to bukkit events.
 * @param <P> the main class of the plugin
 */
@SuppressWarnings("unused")
public abstract class BaseListener<P extends JavaPlugin> implements Listener {

    /**
     * The reference to the plugin main class instance.
     */
    protected P plugin;

    /**
     * Registers the implementing listener to bukkit.
     * @param plugin the instance to the plugin main class.
     * @return true if it has been registered, false otherwise.
     */
    public boolean register(@NotNull P plugin) {
        if (this.plugin == null) {
            this.plugin = plugin;
            this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
            return true;
        }

        return false;
    }

    /**
     * Unregisters the implementing listener from bukkit.
     * @param plugin the instance to the plugin main class.
     * @return true if it has been unregistered, false otherwise.
     */
    public boolean unregister(@NotNull P plugin) {
        if (this.plugin == plugin) {
            HandlerList.unregisterAll(this);

            this.plugin = null;
            return true;
        }

        return false;
    }

    @Override
    public @NotNull String toString() {
        StringBuilder events = new StringBuilder();
        EventHandler annotation;

        for (Method method : getClass().getMethods()) {
            annotation = method.getAnnotation(EventHandler.class);
            if (annotation != null && method.getParameterCount() == 1) {
                events.append("{class='").append(method.getParameterTypes()[0].getSimpleName()).append("', ");
                events.append("priority='").append(annotation.priority()).append("'},");
            }
        }

        return getClass().getSimpleName() + "{events=[" + events.deleteCharAt(events.length()) + "]}";
    }
}
