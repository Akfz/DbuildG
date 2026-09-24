package v.akfz.dbg.helper.impl;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.tasks.bundling.Jar;
import v.akfz.dbg.helper.api.Helper;
import v.akfz.dbg.helper.api.LoaderContext;

/**
 * EmbedHelper include something in end-jar with one word (with implementation)
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
            p.getTasks().withType(Jar.class).configureEach(jar -> jar.from(embedFinal));
        }

        for (String notation : ctx.block().getHelperArgs("embed")) {
            p.getDependencies().add("embed", notation);
        }
    }
}