package test.kernel;

import one.profiler.test.Suite;
import one.profiler.test.Test;
import one.profiler.test.TestProcess;

import java.util.List;

public class KernelSuite extends Suite {

    @Test
    public static void kernel() throws Exception {
        try (TestProcess p = runJava(ListFiles.class, "")) {
            List<String> output = p.profile("-e cpu -d 3 -i 1ms -o collapsed");
            assert contains(output, "test/kernel/ListFiles.listFiles;java/io/File");
            assert contains(output, "sys_getdents");

            output = p.profile("stop -o flamegraph");
            assert contains(output, "f\\(\\d+,\\d+,\\d+,0,'java/io/File.list'\\)");
            assert contains(output, "f\\(\\d+,\\d+,\\d+,2,'.*sys_getdents'\\)");
        }
    }

    @Test
    public static void fdtransfer() throws Exception {
        try (TestProcess p = runJava(ListFiles.class, "")) {
            p.profile("-e cpu -d 3 -i 1ms -o collapsed -f %f --fdtransfer", true);
            List<String> output = p.readFile("%f");
            assert contains(output, "test/kernel/ListFiles.listFiles;java/io/File");
            assert contains(output, "sys_getdents");
        }
    }

    public static void main(String[] args) throws Exception {
        kernel();
        fdtransfer();
    }
}
