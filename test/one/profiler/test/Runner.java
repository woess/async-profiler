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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Runner {
    private static final Logger log = Logger.getLogger(Runner.class.getName());

    private static final OsType currentOs = detectOs();
    private static final JvmType currentJvm = detectJvm();

    private static OsType detectOs() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")) {
            return OsType.LINUX;
        } else if (osName.contains("mac")) {
            return OsType.MACOS;
        } else if (osName.contains("windows")) {
            return OsType.WINDOWS;
        }
        throw new IllegalStateException("Unknown OS type");
    }

    private static JvmType detectJvm() {
        // TODO
        return JvmType.HOTSPOT;
    }

    public static boolean enabled(Test test) {
        OsType[] os = test.os();
        if (os.length > 0 && !Arrays.asList(os).contains(currentOs)) {
            return false;
        }

        JvmType[] jvm = test.jvm();
        if (jvm.length > 0 && !Arrays.asList(jvm).contains(currentJvm)) {
            return false;
        }

        return true;
    }

    public static void run(Method m) {
        for (Test test : m.getAnnotationsByType(Test.class)) {
            if (!enabled(test)) {
                log.info("Skipped " + m.getDeclaringClass().getName() + '.' + m.getName());
                continue;
            }

            log.info("Running " + m.getDeclaringClass().getName() + '.' + m.getName() + "...");
            try (TestProcess p = new TestProcess(test)) {
                Object holder = (m.getModifiers() & Modifier.STATIC) == 0 ? m.getDeclaringClass().newInstance() : null;
                m.invoke(holder, p);
                log.info("OK");
            } catch (InvocationTargetException e) {
                log.log(Level.WARNING, "Test failed", e.getCause());
            } catch (Exception e) {
                log.log(Level.WARNING, "Test failed", e);
            }
        }
    }

    public static void run(Class<?> cls) {
        for (Method m : cls.getMethods()) {
            run(m);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java " + Runner.class.getName() + " TestName ...");
            System.exit(1);
        }

        for (String testName : args) {
            if (testName.indexOf('.') < 0 && Character.isLowerCase(testName.charAt(0))) {
                // Convert package name to class name
                testName = "test." + testName + "." + Character.toUpperCase(testName.charAt(0)) + testName.substring(1) + "Tests";
            }
            run(Class.forName(testName));
        }
    }
}
