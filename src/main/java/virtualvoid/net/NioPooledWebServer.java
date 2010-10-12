package virtualvoid.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class NioPooledWebServer {
    private final ExecutorService executor = Settings.createExecutor();
    private final ExecutorCompletionService<SocketChannel> keepAliveChannels = new ExecutorCompletionService<SocketChannel>(executor);

    private void registerKeepAliveChannels(Selector selector) throws IOException, InterruptedException {
        long now = System.currentTimeMillis();
        Future<SocketChannel> alive;
        while ((alive = keepAliveChannels.poll()) != null) {
            try {
                SocketChannel channel = alive.get();
                if (channel != null) {
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ, now);
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
    private long cleanupKeepAliveChannels(long lastCleanup, Set<SelectionKey> keys) throws IOException {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > 5000) {
            System.err.println("Cleaning up keep-alive connections. "+keys.size());
            for (SelectionKey key: keys)
                if ((key.interestOps() & SelectionKey.OP_READ) != 0 &&
                        ((Long)key.attachment()) + Settings.keepAliveTimeout < now) {
                    System.out.println("Closing connection to "+key.channel());
                    key.channel().close();
                }
            return now;
        }
        else
            return lastCleanup;
    }

    public void run() throws IOException, InterruptedException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        ServerSocket theServer = serverChannel.socket();
        theServer.bind(Settings.endpoint);

        final Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        long lastCleanup = System.currentTimeMillis();
        while(true) {
            registerKeepAliveChannels(selector);
            lastCleanup = cleanupKeepAliveChannels(lastCleanup, selector.keys());

            if (selector.select(10) == 0)
                continue;

            for (SelectionKey key: selector.selectedKeys()) {
                if (key.isAcceptable()) {
                    // we take it for granted that only the server channel is
                    // registered for acception.
                    SocketChannel clientChannel = serverChannel.accept();
                    clientChannel.configureBlocking(true);
                    schedule(clientChannel);
                    key.cancel();
                } else if (key.isReadable()) {
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    key.cancel();
                    clientChannel.configureBlocking(true);
                    Logging.log("Reusing channel %s", clientChannel);
                    schedule(clientChannel);
                }
            }

            selector.selectNow();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        }
    }
    private void schedule(final SocketChannel clientChannel) {
        keepAliveChannels.submit(new Callable<SocketChannel>() {
            @Override
            public SocketChannel call() throws Exception {
                Socket client = clientChannel.socket();
                try {
                    boolean keepAlive = Settings.handler.handleConnection(client);

                    if (keepAlive)
                        return clientChannel;
                    else if (!client.isClosed())
                        client.close();
                } catch (IOException exception) {
                    System.err.println("Error when handling request: "+exception.getMessage());
                    exception.printStackTrace(System.err);

                    if (!client.isClosed())
                        client.close();
                }
                return null;
            }
        });
    }

    public static void main(String[] args) throws Exception {
        new NioPooledWebServer().run();
    }
}
