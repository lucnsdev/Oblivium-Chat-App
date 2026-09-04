package lucns.oblivium.data.models;

import org.json.JSONException;
import org.json.JSONObject;

import lucns.oblivium.utils.Constants;

public class Message {

    public static final String DELIMITER = "=";

    public String username;
    public Text text;
    public File file;
    public long timestamp;

    public Message(String username, String text) {
        this.username = username;
        this.text = new Text(text);
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String username, long timestamp) {
        this.username = username;
        this.timestamp = timestamp;
    }

    public static Message fromJSONObject(JSONObject jsonObject) {
        Message message = null;
        try {
            message = new Message(jsonObject.getString("username"), jsonObject.getLong("timestamp"));
            JSONObject jsonText = jsonObject.getJSONObject("text");
            message.text = new Text(jsonText.getString("text"));
            message.text.sent = jsonText.getBoolean("sent");
            JSONObject jsonFile = jsonObject.optJSONObject("file");
            if (jsonFile != null) {
                message.file = new File(jsonFile.optString("path"));
                message.file.sent = jsonFile.getBoolean("sent");
                message.file.url = jsonFile.optString("url");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return message;
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", username);
            jsonObject.put("timestamp", timestamp);
            JSONObject jsonText = new JSONObject();
            jsonText.put("content", text.content);
            jsonText.put("sent", text.sent);
            jsonObject.put("text", jsonText);
            if (file != null) {
                JSONObject jsonFile = new JSONObject();
                jsonFile.put("path", file.path);
                jsonFile.put("url", file.url);
                jsonFile.put("sent", file.sent);
                jsonObject.put("file", jsonFile);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject toSend() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(Constants.ACTION, Constants.ACTION_MESSAGE);
            jsonObject.put(Constants.DATA, toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Message fromString(String json) {
        try {
            return fromJSONObject(new JSONObject(json));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return toJSONObject().toString();
    }

    public static class File {

        public String path, url;
        public boolean sent;

        public File(String path) {
            this.path = path;
        }
    }

    public static class Text {

        public String content;
        public boolean sent;

        public Text(String content) {
            this.content = content;
        }
    }
}
