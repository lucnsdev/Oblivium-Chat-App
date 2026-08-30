package lucns.oblivium.services.firebase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FirebaseMessagingSender extends FirebaseMessagingSenderBase {

    // present in google-services.json file on the project_id json key
    // https://console.firebase.google.com/project/mysamples-4f48d/settings/general/android:lucns.oblivium
    private static final String PROJECT_ID = "mysamples-4f48d";
    private GToken gToken;
    private MessageIdManager idManager;

    public interface Callback {
        void onFinish(int id, int responseCode, String responseMessage);
    }

    private final Callback callback;
    private final Context context;
    private final Queue<FirebaseMessage> listMessages;
    private boolean requestingToken;

    public FirebaseMessagingSender(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
        this.listMessages = new LinkedList<>();
        this.idManager = MessageIdManager.getInstance();
        setProjectId(PROJECT_ID);
        gToken = readAccessToken();
        if (gToken == null) getAccessToken();
        else setAccessToken(gToken.token);
    }

    public int put(JSONObject jsonObject) {
        int id = idManager.next();
        boolean emptying = !listMessages.isEmpty();
        listMessages.add(new FirebaseMessage(id, jsonObject));
        if (!emptying) dequeue();
        return id;
    }

    private void dequeue() {
        if (!isTokenValid()) {
            getAccessToken();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!isTokenValid()) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            getAccessToken();
                        }
                    });
                    return;
                }
                FirebaseMessage message = listMessages.remove();
                setMessage(message.data);
                requestPost();

                if (responseMessage != null) Log.d("lucas", "response->" + responseMessage);
                if (callback == null) return;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFinish(message.id, responseCode, responseMessage);
                    }
                });
            }
        }).start();
    }

    private boolean isTokenValid() {
        return gToken != null && gToken.isInvalid();
    }

    private void getAccessToken() {
        if (requestingToken) return;
        requestingToken = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> l = new ArrayList<>();
                l.add("https://www.googleapis.com/auth/firebase.messaging");
                try {
                    // https://console.cloud.google.com/iam-admin/serviceaccounts/details/107170223301094435181/keys?authuser=0&hl=pt&project=tracker-4bec3
                    // https://console.firebase.google.com/project/mysamples-4f48d/settings/serviceaccounts/adminsdk
                    GoogleCredentials googleCredentials = GoogleCredentials.fromStream(context.getAssets().open("mysamples-4f48d-firebase-adminsdk-fbsvc-c83b3f6155.json")).createScoped(l);
                    googleCredentials.refresh();
                    AccessToken accessToken = googleCredentials.getAccessToken();
                    gToken = new GToken(accessToken.getTokenValue(), System.currentTimeMillis());
                    saveAccessToken(gToken);
                    setAccessToken(gToken.token);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                requestingToken = false;
                if (listMessages.isEmpty()) return;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        dequeue();
                    }
                });
            }
        }).start();
    }

    private void saveAccessToken(GToken token) {
        if (token.token == null) return;
        String data;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("token", token.token);
            jsonObject.put("timestamp", token.timestamp);
            data = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        writeFile(new File(context.getDataDir(), "files/access_token.json"), data);
    }

    private GToken readAccessToken() {
        String data = readFile(new File(context.getDataDir(), "files/access_token.json"));
        if (data == null) return null;
        try {
            JSONObject jsonObject = new JSONObject(data);
            long time = jsonObject.getLong("timestamp");
            String token = jsonObject.getString("token");
            return new GToken(token, time);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String readFile(File file) {
        if (file.isDirectory() || !file.exists()) return null;
        String everything = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }
            everything = builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return everything;
    }

    private void writeFile(File file, String content) {
        try {
            boolean exists = file.exists();
            if (!exists || file.isDirectory()) {
                File folder = file.getParentFile();
                if (!folder.exists() || folder.isFile()) folder.mkdirs();
                exists = file.createNewFile();
            }
            if (!exists) throw new IOException("Fail create a new file at: " + file.getPath());
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class GToken {
        public long timestamp;
        public String token;

        public GToken(String token, long timestamp) {
            this.token = token;
            this.timestamp = timestamp;
        }

        public boolean isInvalid() {
            return System.currentTimeMillis() - timestamp < 59 * 60 * 1000;
        }
    }

    private static class FirebaseMessage {
        public JSONObject data;
        public int id;

        public FirebaseMessage(int id, JSONObject data) {
            this.id = id;
            this.data = data;
        }
    }
}
