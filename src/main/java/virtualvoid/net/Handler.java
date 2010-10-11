package virtualvoid.net;

import java.io.IOException;
import java.net.Socket;

/**
 * Handles an incoming connection. If the handler returns true, the socket is
 * monitored for incoming data, in which case the socket is scheduled for another
 * time of processing.
 */
public interface Handler {
    boolean handleConnection(Socket client) throws IOException;
}
