package v.akfz.dbg.util;

import org.gradle.api.Project;

public final class DbuildProps {

    private DbuildProps() {}

    public static String resolve(String extValue, Project p, String gradleKey, String extKey) {
        if (extValue != null && !extValue.isBlank()) {
            p.getLogger().lifecycle("[dbuild/props] {} = '{}'  (from dbuild extension)",
                    gradleKey, extValue);
            return extValue;
        }

        Object gp = p.findProperty(gradleKey);
        if (gp != null) {
            String s = String.valueOf(gp);
            if (!s.isBlank()) {
                p.getLogger().lifecycle("[dbuild/props] {} = '{}'  (from gradle.properties)",
                        gradleKey, s);
                return s;
            }
        }

        String msg = """

                ╔══════════════════════════════════════════════════════════════╗
                ║  FIXME [dbuild] required property is not set:                ║
                ║      %s
                ║
                ║  Set it in ONE of:
                ║    1) dbuild { %s = '...' }
                ║    2) gradle.properties -> %s=...
                ╚══════════════════════════════════════════════════════════════╝
                """.formatted(gradleKey, extKey, gradleKey);
        throw new IllegalStateException(msg);
    }
}