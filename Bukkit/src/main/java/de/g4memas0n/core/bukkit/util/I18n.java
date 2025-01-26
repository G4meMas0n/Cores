package de.g4memas0n.core.bukkit.util;

import com.google.common.base.Preconditions;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class providing access to translated messages.
 */
@SuppressWarnings("unused")
public final class I18n {

    private static Logger logger = Logger.getLogger(I18n.class.getName());
    private static I18n instance;

    private final ClassLoader classLoader;
    private final ResourceBundle defaultBundle;
    private ResourceBundle customBundle;
    private ResourceBundle localBundle;

    /**
     * Constructs a new translation class with the given plugin and bundle basename.
     * @param plugin the plugin to which the class belongs to
     * @param bundle the basename of the resource bundle
     */
    public I18n(@NotNull Plugin plugin, @NotNull String bundle) {
        classLoader = new CustomFileClassLoader(plugin.getClass().getClassLoader(), plugin.getDataFolder());
        defaultBundle = ResourceBundle.getBundle(bundle);
        localBundle = defaultBundle;
        customBundle = null;
        logger = plugin.getLogger();
    }

    /**
     * Loads the resource bundles for the given locale.
     * @param locale the new bundle locale
     */
    public void load(@NotNull Locale locale) {
        logger.info("Loading resource bundle for locale " + locale);

        try {
            localBundle = ResourceBundle.getBundle(defaultBundle.getBaseBundleName(), locale);
            if (!localBundle.getLocale().equals(locale)) {
                logger.warning("Could not find resource bundle for locale " + locale
                        + ". Using fallback locale " + localBundle.getLocale());
            }
        } catch (MissingResourceException ex) {
            logger.log(Level.WARNING, "Failed to find resource bundle! Using default resource bundle", ex);
        }

        try {
            customBundle = ResourceBundle.getBundle(defaultBundle.getBaseBundleName(), locale,
                    classLoader, Control.getNoFallbackControl(Control.FORMAT_PROPERTIES));
            logger.info("Found custom resource bundle for locale " + locale);
        } catch (MissingResourceException ignored) { }

        logger.info("Locale has been changed. Using locale " + locale());
        instance = this;
    }

    /**
     * Unloads the resource bundles for the current locale.
     */
    public void unload() {
        ResourceBundle.clearCache(classLoader);

        localBundle = defaultBundle;
        customBundle = null;
        instance = null;
    }

    /**
     * Returns the current locale of the loaded resource bundles.
     * @return the current bundle locale
     */
    public @NotNull Locale locale() {
        return customBundle != null ? customBundle.getLocale() : localBundle.getLocale();
    }

    /**
     * Translates the message for the given key into the currently loaded locale.
     * @param key the key of the message to translate
     * @return the translated message
     */
    public @NotNull String translate(@NotNull String key) {
        if (customBundle != null) {
            try {
                return customBundle.getString(key);
            } catch (MissingResourceException ignored) { }
        }

        try {
            return localBundle.getString(key);
        } catch (MissingResourceException ex) {
            logger.warning("Missing key '" + key + "' in resource bundle for locale " + localBundle.getLocale());
        }

        return defaultBundle.getString(key);
    }

    /**
     * Translates the message for the given key into the currently loaded locale and formats it with the given
     * arguments.
     * @param key the key of the message to translate
     * @param arguments the arguments to use for formatting
     * @return the translated and formatted message
     */
    public @NotNull String format(@NotNull String key, @NotNull Object... arguments) {
        String format = translate(key);

        if (arguments.length > 0) {
            for (int index = 0; index < arguments.length; index++) {
                if (arguments[index] instanceof Enum<?> value) {
                    arguments[index] = value.name().charAt(0) + value.name().substring(1).toLowerCase();
                }
            }

            try {
                return MessageFormat.format(format, arguments);
            } catch (IllegalArgumentException ex) {
                logger.log(Level.WARNING, "Illegal message format for key " + key, ex);
                try {
                    return MessageFormat.format(format.replaceAll("\\{(\\D*?)}", "[$1]"), arguments);
                } catch (IllegalArgumentException ignored) { }
            }
        }

        return format;
    }

    /**
     * Checks whether the translation class has a message associated with the given key.
     * @param key the key of the message to check
     * @return true if it has a message for the key, false otherwise
     */
    public boolean contains(@NotNull String key) {
        return defaultBundle.containsKey(key);
    }

    /**
     * Translated the message for the given key using the last loaded translation class and format it with the given
     * arguments.
     * @param key key the key of the message to translate
     * @param arguments the arguments to use for formatting
     * @return the translated and formatted message
     * @see #format(String, Object...)
     */
    public static @NotNull String tl(@NotNull String key, @NotNull Object... arguments) {
        Preconditions.checkState(instance != null, "localization unavailable");
        return instance.format(key, arguments);
    }

    /**
     * Checks whether the last loaded translation class has a message associated with the given key.
     * @param key the key of the message to check
     * @return true if it has a message for the key, false otherwise
     * @see #contains(String)
     */
    public static boolean has(@NotNull String key) {
        return instance != null && instance.contains(key);
    }

    /**
     * Custom ClassLoader for getting resource bundles located in the plugin data folder.
     */
    private static class CustomFileClassLoader extends ClassLoader {

        private final File directory;

        private CustomFileClassLoader(@NotNull ClassLoader loader, @NotNull File directory) {
            super(loader);
            this.directory = directory;
        }

        @Override
        public @Nullable URL getResource(@NotNull String name) {
            File file = new File(directory, name);
            if (file.exists()) {
                try {
                    return file.toURI().toURL();
                } catch (MalformedURLException ignored) { }
            }

            return null;
        }

        @Override
        public @Nullable InputStream getResourceAsStream(@NotNull String name) {
            File file = new File(directory, name);
            if (file.exists()) {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException ignored) { }
            }

            return null;
        }
    }
}
