package com.thatguysservice.huami_xdrip.webservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {
    public static final int PORT = 29863;
    private final String TAG = this.getClass().getSimpleName();
    private HashMap<String, CommonGatewayInterface> cgiEntries = new HashMap<String, CommonGatewayInterface>();

    public WebServer() {
        super(PORT);
    }

    @Override
    public Response serve(IHTTPSession session) {

        return serveCGI(session.getUri(), session.getMethod(), session.getHeaders(), session.getParameters());
    }

    public Response serveCGI(String uri, NanoHTTPD.Method method, Map<String, String> header, Map<String, List<String>> params) {
        CommonGatewayInterface cgi = cgiEntries.get(uri);
        if (cgi == null)
            return newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");

        String msg = cgi.run(params);
        if (msg == null)
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, msg);

        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, msg);
    }

    public void registerCGI(String uri, CommonGatewayInterface cgi) {
        if (cgi != null)
            cgiEntries.put(uri, cgi);
    }

    public interface CommonGatewayInterface {
        public String run(Map<String, List<String>> params);
    }
}
