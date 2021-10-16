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

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class Suite {
    public static final String DEBUG_NON_SAFEPOINTS = "-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints";

    public static TestProcess runJava(Class<?> mainClass, String jvmArgs) throws IOException {
        return new TestProcess(mainClass.getName(), jvmArgs);
    }

    public static TestProcess runWithAgent(Class<?> mainClass, String jvmArgs, String agentArgs) throws IOException {
        return runJava(mainClass, jvmArgs + " -agentpath:build/libasyncProfiler.so=" + agentArgs);
    }

    public static boolean contains(List<String> output, String regex) {
        Pattern pattern = Pattern.compile(regex);
        return output.stream().anyMatch(s -> pattern.matcher(s).find());
    }

    public static long samples(List<String> output, String regex) {
        Pattern pattern = Pattern.compile(regex);
        return output.stream()
                .filter(s -> pattern.matcher(s).find())
                .mapToLong(Suite::extractSamples)
                .sum();
    }

    public static double ratio(List<String> output, String regex) {
        Pattern pattern = Pattern.compile(regex);
        long total = 0;
        long match = 0;
        for (String s : output) {
            long samples = extractSamples(s);
            total += samples;
            if (pattern.matcher(s).find()) {
                match += samples;
            }
        }
        return (double) match / total;
    }

    private static long extractSamples(String s) {
        return Long.parseLong(s.substring(s.lastIndexOf(' ') + 1));
    }
}
