package com.github.g4memas0n.cores.bukkit.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public abstract class Config {

    protected final YamlConfiguration storage;
    protected final String filename;
    protected final Plugin plugin;

    private boolean readonly;
    private boolean loaded;

    public Config(@NotNull final Plugin plugin) {
        this(plugin, "config.yml");
    }

    public Config(@NotNull final Plugin plugin, @NotNull final String filename) {
        this.plugin = plugin;
        this.filename = filename;
        this.storage = new YamlConfiguration();
    }

    public void load() {
        final File config = new File(this.plugin.getDataFolder(), this.filename);

        try {
            this.storage.load(config);
            this.plugin.getLogger().info("Loaded configuration file '" + config.getName() + "'.");
            this.loaded = true;
        } catch (FileNotFoundException ex) {
            this.plugin.getLogger().warning("Unable to load configuration file '" + config.getName() + "': Configuration file not found.");
            this.plugin.saveResource(this.filename, true);

            if (config.exists()) {
                this.plugin.getLogger().info("Saved default configuration file '" + config.getName() + "'.");
            }
        } catch (InvalidConfigurationException ex) {
            this.plugin.getLogger().warning("Unable to load configuration file '" + config.getName() + "': Configuration file is broken.");

            final File broken = new File(config.getParent(), config.getName().replaceAll("(?i)(yml)$", "broken.$1"));

            if (broken.exists() && broken.delete()) {
                this.plugin.getLogger().info("Deleted old broken configuration file '" + broken.getName() + "'.");
            }
            if (config.renameTo(broken)) {
                this.plugin.getLogger().info("Renamed broken configuration file '" + config.getName() + "' to '" + broken.getName() + "'.");
            }

            this.plugin.saveResource(this.filename, true);

            if (config.exists()) {
                this.plugin.getLogger().info("Saved default configuration file '" + config.getName() + "'.");
            }
        } catch (IOException ex) {
            this.plugin.getLogger().warning("Unable to load configuration file '" + config.getName() + "': " + ex.getMessage());
        }

        if (!this.loaded) {
            try {
                this.storage.load(config);
                this.plugin.getLogger().info("Loaded configuration file '" + config.getName() + "'.");
                this.loaded = true;
            } catch (InvalidConfigurationException | IOException ex) {
                final InputStream resource = this.plugin.getResource(this.filename);

                if (resource == null) {
                    this.plugin.getLogger().severe("Unable to find default configuration file '" + this.filename + "'.");
                    return;
                }

                try {
                    this.storage.load(new InputStreamReader(resource));
                    this.plugin.getLogger().info("Loaded default configuration file '" + this.filename + "' in read-only mode.");
                    this.readonly = true;
                } catch (InvalidConfigurationException | IOException ignored) {
                    this.plugin.getLogger().severe("Unable to load default configuration file '" + this.filename + "'.");
                    return;
                }
            }
        }

        this.initialize();
    }

    public abstract void initialize();

    public void save() {
        if (!this.loaded || this.readonly) {
            this.plugin.getLogger().warning("Unable to save configuration file '" + this.filename + "': Saving is not possible.");
            return;
        }

        final File config = new File(this.plugin.getDataFolder(), this.filename);

        try {
            this.storage.save(config);
            this.plugin.getLogger().info("Saved configuration file '" + config.getName() + "'.");
        } catch (IOException ex) {
            this.plugin.getLogger().warning("Unable to save configuration file '" + config.getName() + "': " + ex.getMessage());
        }
    }

    public <E extends Enum<E>> @NotNull List<E> getEnumList(@NotNull final String path, @NotNull final Class<E> clazz) {
        final List<E> list = new ArrayList<>();

        for (final String name : this.storage.getStringList(path)) {
            try {
                list.add(Enum.valueOf(clazz, name));
            } catch (IllegalArgumentException ignored) {

            }
        }

        return list;
    }

    public <E extends Enum<E>> void setEnumList(@NotNull final String path, @NotNull final List<E> list) {
        this.storage.set(path, list.stream().map(Enum::name).collect(Collectors.toList()));
    }

    public @Nullable Locale getLocale(@NotNull final String path) {
        final String locale = this.storage.getString(path);

        if (locale != null && !locale.isEmpty()) {
            return Locale.forLanguageTag(locale);
        }

        return null;
    }

    public void setLocale(@NotNull final String path, @Nullable final Locale locale) {
        this.storage.set(path, locale != null ? locale.toLanguageTag() : null);
    }
}
