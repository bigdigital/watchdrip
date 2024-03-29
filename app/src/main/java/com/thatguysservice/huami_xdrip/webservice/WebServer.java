package com.thatguysservice.huami_xdrip.webservice;

import com.thatguysservice.huami_xdrip.models.database.UserError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

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
        UserError.Log.d(TAG, "serveCGI: " + uri );
        CommonGatewayInterface cgi = cgiEntries.get(uri);
        if (cgi == null)
            return newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
        String msg = null;
        try {
            msg = cgi.run(params);
        } catch (TimeoutException e) {
            UserError.Log.d(TAG, "CGIResp TimeoutException: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.REQUEST_TIMEOUT, MIME_PLAINTEXT, e.getMessage());
        } catch (Exception e) {
            UserError.Log.d(TAG, "CGIResp Exception: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, e.getMessage());
        }
        UserError.Log.d(TAG, "CGIResp: " + msg);
        if (msg == null)
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, msg);

        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, msg);
    }

    public void registerCGI(String uri, CommonGatewayInterface cgi) {
        if (cgi != null)
            cgiEntries.put(uri, cgi);
    }

    public interface CommonGatewayInterface {
        public String run(Map<String, List<String>> params) throws Exception;
    }
}
