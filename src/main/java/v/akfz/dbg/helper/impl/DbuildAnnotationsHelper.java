package v.akfz.dbg.helper.impl;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import v.akfz.dbg.helper.api.Helper;
import v.akfz.dbg.helper.api.LoaderContext;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements @DevOnly / @OnlyLoader / @ProdOnly by scanning sources.
 *
 * - @DevOnly       kept in dev jar, excluded from prod jar
 * - @ProdOnly      kept in prod jar, excluded from dev jar
 * - @OnlyLoader    kept only for the listed loaders
 *
 * No runtime deps — parser only, works without DBuild itself.
 * Only the active loader is processed; common is skipped.
 */
public class DbuildAnnotationsHelper implements Helper {
    private static final Pattern P_ENUM_NAME = Pattern.compile("\\b([A-Z_]+)\\b");

    @Override public String name() { return "dbuildannotations"; }
    @Override public int order() { return 65; }

    private static final Pattern P_ONLYLOADER = Pattern.compile(
            "@(?:\\w+\\.)*OnlyLoader\\s*\\(([^)]*)\\)");
    private static final Pattern P_DEVONLY = Pattern.compile(
            "@(?:\\w+\\.)*DevOnly\\b");
    private static final Pattern P_PRODONLY = Pattern.compile(
            "@(?:\\w+\\.)*ProdOnly\\b");

    @Override
    public void apply(LoaderContext ctx) {
        Project p = ctx.project();
        String loader = ctx.loader();
        if ("common".equalsIgnoreCase(loader)) return;

        ScanResult scan = scan(p, loader);

        if (!scan.loaderMismatch.isEmpty()) {
            p.getTasks().withType(JavaCompile.class).configureEach(t -> {
                for (String f : scan.loaderMismatch) t.exclude(f);
            });
        }

        p.getTasks().withType(Jar.class).configureEach(jar -> {
            String n = jar.getName().toLowerCase();
            boolean isDev  = n.contains("dev");
            boolean isProd = !isDev && (n.contains("obf") || n.contains("remap") || n.equals("jar"));

            for (String f : scan.loaderMismatch) jar.exclude(f);
            if (isDev) {
                for (String f : scan.prodOnly) jar.exclude(f);
            } else if (isProd) {
                for (String f : scan.devOnly) jar.exclude(f);
            }
        });

        p.getLogger().lifecycle(
                "[dbuild/{}] dbuildannotations: loaderMismatch={}, devOnly={}, prodOnly={}",
                loader,
                scan.loaderMismatch.size() / 3,
                scan.devOnly.size() / 2,
                scan.prodOnly.size() / 2);
    }

    private record ScanResult(List<String> loaderMismatch,
                              List<String> devOnly,
                              List<String> prodOnly) {}

    private ScanResult scan(Project p, String currentLoader) {
        SourceSetContainer ssc = p.getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = ssc.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        List<String> loaderMismatch = new ArrayList<>();
        List<String> devOnly = new ArrayList<>();
        List<String> prodOnly = new ArrayList<>();

        for (File srcRoot : main.getJava().getSrcDirs()) {
            if (!srcRoot.isDirectory()) continue;
            walk(p, srcRoot, srcRoot, currentLoader, loaderMismatch, devOnly, prodOnly);
        }

        return new ScanResult(loaderMismatch, devOnly, prodOnly);
    }

    private void walk(Project p, File srcRoot, File cur, String currentLoader,
                      List<String> loaderMismatch,
                      List<String> devOnly,
                      List<String> prodOnly) {
        File[] kids = cur.listFiles();
        if (kids == null) return;

        for (File f : kids) {
            if (f.isDirectory()) {
                walk(p, srcRoot, f, currentLoader, loaderMismatch, devOnly, prodOnly);
                continue;
            }
            if (!f.getName().endsWith(".java")) continue;

            String rel = srcRoot.toPath().relativize(f.toPath())
                    .toString().replace('\\', '/');
            String classBase = rel.substring(0, rel.length() - 5);

            String raw;
            try {
                raw = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                p.getLogger().warn("[dbuild] cannot read {}: {}", f, e.getMessage());
                continue;
            }
            String s = stripCommentsAndStrings(raw);

            if (isLoaderMismatch(s, currentLoader)) {
                loaderMismatch.add(rel);
                loaderMismatch.add(classBase + ".class");
                loaderMismatch.add(classBase + "$*.class");
                continue;
            }

            if (P_DEVONLY.matcher(s).find()) {
                devOnly.add(classBase + ".class");
                devOnly.add(classBase + "$*.class");
            }
            if (P_PRODONLY.matcher(s).find()) {
                prodOnly.add(classBase + ".class");
                prodOnly.add(classBase + "$*.class");
            }
        }
    }

    private static boolean isLoaderMismatch(String s, String currentLoader) {
        Matcher m = P_ONLYLOADER.matcher(s);
        String cur = currentLoader.toUpperCase();
        while (m.find()) {
            Matcher em = P_ENUM_NAME.matcher(m.group(1).toUpperCase());
            boolean found = false;
            while (em.find()) {
                String name = em.group(1);
                if (name.equals("LOADER")) continue;
                if (name.equals(cur)) { found = true; break; }
            }
            if (!found) return true;
        }
        return false;
    }

    private static String stripCommentsAndStrings(String s) {
        s = s.replaceAll("(?s)/\\*.*?\\*/", " ");
        s = s.replaceAll("//[^\\n]*", " ");
        s = s.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"");
        s = s.replaceAll("'(?:\\\\.|[^'\\\\])*'", "''");
        return s;
    }
}