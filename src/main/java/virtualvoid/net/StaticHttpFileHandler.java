package virtualvoid.net;

import static virtualvoid.net.Logging.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An HttpHandler which interprets URIs as files with paths relative to
 * a root directory.
 */
public class StaticHttpFileHandler extends HttpHandler {
    private static final int BUFFERSIZE = 65536;
    private final File root;

    public StaticHttpFileHandler(File root) {
        super();
        this.root = root;
    }

    private File fileByPath(String path) {
        /* FIXME: This is a potential security risk.
         * In a real application, you would have to make
         * sure that you effectively restrict the scope of
         * accessible files to the ones intended by excluding
         * the parent path (..) and symbolic links etc.
         */

        // strip off leading slashes
        while(path.startsWith("/"))
            path = path.substring(1);

        return new File(root, path);
    }
    private String mimeTypeByExtension(File f) {
        String name = f.getName();
        String extension = name.substring(name.lastIndexOf(".") + 1);
        if ("html".equals(extension))
            return "text/html";
        else
            return "application/octet-stream";
    }
    @Override
    protected Result serve(String uri) {
        final File f = fileByPath(uri);

        if (f.exists() && f.isFile() && !f.isHidden()) {
            log("Serving '%s'", f);

            return new Result("200 OK") {
                @Override
                protected void writeBody(OutputStream os) throws IOException {
                    InputStream is = new FileInputStream(f);

                    try {
                        byte[] buffer = new byte[BUFFERSIZE];

                        while (is.available() > 0) {
                            int read = is.read(buffer);
                            os.write(buffer, 0, read);
                        }
                    } finally {
                        is.close();
                    }
                }
                protected void addHeaders() {
                    addResponseHeader("Content-Type", mimeTypeByExtension(f));
                    addResponseHeader("Content-Length", Long.toString(f.length()));
                }
            };
        } else {
            return new Result("404 File not found") {
                @Override
                protected void writeBody(OutputStream os) {
                    // No body for 404
                }
                @Override
                protected void addHeaders() {
                    addResponseHeader("Content-Length", "0");
                }
            };
        }
    }
}
