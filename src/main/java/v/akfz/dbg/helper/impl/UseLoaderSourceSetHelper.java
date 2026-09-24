package v.akfz.dbg.helper.impl;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import v.akfz.dbg.helper.api.Helper;
import v.akfz.dbg.helper.api.LoaderContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
* Replaces the default src/main/java and src/main/resources with:
*   - src/main/common/{java,resources}          (always)
*   - src/main/<loader>/{java,resources}        (for the active loader)
*
* The directories are created automatically. For the "common" block, only
* src/main/common/{java,resources} is added, without duplication. 
*/
public class UseLoaderSourceSetHelper implements Helper {

    private static final String COMMON = "common";

    @Override
    public String name() {
        return "useLoaderSourceSet";
    }

    @Override
    public Set<String> loaders() {
        return Set.of();
    }

    @Override
    public int order() { 
        return 60; 
    }

    @Override
    public void apply(LoaderContext ctx) {
        Project p = ctx.project();
        String loader = ctx.loader();

        SourceSetContainer ss = p.getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = ss.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        List<File> javaDirs = new ArrayList<>();
        List<File> resDirs = new ArrayList<>();

        File commonJava = dir(p, "src/main/common/java");
        File commonRes  = dir(p, "src/main/common/resources");
        mkdirs(p, commonJava);
        mkdirs(p, commonRes);
        javaDirs.add(commonJava);
        resDirs.add(commonRes);

        if (!COMMON.equalsIgnoreCase(loader)) {
            File loaderJava = dir(p, "src/main/" + loader + "/java");
            File loaderRes  = dir(p, "src/main/" + loader + "/resources");
            mkdirs(p, loaderJava);
            mkdirs(p, loaderRes);
            javaDirs.add(loaderJava);
            resDirs.add(loaderRes);
        }

        main.getJava().setSrcDirs(javaDirs);
        main.getResources().setSrcDirs(resDirs);

        p.getLogger().lifecycle("[dbuild/{}] useLoaderSourceSet:", loader);
        javaDirs.forEach(f -> p.getLogger().lifecycle("  java:      {}", rel(p, f)));
        resDirs.forEach(f  -> p.getLogger().lifecycle("  resources: {}", rel(p, f)));
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