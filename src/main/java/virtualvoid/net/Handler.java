package virtualvoid.net;

import java.io.IOException;
import java.net.Socket;

/**
 * Handles an incoming connection. If the handler doesn't close the
 * socket, it's closed automatically. Note, that Handlers have to be reentrant and
 * share the instance for all threads, so don't put any state inside.
 */
public interface Handler {
    void handleConnection(Socket client) throws IOException;
}
