package test.lock;

import one.profiler.test.Suite;
import one.profiler.test.Test;
import one.profiler.test.TestProcess;

import java.util.List;

public class LockSuite extends Suite {

    @Test
    public static void datagramSocketLock() throws Exception {
        try (TestProcess p = runJava(DatagramTest.class, DEBUG_NON_SAFEPOINTS)) {
            List<String> output = p.profile("-e cpu -d 3 -o collapsed");
            assert ratio(output, "pthread_cond_signal") > 0.1;

            output = p.profile("-e lock -d 3 -o collapsed");
            assert samples(output, "sun/nio/ch/DatagramChannelImpl.send") > 10;
        }
    }

    public static void main(String[] args) throws Exception {
        datagramSocketLock();
    }
}
