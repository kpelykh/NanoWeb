package http;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Request {

    private static final ConcurrentHashMap<String,String> _cache = new ConcurrentHashMap();

    protected abstract Map<String, String> handleRequest(Properties args);
    protected abstract String getView();
    protected abstract String getTemplate();

    private static HashMap<String, ArrayList<MenuItem> > _navbar = new HashMap();
    private static ArrayList<String> _navbarOrdering = new ArrayList();

    private static class MenuItem {
        public final Request _request;
        public final String _name;
        public MenuItem(Request request, String name) {
            _request = request;
            _name = name;
        }
        public void toHTML(StringBuilder sb) {
            sb.append("<li><a href='");
            sb.append(_request.getClass().getSimpleName().toLowerCase()+".html");
            sb.append("'>");
            sb.append(_name);
            sb.append("</a></li>");
        }

    }

    public static Request addToNavbar(Request r, String name) {
        ArrayList<MenuItem> arl = _navbar.get(name);
        if (arl == null) {
            arl = new ArrayList();
            _navbar.put(name,arl);
            _navbarOrdering.add(name);
        }
        arl.add(new MenuItem(r,name));
        return r;
    }

    public static Request addToNavbar(Request r, String name, String category) {
        ArrayList<MenuItem> arl = _navbar.get(category);
        if (arl == null) {
            arl = new ArrayList();
            _navbar.put(category,arl);
            _navbarOrdering.add(category);
        }
        arl.add(new MenuItem(r,name));
        return r;
    }

    public static void initializeNavBar() {
        StringBuilder sb = new StringBuilder();
        for (String s : _navbarOrdering) {
            ArrayList<MenuItem> arl = _navbar.get(s);
            if ((arl.size() == 1) && arl.get(0)._name.equals(s)) {
                arl.get(0).toHTML(sb);
            } else {
                sb.append("<li class='dropdown'>");
                sb.append("<a href='#' class='dropdown-toggle' data-toggle='dropdown'>");
                sb.append(s);
                sb.append("<b class='caret'></b>");
                sb.append("</a>");
                sb.append("<ul class='dropdown-menu'>");
                for (MenuItem i : arl)
                    i.toHTML(sb);
                sb.append("</ul></li>");
            }
        }
        HashMap<String, String> model = new HashMap<String, String>();
        model.put("NAVBAR", sb.toString());
        StrSubstitutor substitutor = new StrSubstitutor(model);
        _htmlTemplate = substitutor.replace(_htmlTemplate);
    }

    public NanoHTTPD.Response serve(GiraffaWebServer server, Properties args) {
        Map<String,String> model = handleRequest(args);
        String view = getView();
        if (view == null) {
            String templateStr = getTemplate();
            if (templateStr == null) {
                return server.new Response( NanoHTTPD.HTTP_INTERNALERROR, NanoHTTPD.MIME_HTML,
                        String.format("Page %s doesn't provide View or template", this.getClass().getName()));
            } else {
                String clazz = getClass().getSimpleName().toLowerCase();
                String template = _cache.get(clazz);
                if( template == null ) {
                    InputStream resource = server.getResource2(getTemplate());
                    try {
                        if (resource != null) {
                            try {
                                StringWriter writer = new StringWriter();
                                IOUtils.copy(resource, writer);
                                template = writer.toString();
                            } catch( IOException e ) {
                                return server.new Response( NanoHTTPD.HTTP_INTERNALERROR, NanoHTTPD.MIME_HTML,
                                        e.getClass().getSimpleName()+": "+e.getMessage());
                            }
                            String res = _cache.putIfAbsent(getTemplate(),template);
                            if( res != null ) template = res;
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
                    if (model.size() > 0) {
                        StrSubstitutor substitutor = new StrSubstitutor(model);
                        return wrap(server, substitutor.replace(template));
                    } else {
                        return wrap(server, template);
                    }

                }
            }

        }

        if (model.size() > 0) {
            StrSubstitutor substitutor = new StrSubstitutor(model);
            return wrap(server, substitutor.replace(getView()));
        } else {
            return wrap(server, getView());
        }

    }


    protected NanoHTTPD.Response wrap(NanoHTTPD server, final String response) {
        StrSubstitutor substitutor = new StrSubstitutor(new StrLookup() {
            @Override
            public String lookup(String key) {
                if (key.equals("CONTENTS")) return response;
                else return null;
            }
        });
        return server.new Response(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_HTML, substitutor.replace(_htmlTemplate));
    }

    private static String _htmlTemplate;

    static {
        InputStream resource = Request.class.getResourceAsStream("/webapp/page.html");
        try {
            _htmlTemplate = new String(IOUtils.toByteArray(resource));
        } catch (NullPointerException e) {
            System.err.println("page.html not found in resources.");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            if (resource != null) {
                try {
                    resource.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    /** Returns the name of the request, that is the request url without the
     * request suffix.
     */
    public static String requestName(String requestUrl) {
        String result = (requestUrl.endsWith(".html")) ? requestUrl.substring(0, requestUrl.length()-".html".length()) : requestUrl;
        if (result.charAt(0) == '/')
            return result.substring(1);
        return result;
    }
}


