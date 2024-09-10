package de.g4memas0n.core.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

/**
 * An extended class for the bukkit configuration.
 */
@SuppressWarnings("unused")
public class BaseConfiguration extends YamlConfiguration {

    protected final Plugin plugin;
    protected final File config;

    /**
     * Constructs a new BaseConfiguration for the given plugin and filename.
     * @param plugin the plugin main class.
     * @param file the name of the config file.
     */
    public BaseConfiguration(@NotNull Plugin plugin, @NotNull String file) {
        this(plugin, new File(plugin.getDataFolder(), file));
    }

    /**
     * Constructs a new BaseConfiguration for the given plugin and file.
     * @param plugin the plugin main class.
     * @param config the config file.
     */
    public BaseConfiguration(@NotNull Plugin plugin, @NotNull File config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Deletes the config file of the configuration.
     */
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

    /**
     * Loads the config file of the configuration.
     * <p>
     * If the configuration file does not exist and the plugin jar contains a template, the configuration file will be
     * created with the template.
     */
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

    /**
     * Saves the config file of the configuration.
     */
    public void save() {
        try {
            save(this.config);
        } catch (IOException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to save config file: " + this.config, ex);
        }
    }

    /*
     *
     */

    /**
     * Gets the requested BigDecimal at the given path.
     * <p>
     * If the BigDecimal does not exist but a default value has been specified, this will return the default value. If
     * the BigDecimal does not exist and no default value was specified, this will return null.
     * @param path the path of the BigDecimal to get.
     * @return the requested BigDecimal.
     */
    @Nullable
    public BigDecimal getBigDecimal(@NotNull String path) {
        return getBigDecimal(this, path, null);
    }

    /**
     * Gets the requested BigDecimal at the given path, returning a default value of not found.
     * <p>
     * If the BigDecimal does not exist then the specified default value will be returned regardless of if a default
     * has been identified in the root Configuration.
     * @param path the path of the BigDecimal to get.
     * @param def the default value to return if the path is not found or is not a BigDecimal.
     * @return the requested BigDecimal.
     */
    @Contract("_, !null -> !null")
    @Nullable
    public BigDecimal getBigDecimal(@NotNull String path, @Nullable BigDecimal def) {
        return getBigDecimal(this, path, def);
    }

    /**
     * Gets the requested BigDecimal at the given section and path.
     * <p>
     * If the BigDecimal does not exist but a default value has been specified, this will return the default value. If
     * the BigDecimal does not exist and no default value was specified, this will return null.
     * @param section the section of the BigDecimal to get.
     * @param path the path of the BigDecimal to get.
     * @return the requested BigDecimal.
     */
    @Nullable
    public BigDecimal getBigDecimal(@NotNull ConfigurationSection section, @NotNull String path) {
        return getBigDecimal(section, path, null);
    }

    /**
     * Gets the requested BigDecimal at the given section and path, returning a default value of not found.
     * <p>
     * If the BigDecimal does not exist then the specified default value will be returned regardless of if a default
     * has been identified in the root Configuration.
     * @param section the section of the BigDecimal to get.
     * @param path the path of the BigDecimal to get.
     * @param def the default value to return if the path is not found or is not a BigDecimal.
     * @return the requested BigDecimal.
     */
    @Contract("_, _, !null -> !null")
    @Nullable
    public BigDecimal getBigDecimal(@NotNull ConfigurationSection section, @NotNull String path,
                                    @Nullable BigDecimal def) {
        Object val = def == null ? section.get(path) : section.get(path, null);
        if (val instanceof Number) {
            try {
                return BigDecimal.valueOf(((Number) val).doubleValue());
            } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    /**
     * Gets the requested Locale at the given path.
     * <p>
     * If the Locale does not exist but a default value has been specified, this will return the default value. If
     * the Locale does not exist and no default value was specified, this will return null.
     * @param path the path of the Locale to get.
     * @return the requested Locale.
     */
    @Nullable
    public Locale getLocale(@NotNull String path) {
        return getLocale(this, path, null);
    }

    /**
     * Gets the requested Locale at the given path, returning a default value of not found.
     * <p>
     * If the Locale does not exist then the specified default value will be returned regardless of if a default
     * has been identified in the root Configuration.
     * @param path the path of the Locale to get.
     * @param def the default value to return if the path is not found or is not a Locale.
     * @return the requested Locale.
     */
    @Contract("_, !null -> !null")
    @Nullable
    public Locale getLocale(@NotNull String path, @Nullable Locale def) {
        return getLocale(this, path, def);
    }

    /**
     * Gets the requested Locale at the given section and path.
     * <p>
     * If the Locale does not exist but a default value has been specified, this will return the default value. If
     * the Locale does not exist and no default value was specified, this will return null.
     * @param section the section of the Locale to get.
     * @param path the path of the Locale to get.
     * @return the requested Locale.
     */
    @Nullable
    public Locale getLocale(@NotNull ConfigurationSection section, @NotNull String path) {
        return getLocale(section, path, null);
    }

    /**
     * Gets the requested Locale at the given section and path, returning a default value of not found.
     * <p>
     * If the Locale does not exist then the specified default value will be returned regardless of if a default
     * has been identified in the root Configuration.
     * @param section the section of the Locale to get.
     * @param path the path of the Locale to get.
     * @param def the default value to return if the path is not found or is not a Locale.
     * @return the requested Locale.
     */
    @Contract("_, _, !null -> !null")
    @Nullable
    public Locale getLocale(@NotNull ConfigurationSection section, @NotNull String path,
                            @Nullable Locale def) {
        String val = def == null ? section.getString(path) : section.getString(path, null);
        if (val != null && !val.isEmpty()) {
            String[] parts = val.split("[_-]");
            if (parts.length > 2) {
                return new Locale(parts[0], parts[1], parts[2]);
            } else if (parts.length > 1) {
                return new Locale(parts[0], parts[1]);
            } else if (parts.length > 0) {
                return new Locale(parts[0]);
            }
        }
        return def;
    }

    /**
     * Gets the requested Properties at the given path.
     * <p>
     * If the Properties do not exist but a default value has been specified, this will return the default value. If
     * the Properties do not exist and no default value was specified, this will return null.
     * @param path the path of the Properties to get.
     * @return the requested Properties.
     */
    @Nullable
    public Properties getProperties(@NotNull String path) {
        return getProperties(this, path, null);
    }

    /**
     * Gets the requested Properties at the given path, returning a default value of not found.
     * <p>
     * If the Properties do not exist then the specified default value will be returned regardless of if a default
     * has been identified in the root Configuration.
     * @param path the path of the Properties to get.
     * @param def the default value to return if the path is not found or contains no Properties.
     * @return the requested Properties.
     */
    @Contract("_, !null -> !null")
    @Nullable
    public Properties getProperties(@NotNull String path, @Nullable Properties def) {
        return getProperties(this, path, def);
    }

    /**
     * Gets the requested Properties at the given section and path.
     * <p>
     * If the Properties do not exist but a default value has been specified, this will return the default value. If
     * the Properties do not exist and no default value was specified, this will return null.
     * @param section the section of the Properties to get.
     * @param path the path of the Properties to get.
     * @return the requested Properties.
     */
    @Nullable
    public Properties getProperties(@NotNull ConfigurationSection section, @NotNull String path) {
        return getProperties(section, path, null);
    }

    /**
     * Gets the requested Properties at the given section and path, returning a default value of not found.
     * <p>
     * If the Properties do not exist then the specified default value will be returned regardless of if a default
     * has been identified in the root Configuration.
     * @param section the section of the Properties to get.
     * @param path the path of the Properties to get.
     * @param def the default value to return if the path is not found or contains no Properties.
     * @return the requested Properties.
     */
    @Contract("_, _, !null -> !null")
    @Nullable
    public Properties getProperties(@NotNull ConfigurationSection section, @NotNull String path,
                                    @Nullable Properties def) {
        if (section.contains(path, def == null) && section.isConfigurationSection(path)) {
            ConfigurationSection props = section.getConfigurationSection(path);
            Map<String, Object> values = props != null ? props.getValues(true) : null;
            if (values != null && !values.isEmpty()) {
                Properties properties = new Properties();
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue().toString());
                }
                return properties;
            }
        }
        return def;
    }

    /**
     * Gets the requested Enum object at the given path.
     * <p>
     * If the Object does not exist but a default value has been specified, this will return the default value. If
     * the Object does not exist and no default value was specified, this will return null.
     * @param path the path of the Object to get.
     * @param clazz the type of {@link java.lang.Enum}.
     * @return the requested Enum object.
     * @param <E> the type of {@link java.lang.Enum}.
     */
    @Nullable
    public <E extends Enum<E>> E getEnum(@NotNull String path, @NotNull Class<E> clazz) {
        return getEnum(this, path, clazz, null);
    }

    /**
     * Gets the requested Enum object at the given path, returning a default value if not found.
     * <p>
     * If the Object does not exist then the specified default value will be returned regardless of if a default
     * has been identified in the root Configuration.
     * @param path the path of the Object to get.
     * @param clazz the type of {@link java.lang.Enum}.
     * @param def  the default object to return if the object is not present at the path.
     * @return the requested Enum object.
     * @param <E> the type of {@link java.lang.Enum}.
     */
    @Contract("_, _, !null -> !null")
    @Nullable
    public <E extends Enum<E>> E getEnum(@NotNull String path, @NotNull Class<E> clazz, @Nullable E def) {
        return getEnum(this, path, clazz, def);
    }

    /**
     * Gets the requested Enum object at the given section and path.
     * <p>
     * If the Object does not exist but a default value has been specified, this will return the default value. If
     * the Object does not exist and no default value was specified, this will return null.
     * @param section the section of the Object to get.
     * @param path the path of the Object to get.
     * @param clazz the type of {@link java.lang.Enum}.
     * @return the requested Enum object.
     * @param <E> the type of {@link java.lang.Enum}.
     */
    @Nullable
    public <E extends Enum<E>> E getEnum(@NotNull ConfigurationSection section, @NotNull String path,
                                         @NotNull Class<E> clazz) {
        return getEnum(section, path, clazz, null);
    }

    /**
     * Gets the requested Enum object at the given section and path, returning a default value if not found.
     * <p>
     * If the Object does not exist then the specified default value will be returned regardless of if a default
     * has been identified in the root Configuration.
     * @param section the section of the Object to get.
     * @param path the path of the Object to get.
     * @param clazz the type of {@link java.lang.Enum}.
     * @param def  the default object to return if the object is not present at the path.
     * @return the requested Enum object.
     * @param <E> the type of {@link java.lang.Enum}.
     */
    @Contract("_, _, _, !null -> !null")
    @Nullable
    public <E extends Enum<E>> E getEnum(@NotNull ConfigurationSection section, @NotNull String path,
                                         @NotNull Class<E> clazz, @Nullable E def) {
        String name = def == null ? section.getString(path) : section.getString(path, null);
        if (name != null && !name.isEmpty()) {
            try {
                return Enum.valueOf(clazz, name);
            } catch (IllegalArgumentException ignored) { }
        }
        return def;
    }
}
