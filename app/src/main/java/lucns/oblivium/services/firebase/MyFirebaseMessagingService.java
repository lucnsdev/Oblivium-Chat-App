package lucns.oblivium.services.firebase;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
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

import java.util.Locale;
import java.util.Map;

import lucns.oblivium.R;
import lucns.oblivium.activities.MainActivity;
import lucns.oblivium.data.User;
import lucns.oblivium.data.models.Message;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.services.AppStateRegister;
import lucns.oblivium.services.ConversationStorageManager;
import lucns.oblivium.services.NotificationProvider;
import lucns.oblivium.services.PersonsManager;
import lucns.oblivium.utils.Annotator;
import lucns.oblivium.utils.Constants;
import lucns.oblivium.utils.Notify;
import lucns.oblivium.utils.TimeRegister;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private NotificationProvider notification;
    private User user;
    private Handler handler;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d("Lucas", "onMessageReceived");
        Map<String, String> map = remoteMessage.getData();
        if (!user.hasCredentials()) {
            Log.i("FirebaseMessagingService", "Message received with User unbounded!");
            return;
        }
        if (map.isEmpty()) {
            Log.e("FirebaseMessagingService", "Map is empty");
        } else {
            String action = map.get(Constants.ACTION);
            String data = map.get(Constants.DATA);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    handleReceivedData(action, data);
                }
            });
        }
    }

    private void handleReceivedData(String action, String data) {
        try {
            JSONObject jsonData = new JSONObject(data);
            String username = jsonData.getString(Constants.USERNAME);
            Log.d("lucas", "action " + action);
            switch (action) {
                case Constants.ACTION_MESSAGE:
                    ConversationStorageManager storageManager = new ConversationStorageManager(this);
                    storageManager.setPerson(new Person(username));
                    storageManager.appendMessage(Message.fromString(data));
                    if (AppStateRegister.getInstance().getState()) {
                        Intent intent = new Intent(Constants.ACTION_MESSAGE);
                        intent.putExtra(Constants.USERNAME, username);
                        intent.putExtra(Constants.DATA, data);
                        sendBroadcast(intent);
                    } else {
                        notification.showAlert(String.format(Locale.getDefault(), getString(R.string.format_arrived_message), username), getString(R.string.arrived_message), null);
                    }
                    break;
                case Constants.ACTION_INVITE_ACCEPTED:
                    Person person = new Person(username, jsonData.getString(Constants.FCM_ID), jsonData.getLong(Constants.TIMESTAMP));
                    PersonsManager.getInstance(this).addPerson(person);
                    notification.showAlert(getString(R.string.invitation_accepted), "@" + username + " " + getString(R.string.invitation_accepted), null, null);
                    break;
                case Constants.ACTION_INVITE_RECEIVED:
                    notification.showAlert(getString(R.string.invitation_received), "@" + username + " " + getString(R.string.invitation_sent_you), null, null);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notification = new NotificationProvider(this, null);
        notification.setActivityClass(MainActivity.class);
        user = User.getInstance();
        handler = new Handler(Looper.getMainLooper());
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
            databaseReference.child(Constants.USERS).child(user.getUsername()).child(Constants.FCM_ID).setValue(installationId);
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