package v.akfz.dbg.helper.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

public final class HelperRegistry {

    private static final String REGISTRY_RESOURCE = "META-INF/dbuildmf";

    private HelperRegistry() {}

    @SuppressWarnings("null")
    public static List<Helper> forLoader(String loader) {
        List<Helper> out = new ArrayList<>();
        for (Helper h : loadAll()) {
            if (h.loaders().isEmpty() || h.loaders().contains(loader)) {
                out.add(h);
            }
        }
        out.sort(Comparator.comparingInt(Helper::order));
        return out;
    }

    private static List<Helper> loadAll() {
        List<Helper> result = new ArrayList<>();
        ClassLoader cl = HelperRegistry.class.getClassLoader();
        try {
            Enumeration<URL> resources = cl.getResources(REGISTRY_RESOURCE);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (InputStream in = url.openStream();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    int lineNo = 0;
                    while ((line = reader.readLine()) != null) {
                        lineNo++;
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        try {
                            result.add(instantiate(line));
                        } catch (RuntimeException e) {
                            throw new RuntimeException(
                                    "Bad helper entry at " + url + ":" + lineNo + " -> " + line, e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + REGISTRY_RESOURCE, e);
        }
        return result;
    }

    private static Helper instantiate(String fqn) {
        try {
            Class<?> cls = Class.forName(fqn, true, HelperRegistry.class.getClassLoader());
            Object obj = cls.getDeclaredConstructor().newInstance();
            if (!(obj instanceof Helper h)) {
                throw new IllegalStateException(fqn + " does not implement " + Helper.class.getName());
            }
            return h;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate helper: " + fqn, e);
        }
    }
}