package io.github.quackerjack.app.android;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

class ClearHistroyHttpCall {

    public static void main() throws IOException {
        URL url = new URL("https://duck123.uw.r.appspot.com/reset");
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

        httpConn.setRequestProperty("Content-Length", "0");

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";
        Log.v("ClearHistroyHttpCall", "Response: "+response);
    }
}