package test.pmu;

import one.profiler.test.Suite;
import one.profiler.test.Test;
import one.profiler.test.TestProcess;

import java.util.List;

public class PmuSuite extends Suite {

    @Test
    public static void cycles() throws Exception {
        try (TestProcess p = runJava(Dictionary.class, "")) {
            p.profile("-e cycles -d 3 -o collapsed -f %f");
            List<String> output = p.readFile("%f");
            assert ratio(output, "test/pmu/Dictionary.test128K") > 0.4;
            assert ratio(output, "test/pmu/Dictionary.test8M") > 0.4;
        }
    }

    @Test
    public static void cacheMisses() throws Exception {
        try (TestProcess p = runJava(Dictionary.class, "")) {
            p.profile("-e cache-misses -d 3 -o collapsed -f %f");
            List<String> output = p.readFile("%f");
            assert ratio(output, "test/pmu/Dictionary.test128K") < 0.2;
            assert ratio(output, "test/pmu/Dictionary.test8M") > 0.8;
        }
    }

    public static void main(String[] args) throws Exception {
        cycles();
        cacheMisses();
    }
}
