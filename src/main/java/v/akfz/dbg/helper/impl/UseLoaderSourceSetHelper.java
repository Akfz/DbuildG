package v.akfz.dbg.helper.impl;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import v.akfz.dbg.DbuildExtension;
import v.akfz.dbg.helper.api.Helper;
import v.akfz.dbg.helper.api.LoaderContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Adds per-loader source dirs:
 *   - src/main/common/{java,resources}      always
 *   - src/main/<loader>/{java,resources}    for loaders configured in build.gradle
 *
 * Replaces default srcDirs — src/main/{java,resources} is NOT included.
 * Add it back yourself if needed.
 *
 * Directories for common + every configured loader are created.
 * Only common + the active loader are attached to the source set.
 */
public class UseLoaderSourceSetHelper implements Helper {

    private static final String COMMON = "common";

    @Override public String name() { return "useLoaderSourceSet"; }
    @Override public Set<String> loaders() { return Set.of(); }
    @Override public int order() { return 60; }

    @Override
    public void apply(LoaderContext ctx) {
        Project p = ctx.project();
        String loader = ctx.loader();

        SourceSetContainer ss = p.getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = ss.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        DbuildExtension ext = p.getExtensions().getByType(DbuildExtension.class);
        Set<String> configured = ext.getConfiguredLoaders();

        List<File> addedJava = new ArrayList<>();
        List<File> addedRes  = new ArrayList<>();

        File commonJava = dir(p, "src/main/common/java");
        File commonRes  = dir(p, "src/main/common/resources");
        mkdirs(p, commonJava);
        mkdirs(p, commonRes);
        main.getJava().srcDir(commonJava);
        main.getResources().srcDir(commonRes);
        addedJava.add(commonJava);
        addedRes.add(commonRes);

        for (String l : configured) {
            mkdirs(p, dir(p, "src/main/" + l + "/java"));
            mkdirs(p, dir(p, "src/main/" + l + "/resources"));
        }

        if (!COMMON.equalsIgnoreCase(loader) && configured.contains(loader)) {
            File loaderJava = dir(p, "src/main/" + loader + "/java");
            File loaderRes  = dir(p, "src/main/" + loader + "/resources");
            main.getJava().srcDir(loaderJava);
            main.getResources().srcDir(loaderRes);
            addedJava.add(loaderJava);
            addedRes.add(loaderRes);
        }

        p.getLogger().lifecycle("[dbuild/{}] useLoaderSourceSet (configured={}):",
                loader, configured);
        addedJava.forEach(f -> p.getLogger().lifecycle("  + java:      {}", rel(p, f)));
        addedRes.forEach(f  -> p.getLogger().lifecycle("  + resources: {}", rel(p, f)));
    }

    private static File dir(Project p, String path) {
        return new File(p.getProjectDir(), path);
    }

    private static void mkdirs(Project p, File dir) {
        if (dir.isDirectory()) return;
        if (dir.mkdirs()) {
            p.getLogger().lifecycle("[dbuild] created dir: {}", dir.getPath());
        } else {
            p.getLogger().warn("[dbuild] could not create dir: {}", dir.getPath());
        }
    }

    private static String rel(Project p, File f) {
        return p.getProjectDir().toPath().relativize(f.toPath()).toString();
    }
}