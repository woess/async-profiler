package one.profiler.test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Runner {

    public static void run() throws IOException  {
        File out = File.createTempFile("aptest-out", ".tmp");
        out.deleteOnExit();

        File err = File.createTempFile("aptest-err", ".tmp");
        err.deleteOnExit();

        Process p = new ProcessBuilder(command)
                .in
                .redirectOutput(out)
                .redirectError(err)
                .start();
        try {
            
        } finally {
            err.delete();
            
            p.destroy();
            p.waitFor(5, TimeUnit.SECONDS);
            p.waitFor()
            p.destroy();
        }
    }
    
    private static File getTempFile()

    public static void main(String[] args) {

    }
}
