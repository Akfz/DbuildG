package v.akfz.dbg.helper.impl;

import java.util.HashSet;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.tasks.bundling.Jar;
import v.akfz.dbg.helper.api.Helper;
import v.akfz.dbg.helper.api.LoaderContext;

/**
 * Adds dependencies to the `embed` configuration — they get bundled inside the final jar.
 *
 * Creates the `embed` configuration on first use and wires it into `implementation`.
 *
 * Usage:
 *   dbuild { embed 'com.google.code.gson:gson:2.11.0' }
 */
public class EmbedHelper implements Helper {

    @Override public String name() { return "embed"; }
    @Override public int order() { return 10; }

    @Override
    public void apply(LoaderContext ctx) {
        Project p = ctx.project();

        ConfigurationContainer confs = p.getConfigurations();
        Configuration embed = confs.findByName("embed");
        if (embed == null) {
            embed = confs.create("embed");
            Configuration embedFinal = embed;
            confs.named("implementation", impl -> impl.extendsFrom(embedFinal));
            Set<Jar> already = new HashSet<>();
            p.getTasks().withType(Jar.class).configureEach(jar -> {
                if (already.add(jar)) jar.from(embedFinal);
            });
        }

        for (String notation : ctx.block().getHelperArgs("embed")) {
            p.getDependencies().add("embed", notation);
        }
    }
}