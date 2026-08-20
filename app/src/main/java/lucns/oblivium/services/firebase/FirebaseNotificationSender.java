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
        databaseReference = databaseReference.child(Constants.USERS).child(username).child("fcm_register_id");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String id = dataSnapshot.getValue(String.class);
                Person person = new Person(username, id, System.currentTimeMillis());
                PersonsManager.getInstance(context).addPerson(person);

                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(Constants.ACTION, action);
                    jsonObject.put(Constants.USERNAME, user.getUsername());
                    jsonObject.put(Constants.TIMESTAMP, String.valueOf(System.currentTimeMillis()));
                    jsonObject.put(Constants.ID, id);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
                FirebaseMessagingSender sender = new FirebaseMessagingSender(context, null);
                sender.setDestineRegisterId(id);
                sender.put(jsonObject);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}
