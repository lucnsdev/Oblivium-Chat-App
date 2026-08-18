package lucns.oblivium.services.firebase;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class FirebaseMessagingSenderBase {

    public static final int ERROR_TIMEOUT = -10;
    public static final int ERROR_UNKNOWN_HOST = -11;
    public static final int ERROR_BAD_CONNECTION = -12;
    public static final int ERROR_NO_STREAM_DATA = -13;
    public static final int ERROR_NOT_FOUND = 404;
    protected int responseCode;
    protected String responseMessage;
    private String accessToken, registerId;
    private String projectId;
    private JSONObject jsonMessage;

    protected FirebaseMessagingSenderBase() {}

    public void setDestineRegisterId(String registerId) {
        this.registerId = registerId;
    }

    public void setMessage(JSONObject message) {
        this.jsonMessage = message;
    }

    public void setAccessToken(String token) {
        accessToken = token;
    }

    public void setProjectId(String id) {
        projectId = id;
    }

    protected void requestPost() {
        responseCode = 0;
        InputStreamReader inputStreamReader = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) (new URL("https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send")).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Content-Type", "application/json; UTF-8");

            String json;
            try {
                JSONObject jsonAndroid = new JSONObject();
                jsonAndroid.put("ttl", "60s");
                jsonAndroid.put("priority", "high");

                JSONObject message = new JSONObject();
                message.put("token", registerId);
                message.put("data", jsonMessage);
                message.put("android", jsonAndroid);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("message", message);
                json = jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            //Log.d("lucas", "json->" + json.toString());

            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            dos.write(json.getBytes(StandardCharsets.UTF_8));
            dos.flush();
            dos.close();

            connection.connect();
            responseCode = connection.getResponseCode();
            if (responseCode == 204) return;
            inputStreamReader = new InputStreamReader(connection.getInputStream());
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            if (responseCode == 0) {
                if (e instanceof SocketTimeoutException) responseCode = ERROR_TIMEOUT;
                else if (e instanceof UnknownHostException) responseCode = ERROR_UNKNOWN_HOST;
                else if (e instanceof ConnectException) responseCode = ERROR_BAD_CONNECTION;
                else if (e instanceof FileNotFoundException) responseCode = ERROR_NOT_FOUND;
                else responseCode = ERROR_NOT_FOUND;
            }
        }

        if (inputStreamReader == null && connection != null) {
            if (connection.getErrorStream() != null) {
                inputStreamReader = new InputStreamReader(connection.getErrorStream());
            } else {
                return;
            }
        } else {
            return;
        }

        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder builder = new StringBuilder();

        try {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            if (e instanceof EOFException && responseCode == 200) {
                responseCode = ERROR_NO_STREAM_DATA;
            }
            return;
        }
        responseMessage = builder.toString();
    }
}
