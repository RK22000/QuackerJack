package io.github.quackerjack.app.android;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class Httpcall {

    public static final String
            SNIPPET = "snippet",
            MODE = "mode",
            RESPONSE = "response",
            NAME = "name";
    public static interface HttpResponseCallback {
        void onServerResponse(String botReply);
    }
    public static void main(JSONObject json, HttpResponseCallback callback) throws IOException, JSONException {
        URL url = new URL("https://duck123.uw.r.appspot.com/chatbot");
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

        httpConn.setRequestProperty("Content-Type", "application/json");

        httpConn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
//        writer.write("{\"snippet\": \"I'm sad\", \"mode\": \"nice\"}");
        writer.write(json.toString());
        writer.flush();
        writer.close();
        httpConn.getOutputStream().close();

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";
        Log.v("Httpcall", response);
        JSONObject reply = new JSONObject(response);
        callback.onServerResponse(reply.getString(RESPONSE));
    }
}
