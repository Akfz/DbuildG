package v.akfz.dbg.helper.impl;

import org.gradle.api.Project;
import org.gradle.language.jvm.tasks.ProcessResources;
import v.akfz.dbg.helper.api.Helper;
import v.akfz.dbg.helper.api.LoaderContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes metadata files belonging to other mod loaders from processResources
 * to prevent files like mods.toml, fabric.mod.json, etc., from ending up in a specific loader's JAR. 
 * Java code separation is handled by useLoaderSourceSet; this helper deals only with resources. 
*/
public class FilterForeignMetadataHelper implements Helper {

    @Override public String name() { return "filterForeignMetadata"; }
    @Override public int order() { return 70; }

    private static final List<String> ALL_FILES = List.of(
            "fabric.mod.json",
            "quilt.mod.json",
            "META-INF/mods.toml",
            "META-INF/neoforge.mods.toml"
    );

    private static final Map<String, Set<String>> OWN = Map.of(
            "fabric",   Set.of("fabric.mod.json"),
            "forge",    Set.of("META-INF/mods.toml"),
            "neoforge", Set.of("META-INF/neoforge.mods.toml"),
            "quilt",    Set.of("quilt.mod.json")
    );

    @Override
    public void apply(LoaderContext ctx) {
        Project p = ctx.project();
        String loader = ctx.loader();

        Set<String> own = OWN.getOrDefault(loader, new HashSet<>(ALL_FILES));

        p.getTasks().named("processResources", ProcessResources.class, task -> {
            for (String f : ALL_FILES) {
                if (!own.contains(f)) {
                    task.exclude(f);
                    p.getLogger().lifecycle("[dbuild/{}] exclude: {}", loader, f);
                }
            }
        });
    }
}