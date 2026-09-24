package v.akfz.dbg;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class DbuildExtension {

    public abstract Property<String> getDefaultLoader();

    public abstract Property<String> getModId();
    public abstract Property<String> getModVersion();
    public abstract Property<String> getArchivesBaseName();

    private final LoaderBlock common;
    private static final List<String> ALL = List.of("common", "fabric", "forge", "neoforge", "quilt");
    private final Map<String, LoaderBlock> blocks = new LinkedHashMap<>();

    @Inject
    public DbuildExtension(ObjectFactory objects) {
        this.common = new LoaderBlock("common");
        this.blocks.put("common", this.common);  
        getDefaultLoader().convention("");
        getModId().convention("");           
        getModVersion().convention("");       
        getArchivesBaseName().convention(""); 
    }

    public void useLoaderSourceSet() { ALL.forEach(n -> block(n).useLoaderSourceSet()); }
    public void filterForeignMetadata() { ALL.forEach(n -> block(n).filterForeignMetadata()); }
    public void devJar() { ALL.forEach(n -> block(n).devJar()); }
    public void obfJar() { ALL.forEach(n -> block(n).obfJar()); }

    public void embed(String... notations) {
        ALL.forEach(n -> block(n).embed(notations));
    }

    public void dbuild(String version) {
        ALL.forEach(n -> block(n).dbuild(version));
    }

    public void dbuildannotations() { ALL.forEach(n -> block(n).dbuildannotations()); }
    
    public LoaderBlock getCommon() { return common; }
    public void common(Action<? super LoaderBlock> action) { action.execute(common); }

    public LoaderBlock getFabric()   { return block("fabric"); }
    public LoaderBlock getForge()    { return block("forge"); }
    public LoaderBlock getNeoForge() { return block("neoforge"); }
    public LoaderBlock getQuilt()    { return block("quilt"); }

    public void fabric(Action<? super LoaderBlock> action)   { action.execute(getFabric()); }
    public void forge(Action<? super LoaderBlock> action)    { action.execute(getForge()); }
    public void neoforge(Action<? super LoaderBlock> action) { action.execute(getNeoForge()); }
    public void quilt(Action<? super LoaderBlock> action)    { action.execute(getQuilt()); }

    public LoaderBlock block(String name) {
        return blocks.computeIfAbsent(name, LoaderBlock::new);
    }
}