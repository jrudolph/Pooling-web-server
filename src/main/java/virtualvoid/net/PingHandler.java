package virtualvoid.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * A simple handler which reads maximally 100 bytes and returns them to the sender.
 */
public class PingHandler implements Handler {
    @Override
    public void handleConnection(Socket client) throws IOException {
        byte[] buffer = new byte[100];

        InputStream is = client.getInputStream();
        OutputStream os = client.getOutputStream();

        int read = is.read(buffer);
        if (read > 0) {
            os.write(Integer.toString(read).getBytes());
            os.write(buffer, 0, read);
        }
        client.close();
    }
}
