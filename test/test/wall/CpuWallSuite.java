package test.wall;

import one.profiler.test.Suite;
import one.profiler.test.Test;
import one.profiler.test.TestProcess;

import java.util.List;

public class CpuWallSuite extends Suite {

    @Test
    public static void cpuWall() throws Exception {
        try (TestProcess p = runJava(SocketTest.class, "")) {
            List<String> output = p.profile("-e cpu -d 3 -o collapsed");
            assert ratio(output, "test/wall/SocketTest.main") > 0.25;
            assert ratio(output, "test/wall/BusyClient.run") > 0.25;
            assert ratio(output, "test/wall/IdleClient.run") < 0.05;

            output = p.profile("-e wall -d 3 -o collapsed");
            long s1 = samples(output, "test/wall/SocketTest.main");
            long s2 = samples(output, "test/wall/BusyClient.run");
            long s3 = samples(output, "test/wall/IdleClient.run");
            assert s1 > 10 && s2 > 10 && s3 > 10;
            assert Math.abs(s1 - s2) < 5 && Math.abs(s2 - s3) < 5 && Math.abs(s3 - s1) < 5;
        }
    }

    public static void main(String[] args) throws Exception {
        cpuWall();
    }
}
