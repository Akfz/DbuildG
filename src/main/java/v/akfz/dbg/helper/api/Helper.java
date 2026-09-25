package v.akfz.dbg.helper.api;

import java.util.Set;

public interface Helper {

    String name();

    /**
     * Loaders this helper applies to. Empty set = all loaders.
     * Example: the mixin AP helper applies only to Forge/NeoForge.
     */
    default Set<String> loaders() {
        return Set.of();
    }

    /**
     * Lower — earlier. Helpers with {@code order < 50} run before user closures,
     * with {@code order >= 50} — after.
     */
    default int order() {
        return 100;
    }

    /**
     * Called once per loader.
     */
    void apply(LoaderContext ctx);
    }