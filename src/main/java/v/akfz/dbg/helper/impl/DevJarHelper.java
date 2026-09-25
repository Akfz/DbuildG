package v.akfz.dbg.helper.impl;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.bundling.Jar;
import v.akfz.dbg.helper.api.Helper;
import v.akfz.dbg.helper.api.LoaderContext;

import java.util.Date;
import java.util.Map;

/**
 * Registers the dev jar task for the active loader.
 *
 * <ul>
 *   <li>fabric:    renames default {@code jar} → {@code <base>-<version>-fabric-dev.jar}</li>
 *   <li>forge/…:   registers {@code <loader>DevJar} → {@code <base>-<version>-<loader>-dev.jar}</li>
 * </ul>
 *
 * Dev jars are un-obfuscated. Combine with {@code obfJar()} to also get the prod jar.
 */
public class DevJarHelper implements Helper {

    @Override public String name() { return "devJar"; }
    @Override public int order() { return 100; }

    @Override
    public void apply(LoaderContext ctx) {
        Project p = ctx.project();
        String loader = ctx.loader();
        String baseName = ctx.archivesBaseName();
        String version  = ctx.modVersion();
        String modId    = ctx.modId();

        String fileName = baseName + "-" + version + "-" + loader + "-dev.jar";

        if ("fabric".equals(loader)) {
            p.getTasks().named("jar", Jar.class, jar -> {
                jar.getArchiveFileName().set(fileName);
                jar.getDestinationDirectory().set(p.getLayout().getBuildDirectory().dir("libs"));
                jar.getManifest().attributes(baseManifest(loader, baseName, version, modId));
            });
        } else {
            String taskName = loader + "DevJar";
            p.getTasks().register(taskName, Jar.class, jar -> {
                jar.getArchiveFileName().set(fileName);
                jar.getDestinationDirectory().set(p.getLayout().getBuildDirectory().dir("libs"));
                jar.setDuplicatesStrategy(org.gradle.api.file.DuplicatesStrategy.WARN);
                jar.from(p.getExtensions().getByType(org.gradle.api.tasks.SourceSetContainer.class)
                        .getByName("main").getOutput());
                Configuration embed = p.getConfigurations().findByName("embed");
                if (embed != null) jar.from(embed);
                jar.getManifest().attributes(baseManifest(loader, baseName, version, modId));
            });

            p.getTasks().named("build").configure(t -> t.dependsOn(taskName));
        }
    }

    private Map<String, Object> baseManifest(String loader, String base, String ver, String modId) {
        return Map.of(
                "Specification-Title",  modId,
                "Specification-Vendor", String.valueOf(""),
                "Specification-Version", "1",
                "Implementation-Title",  base,
                "Implementation-Version", ver,
                "Implementation-Vendor", String.valueOf(""),
                "Implementation-Timestamp", new Date().toString()
        );
    }
}