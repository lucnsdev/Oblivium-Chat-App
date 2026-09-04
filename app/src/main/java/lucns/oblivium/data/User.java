package lucns.oblivium.data;

import org.json.JSONException;
import org.json.JSONObject;

import lucns.oblivium.utils.Annotator;
import lucns.oblivium.utils.Constants;

public class User {

    private String username, registerId;
    private long loginTimestamp;
    private boolean isPendingShipment;
    private boolean signed;

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
        return signed && registerId != null;
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

    public void setSigned() {
        signed = true;
    }

    public void save() {
        if (username == null) return;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", username);
            jsonObject.put(Constants.FCM_ID, registerId);
            jsonObject.put("login_timestamp", loginTimestamp);
            jsonObject.put("is_pending_shipment", isPendingShipment);
            jsonObject.put("signed", signed);
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
            registerId = jsonObject.getString(Constants.FCM_ID);
            loginTimestamp = jsonObject.getLong("login_timestamp");
            isPendingShipment = jsonObject.getBoolean("is_pending_shipment");
            signed = jsonObject.getBoolean("signed");
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
