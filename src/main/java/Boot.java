import http.Request;
import pages.Index;
import http.GiraffaWebServer;
import pages.Info;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: konstantin
 * Date: 2/20/13
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class Boot {

    public static int WEB_PORT = 9090;

    public static void main( String[] args ) throws IOException {

        GiraffaWebServer webServer = new GiraffaWebServer(WEB_PORT, "webapp");
        webServer.registerRequest(new Index());

        Request.addToNavbar(webServer.registerRequest(new Info()),      "Info");
        Request.initializeNavBar();

        System.out.println("Listening on port " + WEB_PORT + ". Hit Enter to stop.\nPlease open your browsers to http://localhost:"
                + WEB_PORT);
        try {
            System.in.read();
        } catch (Throwable ignore) {}

    }
}

