package com.github.g4memas0n.cores.bukkit.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.logging.Level;

public class Configuration extends YamlConfiguration {

    protected final Plugin plugin;
    protected final File config;

    public Configuration(@NotNull final Plugin plugin, @NotNull final String file) {
        this(plugin, new File(plugin.getDataFolder(), file));
    }

    public Configuration(@NotNull final Plugin plugin, @NotNull final File config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void delete() {
        if (!this.config.exists()) {
            this.plugin.getLogger().log(Level.WARNING, "Config file " + this.config + " could not be found.");
            return;
        }

        try {
            Files.delete(this.config.toPath());
        } catch (IOException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to delete config file: " + this.config, ex);
        }
    }

    public void load() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(this.config.getPath())) {
            if (stream != null) {
                if (!this.config.exists()) {
                    this.plugin.getLogger().info("Creating config file from template: " + this.config);
                    Files.copy(stream, this.config.toPath());
                }

                setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(stream)));
            }
        } catch (IOException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to read/write config file: " + this.config, ex);
        }

        try {
            load(this.config);
        } catch (InvalidConfigurationException ex) {
            File broken = new File(this.config.getParentFile(), this.config.getName().replaceAll("(?i)(yml)$", "broken.$1"));

            if (!broken.exists() || broken.delete()) {
                if (this.config.renameTo(broken)) {
                    this.plugin.getLogger().log(Level.SEVERE, "Config file " + this.config + " is broken, it has been renamed to " + broken, ex.getCause());
                    return;
                }
            }

            this.plugin.getLogger().log(Level.SEVERE, "Config file " + this.config + " is broken.", ex.getCause());
        } catch (FileNotFoundException ex) {
            this.plugin.getLogger().log(Level.WARNING, "Config file " + this.config + " could not be found.");
        } catch (IOException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to load config file: " + this.config, ex);
        }
    }

    public void save() {
        try {
            save(this.config);
        } catch (IOException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to save config file: " + this.config, ex);
        }
    }
}
