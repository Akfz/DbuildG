package v.akfz.dbg.helper.impl;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Jar;
import v.akfz.dbg.helper.api.Helper;
import v.akfz.dbg.helper.api.LoaderContext;

/**
 * obfuscated end-jar 
 */
public class ObfJarHelper implements Helper {

    @Override public String name() { return "obfJar"; }
    @Override public int order() { return 100; }

    @Override
    public void apply(LoaderContext ctx) {
        Project p = ctx.project();
        String loader = ctx.loader();

        String baseName = ctx.archivesBaseName();
        String version  = ctx.modVersion();
        String fileName = baseName + "-" + version + "-" + loader + ".jar";

        Provider<Directory> libs = p.getLayout().getBuildDirectory().dir("libs");

        if ("fabric".equals(loader)) {
            p.getTasks().named("remapJar").configure(task -> configureArchive(task, fileName, libs));
        } else {
            p.getTasks().named("jar", Jar.class, jar -> {
                jar.getArchiveFileName().set(fileName);
                jar.getDestinationDirectory().set(libs);
                jar.getArchiveClassifier().set("");
            });
        }
    }

    private static void configureArchive(
            org.gradle.api.Task task, String fileName, Provider<Directory> libs) {

        if (task instanceof AbstractArchiveTask aat) {
            aat.getArchiveFileName().set(fileName);
            aat.getDestinationDirectory().set(libs);
            aat.getArchiveClassifier().set("");
            return;
        }

        Class<?> cls = task.getClass();
        try {
            Object afn = cls.getMethod("getArchiveFileName").invoke(task);
            if (afn instanceof RegularFileProperty rfp) {
                rfp.set(libs.map(d -> d.file(fileName)));
            }

            Object dd = cls.getMethod("getDestinationDirectory").invoke(task);
            if (dd instanceof DirectoryProperty dp) {
                dp.set(libs);
            }

            Object ac = cls.getMethod("getArchiveClassifier").invoke(task);
            if (ac instanceof Property<?> prop) {
                @SuppressWarnings({"unchecked"})
                Property<Object> raw = (Property<Object>) prop;
                raw.set("");
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to configure archive task '" + task.getName()
                            + "' (" + cls.getName() + ")", e);
        }
    }
}