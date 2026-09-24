package v.akfz.dbg.helper.api;

import org.gradle.api.Project;
import v.akfz.dbg.DbuildExtension;
import v.akfz.dbg.LoaderBlock;
import v.akfz.dbg.util.DbuildProps;

public final class LoaderContext {

    private final Project project;
    private final String loader;
    private final LoaderBlock block;

    public LoaderContext(Project project, String loader, LoaderBlock block) {
        this.project = project;
        this.loader = loader;
        this.block = block;
    }

    public Project project() { return project; }
    public String loader() { return loader; }
    public LoaderBlock block() { return block; }

    private DbuildExtension ext() {
        return project.getExtensions().getByType(DbuildExtension.class);
    }

    public String modId() {
        return DbuildProps.resolve(ext().getModId().getOrElse(""), project, "mod_id", "modId");
    }

    public String modVersion() {
        return DbuildProps.resolve(ext().getModVersion().getOrElse(""), project, "mod_version", "modVersion");
    }

    public String archivesBaseName() {
        return DbuildProps.resolve(ext().getArchivesBaseName().getOrElse(""), project, "archives_base_name", "archivesBaseName");
    }
}