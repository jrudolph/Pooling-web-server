package virtualvoid.net;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple pooling web server. You can define the behaviour by setting executor and handler.
 * Field `executor` specifies the pooling strategy to use. The handler is called in its own
 * thread to handle an incoming connection.
 */
public class PooledWebServer {
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
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        ServerSocket theServer = serverChannel.socket();
        theServer.bind(endpoint);

        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while(true) {
            if (selector.select(1000) == 0)
                // TODO: we are idle, so do cleanup
                continue;

            for (SelectionKey key: selector.selectedKeys()) {
                if (key.isAcceptable()) {
                    // we take it for granted that only the server channel is
                    // registered for acception.
                    SocketChannel clientChannel = serverChannel.accept();

                    // we have to re-register the key all the time because of
                    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
                    key.cancel();
                    selector.selectNow();
                    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
                    final Socket client = clientChannel.socket();

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
        }
    }

    public static void main(String[] args) throws IOException {
        new PooledWebServer().run();
    }
}
