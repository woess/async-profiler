package test.smoke;

import one.profiler.test.Suite;
import one.profiler.test.Test;
import one.profiler.test.TestProcess;

import java.util.List;

public class SmokeTest extends Suite {

    @Test
    public static void cpu() throws Exception {
        try (TestProcess p = runJava(Cpu.class, "")) {
            List<String> output = p.profile("-d 3 -e cpu -o collapsed");
            assert contains(output, "test/smoke/Cpu.main;test/smoke/Cpu.method1 ");
            assert contains(output, "test/smoke/Cpu.main;test/smoke/Cpu.method2 ");
            assert contains(output, "test/smoke/Cpu.main;test/smoke/Cpu.method3;java/io/File");
        }
    }

    @Test
    public static void alloc() throws Exception {
        try (TestProcess p = runJava(Alloc.class, "")) {
            List<String> output = p.profile("-d 3 -e alloc -o collapsed -t");
            assert contains(output, "\\[AllocThread-1 tid=[0-9]+];.*Alloc.allocate;.*java.lang.Integer\\[]");
            assert contains(output, "\\[AllocThread-2 tid=[0-9]+];.*Alloc.allocate;.*int\\[]");
        }
    }

    @Test
    public static void threads() throws Exception {
        try (TestProcess p = runWithAgent(Threads.class, "", "start,event=cpu,collapsed,threads,file=%f")) {
            p.waitForExit();
            List<String> output = p.readFile("%f");
            System.out.println(output);
            assert contains(output, "\\[ThreadEarlyEnd tid=[0-9]+];.*Threads.methodForThreadEarlyEnd;.*");
            assert contains(output, "\\[RenamedThread tid=[0-9]+];.*Threads.methodForRenamedThread;.*");
        }
    }

    @Test
    public static void loadLibrary() throws Exception {
        try (TestProcess p = runJava(LoadLibrary.class, "")) {
            p.profile("-f %f -o collapsed -d 4 -i 1ms");
            List<String> output = p.readFile("%f");
            assert contains(output, "Java_sun_management");
        }
    }


    public static void main(String[] args) throws Exception {
        cpu();
        alloc();
        threads();
        loadLibrary();
    }
}
