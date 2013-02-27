package pages;

import http.Request;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: konstantin
 * Date: 2/23/13
 * Time: 10:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class Info extends Request {

    @Override
    protected Map<String, String> handleRequest(Properties args) {
        HashMap<String, String> model = new HashMap<String, String>();
        model.put("key2", "value2");
        return model;
    }

    @Override
    protected String getView() {
        return null;
    }

    @Override
    protected String getTemplate() {
        return "/info.html";
    }
}
