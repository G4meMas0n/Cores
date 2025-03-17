package de.g4memas0n.core.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An extended class for the bukkit yaml configuration.
 */
@SuppressWarnings("unused")
public class YamlConfig extends YamlConfiguration {

    /**
     * Logger instance used by the configs.
     */
    public static Logger logger = Logger.getLogger(YamlConfig.class.getName());
    protected final String templatePath;

    /**
     * Construct a new extended yaml configuration.
     */
    public YamlConfig() {
        this.templatePath = null;
    }

    /**
     * Construct a new extended yaml configuration with the given template path.
     * @param template the path of the config template file
     */
    public YamlConfig(@NotNull String template) {
        this.templatePath = template;
    }

    /*
     *
     */

    /**
     * Deletes the yaml configuration file at the given path.
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException if an I/O error occurs
     */
    public void delete(@NotNull Path path) throws IOException {
        try {
            Files.delete(path);
        } catch (NoSuchFileException ex) {
            logger.warning("Could not find config file " + path.getFileName());
            throw new FileNotFoundException(ex.getMessage());
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to delete config file " + path.getFileName(), ex);
            throw ex;
        }
    }

    /**
     * Loads the file of the yaml configuration.
     * <p>
     * If the file does not exist and the plugin jar contains a template file, a new file will be created based on the
     * found template.
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException if an I/O error occurs
     */
    public void load(@NotNull Path path) throws IOException {
        String template = templatePath == null ? path.getFileName().toString() : templatePath;
        try (InputStream stream = YamlConfig.class.getClassLoader().getResourceAsStream(template)) {
            if (stream != null) {
                if (Files.notExists(path)) {
                    logger.info("Saving default config from template file " + template);
                    Files.createDirectories(path.getParent());
                    Files.copy(stream, path);
                }
                setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(stream)));
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to read/write template file " + template, ex);
        }

        try {
            load(path.toFile());
        } catch (InvalidConfigurationException ex) {
            String brokenName = path.getFileName().toString().replaceAll("(?i)(yml)$", "broken.$1");
            try {
                Files.move(path, path.resolveSibling(brokenName), StandardCopyOption.REPLACE_EXISTING);
                logger.log(Level.WARNING, "Config file " + path.getFileName() + " is broken, renaming it to " + brokenName, ex);
            } catch (IOException ignored) {
                logger.log(Level.WARNING, "Config file " + path.getFileName() + " is broken and need to be fixed", ex);
            }
            throw new IOException("Invalid configuration", ex);
        } catch (FileNotFoundException ex) {
            logger.warning("Could not find config file " + path.getFileName());
            throw ex;
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to load config file " + path.getFileName(), ex);
            throw ex;
        }
    }

    /**
     * Saves the file of the yaml configuration.
     * @throws IOException if an I/O error occurs
     */
    public void save(@NotNull Path path) throws IOException {
        try {
            save(path.toFile());
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to save config file " + path.getFileName(), ex);
            throw ex;
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
     * @param path the path of the BigDecimal to get
     * @return the requested BigDecimal
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
     * @param path the path of the BigDecimal to get
     * @param def the default value to return if the path is not found or is not a BigDecimal
     * @return the requested BigDecimal
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
     * @param section the section of the BigDecimal to get
     * @param path the path of the BigDecimal to get
     * @return the requested BigDecimal
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
     * @param section the section of the BigDecimal to get
     * @param path the path of the BigDecimal to get
     * @param def the default value to return if the path is not found or is not a BigDecimal
     * @return the requested BigDecimal
     */
    @Contract("_, _, !null -> !null")
    @Nullable
    public BigDecimal getBigDecimal(@NotNull ConfigurationSection section, @NotNull String path,
                                    @Nullable BigDecimal def) {
        Object val = def == null ? section.get(path) : section.get(path, null);
        try {
            if (val instanceof Long) {
                return BigDecimal.valueOf((Long) val);
            } else if (val instanceof Number) {
                return BigDecimal.valueOf(((Number) val).doubleValue());
            } else if (val instanceof String) {
                return new BigDecimal((String) val);
            }
        } catch (NumberFormatException ignored) { }
        return def;
    }

    /**
     * Gets the requested Locale at the given path.
     * <p>
     * If the Locale does not exist but a default value has been specified, this will return the default value. If
     * the Locale does not exist and no default value was specified, this will return null.
     * @param path the path of the Locale to get
     * @return the requested Locale
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
     * @param path the path of the Locale to get
     * @param def the default value to return if the path is not found or is not a Locale
     * @return the requested Locale
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
     * @param section the section of the Locale to get
     * @param path the path of the Locale to get
     * @return the requested Locale
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
     * @param section the section of the Locale to get
     * @param path the path of the Locale to get
     * @param def the default value to return if the path is not found or is not a Locale
     * @return the requested Locale
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
     * @param path the path of the Properties to get
     * @return the requested Properties
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
     * @param path the path of the Properties to get
     * @param def the default value to return if the path is not found or contains no Properties
     * @return the requested Properties
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
     * @param section the section of the Properties to get
     * @param path the path of the Properties to get
     * @return the requested Properties
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
     * @param section the section of the Properties to get
     * @param path the path of the Properties to get
     * @param def the default value to return if the path is not found or contains no Properties
     * @return the requested Properties
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
     * @param path the path of the Object to get
     * @param clazz the type of {@link java.lang.Enum}
     * @return the requested Enum object
     * @param <E> the type of {@link java.lang.Enum}
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
     * @param path the path of the Object to get
     * @param clazz the type of {@link java.lang.Enum}
     * @param def  the default object to return if the object is not present at the path
     * @return the requested Enum object
     * @param <E> the type of {@link java.lang.Enum}
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
     * @param section the section of the Object to get
     * @param path the path of the Object to get
     * @param clazz the type of {@link java.lang.Enum}
     * @return the requested Enum object
     * @param <E> the type of {@link java.lang.Enum}
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
     * @param section the section of the Object to get
     * @param path the path of the Object to get
     * @param clazz the type of {@link java.lang.Enum}
     * @param def  the default object to return if the object is not present at the path
     * @return the requested Enum object
     * @param <E> the type of {@link java.lang.Enum}
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
