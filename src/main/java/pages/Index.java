package pages;

import http.Request;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: konstantin
 * Date: 2/22/13
 * Time: 11:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class Index extends Request {

    @Override
    protected Map<String, String> handleRequest(Properties args) {
        HashMap<String, String> model = new HashMap<String, String>();
        model.put("key1", "value1");
        return model;
    }

    @Override
    protected String getView() {
        return "<div class='container'><div class='hero-unit' style='overflow: hidden'>"
                + "<style scoped='scoped'>"
                + "  .col { height: 330px; position: relative;}"
                + "  .col p { overflow: hidden; text-overflow: ellipsis; -o-text-overflow: ellipsis;}"
                + "  .col a { position: absolute; right: 40px; bottom: 10px; } "
                + "</style>"
                + "<h1>${key1}</h1>"
                + "<h2><a href=\"info.html\">Info</a></h2>"
             + "</div></div>";

    }

    @Override
    protected String getTemplate() {
        return null;
    }
}
