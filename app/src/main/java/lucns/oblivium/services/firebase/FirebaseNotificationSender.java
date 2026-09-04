package lucns.oblivium.services.firebase;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import lucns.oblivium.R;
import lucns.oblivium.data.User;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.services.PersonsManager;
import lucns.oblivium.utils.Constants;

public class FirebaseNotificationSender {

    private Context context;
    private String appName;
    private User user;

    public FirebaseNotificationSender(Context context) {
        this.context = context;
        this.appName = context.getString(R.string.app_name).toLowerCase();
        this.user = User.getInstance();
    }

    public void sendNotification(String username, String action) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(appName);
        databaseReference = databaseReference.child(Constants.USERS).child(username).child(Constants.FCM_ID);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String fcmId = dataSnapshot.getValue(String.class);
                JSONObject jsonObject = new JSONObject();
                try {
                    JSONObject jsonData = new JSONObject();
                    jsonData.put(Constants.USERNAME, user.getUsername());
                    jsonData.put(Constants.TIMESTAMP, System.currentTimeMillis());
                    jsonData.put(Constants.FCM_ID, fcmId);
                    jsonObject.put(Constants.ACTION, action);
                    jsonObject.put(Constants.DATA, jsonData.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
                FirebaseMessagingSender sender = new FirebaseMessagingSender(context, null);
                sender.setDestineRegisterId(fcmId);
                sender.put(jsonObject);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}
