package http;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class GiraffaWebServer extends NanoHTTPD {

    // cache of all loaded resources
    private static final ConcurrentHashMap<String,byte[]> _cache = new ConcurrentHashMap();
    protected static final HashMap<String,Request> _requests = new HashMap();

    private boolean fromResources;
    private String resources;
    private File myRootDir;

    public Request registerRequest(Request req) {
        String href = req.getClass().getSimpleName().toLowerCase();
        assert (! _requests.containsKey(href)) : "Request with href "+href+" already registered";
        _requests.put(href,req);
        return req;
    }

    // uri serve -----------------------------------------------------------------

    @Override public NanoHTTPD.Response serve( String uri, String method, Properties header, Properties params, Properties files ) {
        // Jack priority for user-visible requests
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY-1);
        if( uri.isEmpty() || uri.equals("/") ) uri = "/index.html";
        // determine the request type
        String requestName = Request.requestName(uri);
        try {
            // determine if we have known resource
            Request request = _requests.get(requestName);
            // if the request is not know, treat as resource request
            if (request == null)
                return getResource(uri, header);
            // otherwise unify get & post arguments
            params.putAll(files);
            // call the request
            return request.serve(this, params);
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
            return new Response( HTTP_INTERNALERROR, MIME_PLAINTEXT,e.getClass().getSimpleName()+": "+e.getMessage());
        }
    }

    public GiraffaWebServer(int port, String resources) throws IOException {
        super(port,null);
        this.resources = resources;
        this.fromResources = true;
    }

    public GiraffaWebServer(int port, File wwwDir) throws IOException {
        super(port,wwwDir);
        this.myRootDir = wwwDir;
    }



    // Resource loading ----------------------------------------------------------

    public InputStream getResource2(String uri) {
        if( fromResources ) {
            return ClassLoader.getSystemClassLoader().getResourceAsStream(resources+uri);
        } else {
            try {
                return new FileInputStream(new File(this.myRootDir + uri));
            } catch (FileNotFoundException e) {
                return null;
            }
        }
    }

    // Returns the response containing the given uri with the appropriate mime
    // type.
    private NanoHTTPD.Response getResource(String uri, Properties header) {
        byte[] bytes = _cache.get(uri);
        if( bytes == null ) {
            InputStream resource = getResource2(uri);
            try {
                if (resource != null) {
                    try {
                        bytes = IOUtils.toByteArray(resource);
                    } catch( IOException e ) { }
                    byte[] res = _cache.putIfAbsent(uri,bytes);
                    if( res != null ) bytes = res; // Racey update; take what is in the _cache
                }

            } finally {
                if (resource != null) {
                    try {
                        resource.close();
                    } catch (IOException e) {
                        System.err.println(e.getStackTrace());
                    }
                }
            }
        }

        if ((bytes == null) || (bytes.length == 0)) {
            // make sure that no Exception is ever thrown out from the request
            Properties parms = new Properties();
            parms.setProperty(NanoHTTPD.HTTP_NOTFOUND,uri);
            return new Response( HTTP_NOTFOUND, MIME_PLAINTEXT,
                    "Error 404, file not found." );
        }
        String mime = MIME_DEFAULT_BINARY;
        if (uri.endsWith(".css"))
            mime = "text/css";
        else if (uri.endsWith(".html"))
            mime = "text/html";
        return new NanoHTTPD.Response(HTTP_OK,mime,new ByteArrayInputStream(bytes));
    }

}
