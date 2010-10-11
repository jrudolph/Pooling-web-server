package virtualvoid.net;

import static virtualvoid.net.Logging.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An abstract HttpHandler class which does basic parsing and defers the actual
 * result generation to abstract methods serverHeader and serveBody.
 */
public abstract class HttpHandler implements Handler {
    public static interface Response {
        void addResponseHeader(String header, String value);
    }
    public static abstract class Result {
        private final String resultCode;
        public Result(String resultCode) {
            this.resultCode = resultCode;
        }
        public String getResultCode() {
            return resultCode;
        }
        protected abstract void writeBody(OutputStream os) throws IOException;
    }

    final static Pattern GETHEADRequest = Pattern.compile("(GET|HEAD) ([^ ]+) HTTP/(\\d\\.\\d)");

    private void fail(Writer writer, String code) throws IOException {
        writer.append("HTTP/1.0 ")
            .append(code)
            .append("\r\n");
    }

    @Override
    public boolean handleConnection(Socket client) throws IOException {
        // fail if a read takes longer than 5s
        client.setSoTimeout(5000);

        return waitAndServeRequest(client);
    }
    private boolean waitAndServeRequest(Socket client) throws IOException {
        final OutputStream os = client.getOutputStream();
        final Writer writer = new OutputStreamWriter(os);

        final InputStream is = client.getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        // Request processing
        try {
            String requestLine = reader.readLine();
            log("Got request '%s'", requestLine);

            // Headers have to be send continuously without to much pauses in between
            client.setSoTimeout(2000);
            skipHeaders(reader);

            Matcher lineMatcher = GETHEADRequest.matcher(requestLine);
            if (lineMatcher.matches()) {
                String method = lineMatcher.group(1);
                String uri = lineMatcher.group(2);
                String version = lineMatcher.group(3);

                boolean onlyHeader = "HEAD".equals(method);

                if (!("1.0".equals(version) || "1.1".equals(version)))
                    fail(writer, "501 Version not implemented "+version);
                else {
                    final StringWriter headerBuffer = new StringWriter();
                    Result res = serve(uri, new Response() {
                        @Override
                        public void addResponseHeader(String header, String value) {
                            headerBuffer
                                .append(header)
                                .append(':')
                                .append(value)
                                .append("\r\n");
                        }
                    });

                    writer.append("HTTP/")
                        .append(version)
                        .append(' ')
                        .append(res.getResultCode())
                        .append("\r\n")
                        .append(headerBuffer.getBuffer().toString())
                        .append("\r\n");

                    writer.flush();
                    if (!onlyHeader)
                        res.writeBody(os);

                    // keep-alive logic for HTTP 1.1
                    if ("1.1".equals(version)) {
                        //client.setSoTimeout(20000);

                        if (is.available() > 0) {
                            System.out.println("More bits available");
                            return waitAndServeRequest(client);
                        }
                        else
                            return true;
                    }
                }
            } else {
                System.err.printf("Bad Request: '%s'\n",requestLine);
                fail(writer, "400 Bad Request");
            }
        } catch(SocketTimeoutException e) {
            fail(writer, "408 Request Timeout");
        }

        writer.flush();
        writer.close();

        return false;
    }

    private void skipHeaders(BufferedReader reader) throws IOException {
        // read and skip request headers
        int length = 0;
        do {
            String line = reader.readLine();
            length = line.length();
            if (length > 0)
                log("Skipping header: '%s'", line);
        } while (length > 0) ;
    }

    protected abstract Result serve(String uri, Response resp);
}
