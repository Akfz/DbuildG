package v.akfz.dbg;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class DbuildGradlePlugin implements Plugin<Project> {

    public static final String PROPERTY = "dbuild.loader";

    @Override
    public void apply(Project project) {
        DbuildExtension ext = project.getExtensions()
                .create("dbuild", DbuildExtension.class);

        project.afterEvaluate(p -> {
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
}