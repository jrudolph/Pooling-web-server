package virtualvoid.net;

import java.io.IOException;
import java.net.Socket;

/**
 * Handles an incoming connection. If the handler doesn't close the
 * socket, it's closed automatically.
 */
public interface Handler {
    void handleConnection(Socket client) throws IOException;
}
