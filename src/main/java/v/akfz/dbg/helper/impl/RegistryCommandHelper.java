package v.akfz.dbg.helper.impl;

import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.tasks.GradleBuild;
import v.akfz.dbg.helper.api.Helper;
import v.akfz.dbg.helper.api.LoaderContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers a Gradle wrapper task `<loader><Command>`
 * that executes `./gradlew <command> -Pdbuild.loader=<loader>`
 * in a separate nested Gradle session.
 */
public class RegistryCommandHelper implements Helper {

    @Override public String name() { return "registryCommand"; }
    @Override public int order() { return 100; }

    @Override
    public void apply(LoaderContext ctx) {
        Project p = ctx.project();
        String loader = ctx.loader();

        Map<String, String> props = new HashMap<>(
                p.getGradle().getStartParameter().getProjectProperties());
        props.put("dbuild.loader", loader);

        for (String command : ctx.block().getHelperArgs("registryCommand")) {
            String wrapperName = loader + capitalize(command);
            p.getTasks().register(wrapperName, GradleBuild.class, t -> {
                t.setGroup("dbuild");
                t.setDescription("Runs `./gradlew " + command + " -Pdbuild.loader=" + loader + "`");
                t.setDir(p.getProjectDir());
                t.setTasks(List.of(command));

                StartParameter sp = p.getGradle().getStartParameter().newBuild();
                sp.setCurrentDir(p.getProjectDir());
                sp.setProjectProperties(props);
                t.setStartParameter(sp);
            });
            p.getLogger().lifecycle("[dbuild/{}] registered command: {} -> gradlew {}",
                    loader, wrapperName, command);
        }
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}