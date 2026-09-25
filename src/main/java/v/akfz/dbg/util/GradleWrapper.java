package v.akfz.dbg.util;

import org.gradle.api.Project;
import org.gradle.api.tasks.Exec;

import java.util.ArrayList;
import java.util.List;

public final class GradleWrapper {

    private GradleWrapper() {}

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static List<String> commandLine(String... args) {
        List<String> cmd = new ArrayList<>();
        if (isWindows()) {
            cmd.add("cmd");
            cmd.add("/c");
            cmd.add("gradlew.bat");
        } else {
            cmd.add("./gradlew");
        }
        for (String a : args) cmd.add(a);
        return cmd;
    }

    public static String displayName() {
        return isWindows() ? "gradlew.bat" : "./gradlew";
    }

    public static void configure(Project p, Exec t, String command, String loader) {
        t.setGroup("dbuild");
        t.setDescription("Runs `" + displayName() + " " + command
                + " -Pdbuild.loader=" + loader + "`");
        t.setWorkingDir(p.getProjectDir());
        t.setCommandLine(commandLine(command, "-Pdbuild.loader=" + loader));
        t.setStandardOutput(System.out);
        t.setErrorOutput(System.err);
        t.setIgnoreExitValue(false);
    }
}