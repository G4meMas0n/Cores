package com.github.g4memas0n.cores.bukkit.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class Configuration {

    protected final YamlConfiguration storage;
    protected final String filename;
    protected final Plugin plugin;

    public Configuration(@NotNull final Plugin plugin) {
        this(plugin, "config.yml");
    }

    public Configuration(@NotNull final Plugin plugin, @NotNull final String filename) {
        this.plugin = plugin;
        this.filename = filename;
        this.storage = new YamlConfiguration();
    }

    public void delete() {
        final File config = new File(this.plugin.getDataFolder(), this.filename);

        if (!config.exists()) {
            this.plugin.getLogger().warning("Unable to delete configuration file '" + config.getName() + "': Configuration file not found.");
            return;
        }

        if (config.delete()) {
            this.plugin.getLogger().info("Deleted configuration file '" + config.getName() + "'.");
        }
    }

    public void load() {
        final File config = new File(this.plugin.getDataFolder(), this.filename);
        boolean loaded = false;

        try {
            this.storage.load(config);
            this.plugin.getLogger().info("Loaded configuration file '" + config.getName() + "'.");

            loaded = true;
        } catch (InvalidConfigurationException ex) {
            this.plugin.getLogger().warning("Unable to load configuration file '" + config.getName() + "': Configuration file is broken.");

            final File broken = new File(config.getParent(), config.getName().replaceAll("(?i)(yml)$", "broken.$1"));

            if (broken.exists() && broken.delete()) {
                this.plugin.getLogger().info("Deleted old broken configuration file '" + broken.getName() + "'.");
            }
            if (config.renameTo(broken)) {
                this.plugin.getLogger().info("Renamed broken configuration file '" + config.getName() + "' to '" + broken.getName() + "'.");
            }
        } catch (FileNotFoundException ex) {
            this.plugin.getLogger().warning("Unable to load configuration file '" + config.getName() + "': Configuration file not found.");
        } catch (IOException ex) {
            this.plugin.getLogger().warning("Unable to load configuration file '" + config.getName() + "': " + ex.getMessage());
        }

        if (!loaded) {
            if (!config.exists()) {
                this.plugin.saveResource(this.filename, true);

                if (config.exists()) {
                    this.plugin.getLogger().info("Saved default configuration file '" + config.getName() + "'.");
                }
            }

            try {
                this.storage.load(config);
                this.plugin.getLogger().info("Loaded configuration file '" + config.getName() + "' on second try.");
            } catch (FileNotFoundException ignored) {
            } catch (InvalidConfigurationException | IOException ex) {
                this.plugin.getLogger().severe("Unable to load configuration file '" + config.getName() + "' on second try: " + ex.getMessage());
            }
        }

        this.initialize();
    }

    public abstract void initialize();

    public void save() {
        final File config = new File(this.plugin.getDataFolder(), this.filename);

        try {
            this.storage.save(config);
            this.plugin.getLogger().info("Saved configuration file '" + config.getName() + "'.");
        } catch (IOException ex) {
            this.plugin.getLogger().warning("Unable to save configuration file '" + config.getName() + "': " + ex.getMessage());
        }
    }
}
