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
 * A simple pooling web server. You can define the behaviour by setting executor and handler.
 * Variable executor specifies the pooling strategy to use. The handler is called in its own
 * thread to handle an incoming connection.
 */
public class PooledWebServer {
    private final ExecutorService executor = Executors.newFixedThreadPool(30);
    private final Handler handler = new StaticHttpFileHandler(new File("www"));
    private final SocketAddress endpoint =
        new InetSocketAddress(8020);

    public void run() throws IOException {
        ServerSocket theServer = new ServerSocket();
        theServer.bind(endpoint);

        while(true) {
            final Socket client = theServer.accept();
            Logging.log("New connection to %s", client);

            // schedule for processing in a thread of the thread pool
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
        new PooledWebServer().run();
    }
}
