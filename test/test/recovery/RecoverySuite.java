package test.recovery;

import one.profiler.test.Suite;
import one.profiler.test.Test;
import one.profiler.test.TestProcess;

import java.util.List;

public class RecoverySuite extends Suite {

    @Test
    public static void stringBuilder() throws Exception {
        try (TestProcess p = runJava(StringBuilderTest.class, "")) {
            Thread.sleep(1000);

            List<String> output = p.profile("-d 3 -e cpu -o collapsed");
            assert ratio(output, "StringBuilder.delete;") > 0.9;
            assert ratio(output, "arraycopy") > 0.9;
            assert ratio(output, "unknown_Java") < 0.01;

            output = p.profile("-d 3 -e cpu -o collapsed --safe-mode 12");
            assert ratio(output, "StringBuilder.delete;") < 0.1;
            assert ratio(output, "unknown_Java") > 0.5;
        }
    }

    @Test
    public static void numbers() throws Exception {
        try (TestProcess p = runJava(Numbers.class, DEBUG_NON_SAFEPOINTS)) {
            Thread.sleep(1000);

            List<String> output = p.profile("-d 3 -e cpu -o collapsed");
            assert ratio(output, "unknown_Java") < 0.01;
            assert ratio(output, "vtable stub") > 0.01;
            assert ratio(output, "Numbers.loop") > 0.8;

            output = p.profile("-d 3 -e cpu -o collapsed --safe-mode 31");
            assert ratio(output, "unknown_Java") > 0.1;
        }
    }

    @Test
    public static void suppliers() throws Exception {
        try (TestProcess p = runJava(Suppliers.class, DEBUG_NON_SAFEPOINTS)) {
            Thread.sleep(1000);

            List<String> output = p.profile("-d 3 -e cpu -o collapsed --safe-mode 31");
            assert ratio(output, "unknown_Java") > 0.2;

            output = p.profile("-d 3 -e cpu -o collapsed");
            assert ratio(output, "unknown_Java") < 0.01;
            assert ratio(output, "itable stub") > 0.01;
            assert ratio(output, "Suppliers.loop") > 0.8;
        }
    }

    public static void main(String[] args) throws Exception {
        stringBuilder();
        numbers();
        suppliers();
    }
}
