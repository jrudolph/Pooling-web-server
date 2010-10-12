package virtualvoid.net;

import java.io.IOException;
import java.net.Socket;

/**
 * Handles an incoming connection. If the handler returns true, the socket is
 * monitored for incoming data, in which case the socket is scheduled for another
 * time of processing. Note, that Handlers have to be reentrant and
 * share the instance for all threads, so don't put any state inside.
 */
public interface Handler {
    boolean handleConnection(Socket client) throws IOException;
}
