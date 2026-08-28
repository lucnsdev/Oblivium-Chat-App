package lucns.oblivium.activities;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.installations.FirebaseInstallations;

import java.util.HashMap;
import java.util.Map;

import lucns.oblivium.R;
import lucns.oblivium.activities.fragments.FragmentConversation;
import lucns.oblivium.activities.fragments.FragmentPersons;
import lucns.oblivium.data.User;
import lucns.oblivium.data.models.Person;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        user = User.getInstance();

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

        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(callback);
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
        checkRegistrationId();
    }

    private void checkRegistrationId() {
        if (!Utils.hasInternetConnection()) {
            Notify.showToast(R.string.error_no_connection);
            return;
        }
        if (new TimeRegister("fcm_registration").isOverTime(60 * 24 * 7)) {
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
}
