package v.akfz.dbg.helper.api;

import java.util.Set;

public interface Helper {

    String name();

    /**
     * Specifies the loaders for which this is applicable. An empty set means all loaders. 
     * For example, the mixin-AP helper is applicable only to Forge/NeoForge. 
    */
    default Set<String> loaders() {
        return Set.of();
    }

    /**
     * lower - earlier
     * @return <50 before closures >= 50 after
     */
    default int order() {
        return 100;
    }

    /**
     * one time per loader
     * @param ctx
     */
    void apply(LoaderContext ctx);
}