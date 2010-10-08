package virtualvoid.net;

import static org.testng.AssertJUnit.assertTrue;

import java.util.regex.Pattern;

import org.testng.annotations.Test;

public class HttpHandlerTest {
    private static boolean matches(Pattern p, String str) {
        return p.matcher(str).matches();
    }

    @Test
    public void testGetRequestLines() {
        assertTrue(matches(HttpHandler.GETHEADRequest, "GET /src/test/server/ HTTP/0.9\r\n"));
        assertTrue(matches(HttpHandler.GETHEADRequest, "GET /src/test/server/ HTTP/1.0\r\n"));
        assertTrue(matches(HttpHandler.GETHEADRequest, "GET /src/test/server/ HTTP/1.1\r\n"));

        assertTrue(matches(HttpHandler.GETHEADRequest, "HEAD /src/test/server/ HTTP/1.0\r\n"));

        assertTrue(matches(HttpHandler.GETHEADRequest, "GET / HTTP/1.1"));
    }
}
