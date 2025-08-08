package in.daydreamers.devappsanalytics;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class DevAppsTime {

        private static final String NTP_SERVER = "time.google.com";  // NTP server
        private static final int NTP_PORT = 123;                     // NTP port

    public static long getCurrentTimeFromNTP() {
        String urlString = "http://time.google.com";  // This is just an example URL, it could be any HTTP server

        try {
            // Create a URL object from the string
            URL url = new URL(urlString);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method (GET request)
            connection.setRequestMethod("GET");

            // Connect to the server
            connection.connect();

            // Get the "Date" header from the HTTP response
            String dateHeader = connection.getHeaderField("Date");
            Log.i("DevApps","dateHeader="+dateHeader);
            // Close the connection
            connection.disconnect();

            if (dateHeader != null) {
                // Parse the "Date" header string into a Date object
                Date date = new Date(dateHeader);
                Log.i("DevApps","date="+date.getTime());
                return date.getTime();
            } else {

                return new Date().getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Date().getTime();
    }
    }

