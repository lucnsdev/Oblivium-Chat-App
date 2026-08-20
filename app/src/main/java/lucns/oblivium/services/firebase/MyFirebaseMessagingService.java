package lucns.oblivium.services.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import lucns.oblivium.R;
import lucns.oblivium.activities.MainActivity;
import lucns.oblivium.data.User;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.services.NotificationProvider;
import lucns.oblivium.services.PersonsManager;
import lucns.oblivium.utils.Annotator;
import lucns.oblivium.utils.Constants;
import lucns.oblivium.utils.TimeRegister;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private NotificationProvider notification;
    private User user;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        //Log.d("Lucas", "onMessageReceived");
        Map<String, String> map = remoteMessage.getData();
        if (!user.hasCredentials()) {
            Log.i("FirebaseMessagingService", "Message received with User unbounded!");
            return;
        }
        if (!map.isEmpty()) {
            String username = map.get(Constants.USERNAME);
            String action = map.get(Constants.ACTION);
            Log.d("lucas", "action " + action);
            switch (action) {
                case Constants.ACTION_MESSAGE:
                    break;
                case Constants.ACTION_INVITE_ACCEPTED:
                    Person person = new Person(map.get("username"), map.get("fcm_register_id"), Long.valueOf(map.get("timestamp")));
                    PersonsManager.getInstance(this).addPerson(person);
                    notification.showAlert(getString(R.string.invitation_accepted), "@" + username + " " +  getString(R.string.invitation_accepted), null, null);
                    break;
                case Constants.ACTION_INVITE_RECEIVED:
                    notification.showAlert(getString(R.string.invitation_received), "@" + username + " " +  getString(R.string.invitation_sent_you), null, null);
                    break;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notification = new NotificationProvider(this, null);
        notification.setActivityClass(MainActivity.class);
        user = User.getInstance();
    }

    @Override
    public void onRegistered(@NonNull String installationId) {
        super.onRegistered(installationId);

        new TimeRegister("fcm_registration").setLastUpdate();
        User user = User.getInstance();
        boolean hasLogin = user.hasCredentials();
        user.setRegisterId(installationId);
        user.save();

        if (hasLogin) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(getString(R.string.app_name).toLowerCase());
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    user.removePending();
                    user.save();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
            databaseReference.child(Constants.USERS).child(user.getUsername()).child("fcm_register_id").setValue(installationId);
        }

        /*
        FirebaseMessaging.getInstance().register().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("FCM", "Registered via FID");
            }
        });


        FirebaseInstallations.getInstance().getId().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                showDialogBadConnection();
                return;
            }
        });
         */
    }
}