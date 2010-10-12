package virtualvoid.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * A simple pooling web server. It waits on the main thread for new connections and
 * then schedules them for processing with one of the threads from the pool.
 *
 * You can define the behaviour by setting executor and handler.
 * Field `executor` specifies the pooling strategy to use. The handler is called in its own
 * thread to handle an incoming connection.
 */
public class SimplePooledWebServer {
    private final ExecutorService executor = Settings.createExecutor();
    public void run() throws IOException {
        ServerSocket theServer = new ServerSocket();
        theServer.bind(Settings.endpoint);

        while(true) {
            final Socket client = theServer.accept();
            Logging.log("New connection: %s", client);
            executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        Settings.handler.handleConnection(client);
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
