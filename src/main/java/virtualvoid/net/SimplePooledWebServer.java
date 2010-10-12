package virtualvoid.net;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple pooling web server. It waits on the main thread for new connections and
 * then schedules them for processing with one of the threads from the pool.
 *
 * You can define the behaviour by setting executor and handler.
 * Field `executor` specifies the pooling strategy to use. The handler is called in its own
 * thread to handle an incoming connection.
 */
public class SimplePooledWebServer {
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
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler handler = new StaticHttpFileHandler(new File("www"));
    private final SocketAddress endpoint =
        new InetSocketAddress(8020);

    public void run() throws IOException {
        ServerSocket theServer = new ServerSocket();
        theServer.bind(endpoint);

        while(true) {
            final Socket client = theServer.accept();
            Logging.log("New connection: %s", client);
            executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        handler.handleConnection(client);
                    } catch (IOException exception) {
                        System.err.println("Error when handling request: "+exception.getMessage());
                        exception.printStackTrace(System.err);
                    } finally {
                        if (!client.isClosed())
                            client.close();
                    }
                    return null;
                }
            });
        }
    }

    public static void main(String[] args) throws IOException {
        new SimplePooledWebServer().run();
    }
}
