package lucns.oblivium.data;

import org.json.JSONException;
import org.json.JSONObject;

import lucns.oblivium.utils.Annotator;

public class User {

    private String username, registerId;
    private long loginTimestamp;
    private boolean isPendingShipment;

    private static User instance;

    public static User getInstance() {
        if (instance == null) {
            synchronized (User.class) {
                instance = new User();
                instance.load();
            }
        }
        return instance;
    }

    protected User() {}

    public boolean hasCredentials() {
        return registerId != null;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public String getRegisterId() {
        return registerId;
    }

    public void setRegisterId(String registerId) {
        this.registerId = registerId;
    }

    public void removePending() {
        isPendingShipment = false;
    }

    public boolean isPendingShipment() {
        return isPendingShipment;
    }

    public long getLoginTimestamp() {
        return loginTimestamp;
    }

    public void setLoginTimestamp(long loginTimestamp) {
        this.loginTimestamp = loginTimestamp;
    }

    public void save() {
        if (username == null) return;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", username);
            jsonObject.put("fcm_register_id", registerId);
            jsonObject.put("login_timestamp", loginTimestamp);
            jsonObject.put("is_pending_shipment", isPendingShipment);
            new Annotator("user", "User.json").setContent(jsonObject.toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        Annotator annotator = new Annotator("user", "user.json");
        if (!annotator.exists()) return;
        try {
            JSONObject jsonObject = new JSONObject(annotator.getContent());
            username = jsonObject.getString("username");
            registerId = jsonObject.getString("fcm_register_id");
            loginTimestamp = jsonObject.getLong("login_timestamp");
            isPendingShipment = jsonObject.getBoolean("is_pending_shipment");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void logout() {

        new Annotator("user", "user.json").delete();
        registerId = null;
        isPendingShipment = false;

    }
}
