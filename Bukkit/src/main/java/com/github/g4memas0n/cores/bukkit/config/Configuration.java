package com.github.g4memas0n.cores.bukkit.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Configuration extends YamlConfiguration {

    protected final String filename;
    protected final Plugin plugin;

    public Configuration(@NotNull final Plugin plugin) {
        this(plugin, "config.yml");
    }

    public Configuration(@NotNull final Plugin plugin, @NotNull final String filename) {
        this.plugin = plugin;
        this.filename = filename;
    }

    public void delete() {
        final File config = new File(this.plugin.getDataFolder(), this.filename);

        if (!config.exists()) {
            warning("Unable to delete configuration file '" + config.getName() + "': Configuration file not found");
            return;
        }

        if (config.delete()) {
            info("Deleted configuration file '" + config.getName() + "'");
        }
    }

    public void load() {
        final File config = new File(this.plugin.getDataFolder(), this.filename);
        boolean loaded = false;

        try {
            load(config);
            info("Loaded configuration file '" + config.getName() + "'");

            loaded = true;
        } catch (InvalidConfigurationException ex) {
            final File broken = new File(config.getParent(), config.getName().replaceAll("(?i)(yml)$", "broken.$1"));

            warning("Unable to load configuration file '" + config.getName() + "': Configuration file is broken");
            if (broken.exists() && broken.delete()) {
                info("Deleted old broken configuration file '" + broken.getName() + "'");
            }
            if (config.renameTo(broken)) {
                info("Renamed broken configuration file '" + config.getName() + "' to '" + broken.getName() + "'");
            }
        } catch (FileNotFoundException ex) {
            warning("Unable to load configuration file '" + config.getName() + "': Configuration file not found");
        } catch (IOException ex) {
            warning("Unable to load configuration file '" + config.getName() + "': " + ex.getMessage());
        }

        if (!loaded) {
            try {
                if (!config.exists()) {
                    this.plugin.saveResource(this.filename, true);

                    if (config.exists()) {
                        info("Saved default configuration file '" + config.getName() + "'");
                    }
                }

                load(config);
                info("Loaded configuration file '" + config.getName() + "' on second try");
            } catch (FileNotFoundException ignored) {
            } catch (InvalidConfigurationException | IOException ex) {
                warning("Unable to load configuration file '" + config.getName() + "' on second try: " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                warning("Unable to find default configuration file '" + config.getName() + "'");
            }
        }
    }

    public void save() {
        final File config = new File(this.plugin.getDataFolder(), this.filename);

        try {
            save(config);
            info("Saved configuration file '" + config.getName() + "'");
        } catch (IOException ex) {
            warning("Unable to save configuration file '" + config.getName() + "': " + ex.getMessage());
        }
    }

    /*
     * Logging methods for improved code readability:
     */

    protected final void info(@NotNull final String message) {
        this.plugin.getLogger().info(message);
    }

    protected final void warning(@NotNull final String message) {
        this.plugin.getLogger().warning(message);
    }
}
