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
 * Variable executor specifies the pooling strategy to use. The handler is called in its own
 * thread to handle an incoming connection.
 */
public class PooledWebServer {
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
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
