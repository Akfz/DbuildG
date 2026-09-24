package v.akfz.dbg.helper.impl;

import org.gradle.api.Project;
import v.akfz.dbg.helper.api.Helper;
import v.akfz.dbg.helper.api.LoaderContext;

import java.util.List;

/**
 * Adds in deps dbuild (for easier work, watch : https://github.com/Akfz/DBuild)
 */
public class DbuildDepHelper implements Helper {

    @Override public String name() { return "dbuild"; }
    @Override public int order() { return 10; }

    @Override
    public void apply(LoaderContext ctx) {
        List<String> args = ctx.block().getHelperArgs("dbuild");
        if (args.isEmpty()) throw new IllegalStateException("dbuild(version) requires a version");
        String notation = "v.akfz:DBuild:" + args.get(0);

        Project p = ctx.project();
        p.getDependencies().add("compileOnly", notation);
        p.getDependencies().add("annotationProcessor", notation);
        p.getLogger().lifecycle("[dbuild/{}] + {} (compileOnly, AP)", ctx.loader(), notation);
    }
}