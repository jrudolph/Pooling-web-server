package virtualvoid.net;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PooledWebServer {
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final ExecutorCompletionService<SocketChannel> keepAliveChannels = new ExecutorCompletionService<SocketChannel>(executor);
    private final Handler handler = new StaticHttpFileHandler(new File("www"));


    private static InetSocketAddress addr(int i1, int i2, int i3, int i4) {
        try {
            return new InetSocketAddress(InetAddress.getByAddress(new byte[]{(byte)i1, (byte)i2, (byte)i3, (byte)i4}), 80);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
    //private final InetSocketAddress bindAddress = new InetSocketAddress(8020);
    private final InetSocketAddress bindAddress = addr(192, 168, 2, 200);

    private final long KEEP_ALIVE = 20000;

    public void run() throws IOException, InterruptedException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        ServerSocket theServer = serverChannel.socket();
        theServer.bind(bindAddress);

        final Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        long lastCleanup = System.currentTimeMillis();
        while(true) {
            long now = System.currentTimeMillis();
            Future<SocketChannel> alive;
            while ((alive = keepAliveChannels.poll()) != null) {
                try {
                    SocketChannel channel = alive.get();
                    if (channel != null) {
                        System.err.println("Found a still usable channel: "+channel);
                        channel.configureBlocking(false);
                        channel.register(selector, SelectionKey.OP_READ, now);
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

            long cur = System.currentTimeMillis();
            if (cur - lastCleanup > 5000) {
                lastCleanup = cur;
                System.err.println("Cleaning up keep-alive connections."+selector.keys().size());
                for (SelectionKey key: selector.keys())
                    if ((key.interestOps() & SelectionKey.OP_READ) != 0 &&
                            ((Long)key.attachment()) + KEEP_ALIVE < cur) {
                        System.out.println("Closing connection to "+key.channel());
                        key.channel().close();
                    }
                System.err.println(selector.keys().size());
            }

            if (selector.select(1) == 0) {
                continue;
            }

            LinkedList<SelectionKey> keysToCancel = new LinkedList<SelectionKey>();
            for (SelectionKey key: selector.selectedKeys()) {
                if (key.isAcceptable()) {
                    // we take it for granted that only the server channel is
                    // registered for acception.
                    SocketChannel clientChannel = serverChannel.accept();
                    clientChannel.configureBlocking(true);
                    schedule(clientChannel);
                    keysToCancel.add(key);
                } else if (key.isReadable()) {
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    key.cancel();
                    clientChannel.configureBlocking(true);
                    System.out.println("Reusing channel "+clientChannel);
                    keysToCancel.add(key);
                    schedule(clientChannel);
                } else {
                    System.err.println("Key wont be used: "+key);
                }
            }

            for (SelectionKey key: keysToCancel)
                key.cancel();
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
                    boolean keepAlive = handler.handleConnection(client);

                    if (keepAlive) {
                        /*Selector sel = Selector.open();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(sel, SelectionKey.OP_READ);
                        boolean selected = sel.selectNow() > 0;
                        sel.close();
                        if (selected)
                            return call();*/

                        return clientChannel;
                    }
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
        new PooledWebServer().run();
    }
}
