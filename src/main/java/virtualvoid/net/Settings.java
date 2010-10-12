package virtualvoid.net;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class Settings {
    // This is the most important configuration to do and you would have to find
    // out experimentally which is the setting for the pool.
    // Without keep-alive and when the handling of request is mainly CPU-bound
    // you would use about N threads (with N being the number of CPUs).
    // When processing requests contains wait times (e.g. because of I/O) you
    // would have to increase the number of threads to N*(1+WT/ST) with WT/ST being the ratio
    // of wait time to service time.
    //
    // E.g. see Brian Goetz, Java theory and practice: Thread pools and work queues
    // http://www.ibm.com/developerworks/java/library/j-jtp0730.html
    //
    // With keep-alive in place, configuration becomes harder because you have
    // to figure out both, keep-alive timeouts and the number of threads. With a long too long timeout
    // you might keep open many connections and threads waiting for requests and using system resources.
    //
    // One possibility is to decouple connections and threads by putting open, waiting connections into
    // a list which is monitored for new data. When new data arrives the requests are scheduled again
    // for processing. (not implemented here)
    public final static ExecutorService createExecutor() {
        return Executors.newFixedThreadPool(2);
    }

    /**
     * The handler which handles requests.
     */
    public final static Handler handler = new StaticHttpFileHandler(new File("www"));
    /**
     * The endpoint to bind to.
     */
    public final static SocketAddress endpoint = new InetSocketAddress(8020);
    /**
     * The socket timeout between connection accept and expecting the first request.
     */
    public final static int firstReadTimeout = 5000;
    /**
     * The timeout when waiting for headers.
     */
    public final static int headerTimeout = 2000;
    /**
     * The timeout in keep-alive connections when waiting for the next request
     */
    public final static int keepAliveTimeout = 20000;

}
