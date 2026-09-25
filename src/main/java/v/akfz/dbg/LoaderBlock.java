package v.akfz.dbg;

import groovy.lang.Closure;
import org.gradle.api.Project;
import v.akfz.dbg.helper.api.Helper;
import v.akfz.dbg.helper.api.HelperRegistry;
import v.akfz.dbg.helper.api.LoaderContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoaderBlock {

    private final String name;
    private final List<Closure<?>> closures = new ArrayList<>();
    private final Set<String> enabledHelpers = new LinkedHashSet<>();
    private final Map<String, List<String>> helperArgs = new HashMap<>();

    public LoaderBlock(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public void configure(Closure<?> closure) {
        closures.add(closure);
    }

    public void enableHelper(String helperName) {
        enabledHelpers.add(helperName);
    }

    public Set<String> getEnabledHelpers() { return enabledHelpers; }

    public List<String> getHelperArgs(String helperName) {
        return helperArgs.getOrDefault(helperName, List.of());
    }

    public void addHelperArgs(String helperName, List<String> args) {
        helperArgs.computeIfAbsent(helperName, k -> new ArrayList<>()).addAll(args);
    }

    public void useLoaderSourceSet() { enableHelper("useLoaderSourceSet"); }
    public void filterForeignMetadata() { enableHelper("filterForeignMetadata"); }
    public void devJar() { enableHelper("devJar"); }
    public void obfJar() { enableHelper("obfJar"); }

    public void embed(String... notations) {
        enableHelper("embed");
        addHelperArgs("embed", List.of(notations));
    }

    public void dbuild(String version) {
        enableHelper("dbuild");
        addHelperArgs("dbuild", List.of(version));
    }

    public void registryCommand(String... commands) {
        enableHelper("registryCommand");
        addHelperArgs("registryCommand", List.of(commands));
    }

    public boolean hasConfigureClosures() {
        return !closures.isEmpty();
    }

    public void dbuildannotations() { enableHelper("dbuildannotations"); }
    
    public void applyTo(Project project) {
        LoaderContext ctx = new LoaderContext(project, name, this);
        List<Helper> active = HelperRegistry.forLoader(name).stream()
                .filter(h -> enabledHelpers.contains(h.name()))
                .toList();

        for (Helper h : active) {
            if (h.order() < 50) {
                project.getLogger().lifecycle("[dbuild/{}] helper(pre): {}", name, h.name());
                h.apply(ctx);
            }
        }

        for (Closure<?> c : closures) {
            Closure<?> copy = (Closure<?>) c.clone();
            copy.setDelegate(project);
            copy.setResolveStrategy(Closure.DELEGATE_FIRST);
            copy.call();
        }

        for (Helper h : active) {
            if (h.order() >= 50) {
                project.getLogger().lifecycle("[dbuild/{}] helper(post): {}", name, h.name());
                h.apply(ctx);
            }
        }
    }
}