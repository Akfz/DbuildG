package v.akfz.dbg.helper.impl;

import org.gradle.api.Project;
import org.gradle.api.tasks.Exec;
import v.akfz.dbg.helper.api.Helper;
import v.akfz.dbg.helper.api.LoaderContext;
import v.akfz.dbg.util.GradleWrapper;

/**
 * Registers Exec wrapper tasks for the active loader, one per command.
 *
 * Per-loader form (fires only when that loader is active):
 * <pre>
 *   dbuild { fabric { registryCommand 'runClient', 'runServer' } }
 *   → fabricRunClient, fabricRunServer
 * </pre>
 *
 * Each wrapper runs in a fresh OS process:
 * <pre>
 *   ./gradlew &lt;command&gt; -Pdbuild.loader=&lt;loader&gt;
 * </pre>
 *
 * For the global form (all loaders at once), see {@code DbuildExtension.registryCommand()}.
 */
public class RegistryCommandHelper implements Helper {

    @Override public String name() { return "registryCommand"; }
    @Override public int order() { return 100; }

    @Override
    public void apply(LoaderContext ctx) {
        Project p = ctx.project();
        String loader = ctx.loader();

        for (String command : ctx.block().getHelperArgs("registryCommand")) {
            String wrapperName = loader + capitalize(command);

            if (p.getTasks().findByName(wrapperName) != null) {
                p.getLogger().warn("[dbuild] task {} already exists, skipping", wrapperName);
                continue;
            }

            p.getTasks().register(wrapperName, Exec.class,
                    t -> GradleWrapper.configure(p, t, command, loader));

            p.getLogger().lifecycle(
                    "[dbuild] registered command: {} -> {} {} -Pdbuild.loader={}",
                    wrapperName, GradleWrapper.displayName(), command, loader);
        }
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}