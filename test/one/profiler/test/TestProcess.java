package one.profiler.test;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TestProcess implements AutoCloseable {
    private static final MethodHandle pid = getPidHandle();

    private static MethodHandle getPidHandle() {
        // JDK 9+
        try {
            return MethodHandles.publicLookup().findVirtual(Process.class, "pid", MethodType.methodType(long.class));
        } catch (ReflectiveOperationException e) {
            // fallback
        }

        // JDK 8
        try {
            Field f = Class.forName("java.lang.UNIXProcess").getDeclaredField("pid");
            f.setAccessible(true);
            return MethodHandles.lookup().unreflectGetter(f).asType(MethodType.methodType(long.class, Process.class));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unsupported API", e);
        }
    }

    private final Process p;

    public TestProcess(Process p) {
        this.p = p;
    }

    private static void addArgs(List<String> cmd, String args) {
        if (args != null && !args.isEmpty()) {
            Collections.addAll(cmd, args.split(" "));
        }
    }

    public static TestProcess runJava(Class<?> mainClass, String jvmArgs) throws IOException, InterruptedException {
        return runJava(mainClass.getName(), jvmArgs);
    }

    public static TestProcess runJava(String mainClass, String jvmArgs) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + "/bin/java");
        cmd.add("-cp");
        cmd.add("out/production/async-profiler");
        addArgs(cmd, jvmArgs);
        cmd.add(mainClass);
        System.out.println(cmd);

        Process p = new ProcessBuilder(cmd)
                .inheritIO()
                .start();

        Thread.sleep(100);
        return new TestProcess(p);
    }

    @Override
    public void close() throws TimeoutException, InterruptedException {
        p.destroy();
        if (!p.waitFor(5, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new TimeoutException("Child process has not exited");
        }
    }

    public long pid() {
        try {
            return (long) pid.invokeExact((Process) p);
        } catch (Throwable e) {
            throw new IllegalStateException("Unsupported API", e);
        }
    }

    public File profile(String args) throws IOException, TimeoutException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("/bin/sh");
        cmd.add("profiler.sh");
        addArgs(cmd, args);
        cmd.add(Long.toString(pid()));
        System.out.println(cmd);

        File out = File.createTempFile("ap-out", ".tmp");

        Process p = new ProcessBuilder(cmd)
                .inheritIO()
                .redirectOutput(out)
                .start();

        if (!p.waitFor(30, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new TimeoutException("Profile process has not exited");
        }

        return out;
    }

    public static void main(String[] args) throws Exception {
        try (TestProcess p = runJava(test.smoke.Cpu.class, "")) {
            File out = p.profile("-d 5 -e cpu -o collapsed");
            System.out.println(out);
        }
    }
}
