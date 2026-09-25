package v.akfz.dbg;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Exec;

import v.akfz.dbg.util.GradleWrapper;

import java.util.List;
import java.util.Map;

public class DbuildGradlePlugin implements Plugin<Project> {

    public static final String PROPERTY = "dbuild.loader";

    @Override
    public void apply(Project project) {
        DbuildExtension ext = project.getExtensions()
                .create("dbuild", DbuildExtension.class);

        project.afterEvaluate(p -> {
            registerGlobalCommands(p, ext);

            String active = (String) p.findProperty(PROPERTY);
            if (active == null || active.isBlank()) {
                active = ext.getDefaultLoader().getOrElse("");
            }
            String loaderName = (active == null || active.isBlank())
                    ? null
                    : active.toLowerCase();

            p.getLogger().lifecycle("[dbuild] config: defaultLoader='{}', active='{}'",
                    ext.getDefaultLoader().getOrElse("(none)"),
                    loaderName == null ? "(none)" : loaderName);

            p.getLogger().lifecycle("[dbuild] applying block: common");
            ext.getCommon().applyTo(p);

            if (loaderName == null) {
                p.getLogger().lifecycle(
                        "[dbuild] no loader selected (no -P{} and no defaultLoader)", PROPERTY);
                return;
            }

            p.getLogger().lifecycle("[dbuild] applying block: {}", loaderName);
            ext.block(loaderName).applyTo(p);
        });
    }

    private void registerGlobalCommands(Project p, DbuildExtension ext) {
        for (Map.Entry<String, List<String>> e : ext.getGlobalCommands().entrySet()) {
            String loader = e.getKey();
            for (String command : e.getValue()) {
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
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}