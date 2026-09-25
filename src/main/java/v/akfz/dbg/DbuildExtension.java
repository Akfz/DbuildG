package v.akfz.dbg;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class DbuildExtension {

    public abstract Property<String> getDefaultLoader();

    public abstract Property<String> getModId();
    public abstract Property<String> getModVersion();
    public abstract Property<String> getArchivesBaseName();

    private final LoaderBlock common;
    private static final List<String> ALL = List.of("common", "fabric", "forge", "neoforge", "quilt");
    private static final List<String> LOADERS = List.of("fabric", "forge", "neoforge", "quilt");
    private final Map<String, LoaderBlock> blocks = new LinkedHashMap<>();
    private final Map<String, List<String>> globalCommands = new LinkedHashMap<>();

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

    public void devJar() { LOADERS.forEach(n -> block(n).devJar()); }
    public void obfJar() { LOADERS.forEach(n -> block(n).obfJar()); }

    public void embed(String... notations) { ALL.forEach(n -> block(n).embed(notations));}

    public void dbuild(String version) {ALL.forEach(n -> block(n).dbuild(version));}

    public void registryCommand(String command, String... loaders) {
        for (String loader : loaders) {
            if (loader == null || loader.isBlank()) continue;
            globalCommands.computeIfAbsent(loader.toLowerCase(), k -> new ArrayList<>()).add(command);
        }
    }

    public Map<String, List<String>> getGlobalCommands() {
        return globalCommands;
    }

    public void dbuildannotations() { ALL.forEach(n -> block(n).dbuildannotations()); }

    @SuppressWarnings("null")
    public Set<String> getConfiguredLoaders() {
        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, LoaderBlock> e : blocks.entrySet()) {
            @SuppressWarnings("null")
            String name = e.getKey();
            if ("common".equals(name)) continue;
            if (e.getValue().hasConfigureClosures()) result.add(name);
        }
        return result;
    }
    
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