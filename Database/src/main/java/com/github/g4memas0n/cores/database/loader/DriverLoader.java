package com.github.g4memas0n.cores.database.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public abstract class DriverLoader {

    protected DriverLoader() { }

    public abstract void load(@NotNull final InputStream file) throws IOException;

    public abstract @NotNull List<Driver> get(@NotNull final String type);

    public record Driver(@NotNull String type, int version,
                         @Nullable String driverClass, @Nullable String dataSourceClass,
                         @Nullable String statements, @NotNull String url) implements Comparable<Driver> {

        public @NotNull String url(@NotNull final String database, @NotNull final String host, final int port) {
            return this.url.replace("{DATABASE}", database).replace("{HOST}", host)
                    .replace("{PORT}", Integer.toString(port));
        }

        @Override
        public boolean equals(@Nullable final Object object) {
            if (this == object) {
                return true;
            }

            if (object == null || this.getClass() != object.getClass()) {
                return false;
            }

            final Driver other = (Driver) object;
            return this.type.equals(other.type) && this.version == other.version;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.type, this.version);
        }

        @Override
        public int compareTo(@NotNull final Driver other) {
            if (this.type.equals(other.type)) {
                return Integer.compare(this.version, other.version);
            }

            return this.type.compareTo(other.type);
        }

        @Override
        public @NotNull String toString() {
            return "Driver{type='" + this.type + "', version=" + this.version + ", statements='" + this.statements + "}";
        }
    }
}
