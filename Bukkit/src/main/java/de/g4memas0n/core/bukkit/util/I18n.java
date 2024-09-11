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

/**
 * A class providing access to translated messages.
 */
@SuppressWarnings("unused")
public final class I18n {

    private static I18n instance;

    private final Plugin plugin;
    private final ClassLoader classLoader;
    private final ResourceBundle defaultBundle;
    private ResourceBundle customBundle;
    private ResourceBundle localBundle;

    /**
     * Constructs a new translation class with the given plugin and bundle basename.
     * @param plugin the plugin to which the class belongs to.
     * @param bundle the basename of the resource bundle.
     */
    public I18n(@NotNull Plugin plugin, @NotNull String bundle) {
        this.plugin = plugin;
        this.classLoader = new CustomFileClassLoader(plugin.getClass().getClassLoader(), plugin.getDataFolder());
        this.defaultBundle = ResourceBundle.getBundle(bundle);
        this.localBundle = this.defaultBundle;
        this.customBundle = null;
    }

    /**
     * Loads the resource bundles for the given locale.
     * @param locale the new bundle locale.
     */
    public void load(@NotNull Locale locale) {
        plugin.getLogger().info("Loading resource bundle for locale " + locale);

        try {
            localBundle = ResourceBundle.getBundle(defaultBundle.getBaseBundleName(), locale);

            if (!localBundle.getLocale().equals(locale)) {
                plugin.getLogger().warning("Could not find resource bundle for locale " + locale
                        + ". Using fallback locale " + localBundle.getLocale());
            }
        } catch (MissingResourceException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to find resource bundle! Using default resource bundle", ex);
        }

        try {
            customBundle = ResourceBundle.getBundle(defaultBundle.getBaseBundleName(), locale,
                    classLoader, Control.getNoFallbackControl(Control.FORMAT_PROPERTIES));
            plugin.getLogger().info("Found custom resource bundle for locale " + locale);
        } catch (MissingResourceException ignored) { }

        plugin.getLogger().info("Locale has been changed. Using locale " + locale());
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
     * @return the current bundle locale.
     */
    public @NotNull Locale locale() {
        return customBundle != null ? customBundle.getLocale() : localBundle.getLocale();
    }

    /**
     * Translates the message for the given key into the currently loaded locale.
     * @param key the key of the message to translate.
     * @return the translated message.
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
            plugin.getLogger().warning("Missing key '" + key + "' in resource bundle for locale " + localBundle.getLocale());
        }

        return defaultBundle.getString(key);
    }

    /**
     * Translates the message for the given key into the currently loaded locale and formats it with the given
     * arguments.
     * @param key the key of the message to translate.
     * @param arguments the arguments to use for formatting.
     * @return the translated and formatted message.
     */
    public @NotNull String format(@NotNull String key, @NotNull Object... arguments) {
        String format = translate(key);

        if (arguments.length > 0) {
            for (int index = 0; index < arguments.length; index++) {
                if (arguments[index] instanceof Enum<?>) {
                    Enum<?> value = (Enum<?>) arguments[index];
                    arguments[index] = value.name().charAt(0) + value.name().substring(1).toLowerCase();
                }
            }

            try {
                return MessageFormat.format(format, arguments);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().log(Level.WARNING, "Illegal message format for key " + key, ex);
                try {
                    return MessageFormat.format(format.replaceAll("\\{(\\D*?)}", "[$1]"), arguments);
                } catch (IllegalArgumentException ignored) { }
            }
        }

        return format;
    }

    /**
     * Checks whether the translation class has a message associated with the given key.
     * @param key the key of the message to check.
     * @return true if it has a message for the key, false otherwise.
     */
    public boolean contains(@NotNull String key) {
        return defaultBundle.containsKey(key);
    }

    /**
     * Translated the message for the given key using the last loaded translation class and format it with the given
     * arguments.
     * @param key key the key of the message to translate.
     * @param arguments the arguments to use for formatting.
     * @return the translated and formatted message.
     * @see #format(String, Object...)
     */
    public static @NotNull String tl(@NotNull String key, @NotNull Object... arguments) {
        Preconditions.checkState(instance != null, "localization unavailable");
        return instance.format(key, arguments);
    }

    /**
     * Checks whether the last loaded translation class has a message associated with the given key.
     * @param key the key of the message to check.
     * @return true if it has a message for the key, false otherwise.
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
