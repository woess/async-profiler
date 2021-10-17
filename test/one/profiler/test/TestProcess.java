/*
 * Copyright 2021 Andrei Pangin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package one.profiler.test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestProcess implements AutoCloseable {
    private static final Logger log = Logger.getLogger(TestProcess.class.getName());

    private static final Pattern filePattern = Pattern.compile("%[a-z]+");

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
    private final Map<String, File> tmpFiles = new HashMap<>();
    private int timeout = 30;

    public TestProcess(String mainClass, String jvmArgs) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + "/bin/java");
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        addArgs(cmd, jvmArgs);
        cmd.add(mainClass);
        log.info("Running " + cmd);

        this.p = new ProcessBuilder(cmd)
                .inheritIO()
                .start();
    }

    private void addArgs(List<String> cmd, String args) {
        if (args != null && !args.isEmpty()) {
            args = substituteFiles(args);
            for (StringTokenizer st = new StringTokenizer(args, " "); st.hasMoreTokens(); ) {
                cmd.add(st.nextToken());
            }
        }
    }

    private String substituteFiles(String s) {
        Matcher m = filePattern.matcher(s);
        if (!m.find()) {
            return s;
        }

        StringBuffer sb = new StringBuffer();
        do {
            File f = createTempFile(m.group());
            m.appendReplacement(sb, f.toString());
        } while (m.find());

        m.appendTail(sb);
        return sb.toString();
    }

    private File createTempFile(String id) {
        try {
            File f = File.createTempFile("ap-" + id.substring(1), ".tmp");
            tmpFiles.put(id, f);
            return f;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() throws TimeoutException, InterruptedException {
        p.destroy();
        try {
            waitForExit(p, 5);
        } finally {
            for (File file : tmpFiles.values()) {
                 file.delete();
            }
        }
    }

    public long pid() {
        try {
            return (long) pid.invokeExact(p);
        } catch (Throwable e) {
            throw new IllegalStateException("Unsupported API", e);
        }
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void waitForExit() throws TimeoutException, InterruptedException {
        waitForExit(p, timeout);
    }

    private void waitForExit(Process p, int seconds) throws TimeoutException, InterruptedException {
        if (!p.waitFor(seconds, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new TimeoutException("Child process has not exited");
        }
    }

    public List<String> profile(String args) throws IOException, TimeoutException, InterruptedException {
        return profile(args, false);
    }

    public List<String> profile(String args, boolean sudo) throws IOException, TimeoutException, InterruptedException {
        // Give JVM process some time to initialize
        Thread.sleep(100);

        List<String> cmd = new ArrayList<>();
        if (sudo) {
            cmd.add("/usr/bin/sudo");
        }
        cmd.add("/bin/sh");
        cmd.add("profiler.sh");
        addArgs(cmd, args);
        cmd.add(Long.toString(pid()));
        log.info("Profiling " + cmd);

        Process p = new ProcessBuilder(cmd)
                .redirectOutput(createTempFile("%out"))
                .redirectError(createTempFile("%err"))
                .start();

        waitForExit(p, timeout);

        return readFile("%out");
    }

    public List<String> readFile(String id) {
        try {
            File f = tmpFiles.get(id);
            return Files.readAllLines(f.toPath());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }
}
