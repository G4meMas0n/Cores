package com.github.g4memas0n.cores.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public interface Registrable<T extends JavaPlugin> {

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean register(@NotNull final T plugin);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean unregister();
}
