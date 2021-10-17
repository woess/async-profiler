package test.alloc;

import one.profiler.test.Suite;
import one.profiler.test.Test;
import one.profiler.test.TestProcess;

import java.util.List;

public class AllocSuite extends Suite {

    @Test
    public static void alloc() throws Exception {
        try (TestProcess p = runJava(MapReader.class, "-XX:+UseG1GC")) {
            List<String> output = p.profile("-e cpu -d 3 -o collapsed");
            assert contains(output, "G1RemSet::");

            output = p.profile("-e alloc -d 3 -o collapsed");
            assert contains(output, "java/io/BufferedReader.readLine;");
            assert contains(output, "java/lang/String.split;");
            assert contains(output, "java/lang/String.trim;");
            assert contains(output, "java\\.lang\\.String\\[]");
        }
    }

    @Test
    public static void allocTotal() throws Exception {
        try (TestProcess p = runJava(MapReaderOpt.class, "-XX:+UseParallelGC")) {
            List<String> output = p.profile("-e alloc -d 3 -o collapsed --total");
            assert samples(output, "java.util.HashMap\\$Node\\[]") > 1_000_000;

            output = p.profile("stop -o flamegraph --total");
            assert contains(output, "f\\(\\d+,\\d+,\\d+,1,'java.lang.Long'\\)");
            assert contains(output, "f\\(\\d+,\\d+,\\d+,2,'java.util.HashMap\\$Node\\[]'\\)");
        }
    }

    @Test
    public static void startup() throws Exception {
        try (TestProcess p = runWithAgent(Hello.class, "-XX:+UseG1GC -XX:-UseTLAB", "start,event=alloc,cstack=fp,flamegraph,file=%f")) {
            p.waitForExit();
            List<String> output = p.readFile("%f");
            assert contains(output, "f\\(\\d+,\\d+,\\d+,\\d,'JNI_CreateJavaVM'\\)");
            assert contains(output, "f\\(\\d+,\\d+,\\d+,\\d,'java/lang/ClassLoader.loadClass'\\)");
            assert contains(output, "f\\(\\d+,\\d+,\\d+,\\d,'java\\.lang\\.Class'\\)");
            assert contains(output, "f\\(\\d+,\\d+,\\d+,\\d,'java\\.lang\\.Thread'\\)");
            assert contains(output, "f\\(\\d+,\\d+,\\d+,\\d,'java\\.lang\\.String'\\)");
            assert contains(output, "f\\(\\d+,\\d+,\\d+,\\d,'int\\[]'\\)");
        }
    }

    @Test
    public static void humongous() throws Exception {
        try (TestProcess p = runWithAgent(MapReaderOpt.class, "-XX:+UseG1GC -XX:G1HeapRegionSize=1M", "start,event=G1CollectedHeap::humongous_obj_allocate")) {
            Thread.sleep(2000);
            List<String> output = p.profile("stop -o collapsed");
            assert contains(output, "java/io/ByteArrayOutputStream.toByteArray;");
            assert contains(output, "G1CollectedHeap::humongous_obj_allocate");
        }
    }

    public static void main(String[] args) throws Exception {
        alloc();
        allocTotal();
        startup();
        humongous();
    }
}
