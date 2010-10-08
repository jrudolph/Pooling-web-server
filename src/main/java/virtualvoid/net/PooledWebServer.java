package virtualvoid.net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PooledWebServer {
    private final ExecutorService executor = Executors.newFixedThreadPool(30);
    private final Handler handler = new PingHandler();

    public void run() throws IOException {
        ServerSocket theServer = new ServerSocket();
        theServer.bind(new InetSocketAddress(Inet4Address.getByAddress(new byte[]{(byte) 192,(byte) 168,2,20}), 2020));
        //theServer.bind(new InetSocketAddress(Inet4Address.getByAddress(new byte[]{127,0,0,1}), 8020));
        System.out.println(theServer.getLocalSocketAddress());

        while(true) {
            final Socket client = theServer.accept();
            //System.out.println("New connection: "+client);
            executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        //System.out.println("Starting to handle connection: "+client);
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
