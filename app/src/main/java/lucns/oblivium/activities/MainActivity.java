package lucns.oblivium.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.installations.FirebaseInstallations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lucns.oblivium.R;
import lucns.oblivium.activities.fragments.FragmentConversation;
import lucns.oblivium.activities.fragments.FragmentPersons;
import lucns.oblivium.data.User;
import lucns.oblivium.data.models.Message;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.services.AppStateRegister;
import lucns.oblivium.services.PersonsManager;
import lucns.oblivium.utils.Constants;
import lucns.oblivium.utils.Notify;
import lucns.oblivium.utils.TimeRegister;
import lucns.oblivium.utils.Utils;
import lucns.oblivium.views.SliderView;

public class MainActivity extends Activity {

    private User user;
    private FragmentConversation fragmentConversation;
    private SliderView sliderView;
    private AppStateRegister stateRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        user = User.getInstance();
        stateRegister = AppStateRegister.getInstance();

        PersonsManager personsManager = PersonsManager.getInstance(this);
        fragmentConversation = new FragmentConversation(this);
        FragmentPersons fragmentPersons = new FragmentPersons(this, personsManager);
        personsManager.setCallback(new PersonsManager.Callback() {
            @Override
            public void onPersonsAvailable() {
                fragmentPersons.update();
            }
        });
        sliderView = findViewById(R.id.sliderView);
        sliderView.disableScroll(true);
        sliderView.addFragment(fragmentPersons);
        sliderView.addFragment(fragmentConversation);

        registerReceiver(messagesReceiver, new IntentFilter(Constants.ACTION_MESSAGE), Context.RECEIVER_NOT_EXPORTED);
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
    }

    public void updatePersonItem(Person person) {
        // update last message in list item
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(callback);
        unregisterReceiver(messagesReceiver);
    }

    private final OnBackInvokedCallback callback = new OnBackInvokedCallback() {

        @Override
        public void onBackInvoked() {
            sliderView.onBackPressed();
        }
    };

    public void goToPersons() {
        sliderView.goToIndex(0);
    }

    public void goToConversation(Person person) {
        fragmentConversation.setPerson(person);
        sliderView.goToIndex(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        stateRegister.setState(true);
        checkRegistrationId();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stateRegister.setState(false);
    }

    private void checkRegistrationId() {
        if (!Utils.hasInternetConnection()) {
            Notify.showToast(R.string.error_no_connection);
            return;
        }
        if (user.hasCredentials() && new TimeRegister("fcm_registration").isOverTime(60 * 24 * 7)) {
            FirebaseInstallations.getInstance().getId().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) {
                    return;
                }
                String registerId = task.getResult();
                user.setRegisterId(registerId);
                user.save();
                sendUserDataToServer();
            });
            return;
        }
        sendUserDataToServer();
    }

    private void sendUserDataToServer() {
        Map<String, Object> map = new HashMap<>();
        map.put("access_timestamp", System.currentTimeMillis());
        if (user.isPendingShipment()) map.put("fcm_register_id", user.getRegisterId());

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
        databaseReference.child(Constants.USERS).child(user.getUsername()).updateChildren(map);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED) {
            Notify.showToast(R.string.canceled);
            return;
        }
        Uri uri = data.getData();
        fragmentConversation.onFilePicked(uri);
    }

    private final BroadcastReceiver messagesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message message = Message.fromString(intent.getStringExtra(Constants.DATA));
            Person person = fragmentConversation.getPerson();
            if (person.username.equals(message.username)) {
                fragmentConversation.putMessage(message);
            } else {
                Notify.showToast(String.format(Locale.getDefault(), getString(R.string.format_toast_message), person.username, message.text.content));
            }
        }
    };
}
