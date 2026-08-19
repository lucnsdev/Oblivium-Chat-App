package lucns.oblivium.activities.fragments;

import android.app.Activity;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lucns.oblivium.R;
import lucns.oblivium.activities.CustomDialog;
import lucns.oblivium.activities.InviteActivity;
import lucns.oblivium.activities.LogoutActivity;
import lucns.oblivium.data.User;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.services.PersonsManager;
import lucns.oblivium.utils.Constants;
import lucns.oblivium.utils.Utils;
import lucns.oblivium.views.FragmentView;
import lucns.oblivium.views.HorizontalIndeterminateThreeBalls;

public class FragmentPersons extends FragmentView {

    private PersonsManager personsManager;
    private PopupMenu popupMenu;
    private CustomDialog dialog;

    public FragmentPersons(Activity activity, PersonsManager personsManager) {
        super(activity);
        this.personsManager = personsManager;
    }

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_persons);

        User user = User.getInstance();
        dialog = new CustomDialog(getActivity());
        HorizontalIndeterminateThreeBalls threeBalls = findViewById(R.id.threeBalls);

        RelativeLayout buttonInvite = findViewById(R.id.buttonInvite);
        Button buttonRetry = findViewById(R.id.buttonRetry);
        View.OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.buttonInvite) {
                    Intent intent = new Intent(getActivity(), InviteActivity.class);
                    intent.setAction("from_button");
                    startActivity(intent);
                } else if (v.getId() == R.id.buttonBack) {
                    finish();
                } else if (v.getId() == R.id.buttonMenu) {
                    popupMenu.show();
                }
            }
        };
        buttonInvite.setOnClickListener(onClickListener);
        buttonRetry.setOnClickListener(onClickListener);
        findViewById(R.id.buttonBack).setOnClickListener(onClickListener);
        ImageButton buttonMenu = findViewById(R.id.buttonMenu);
        buttonMenu.setOnClickListener(onClickListener);

        ContextThemeWrapper darkWrapper = new ContextThemeWrapper(getActivity(), R.style.PopUpMenuTheme);
        popupMenu = new PopupMenu(darkWrapper, buttonMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_about) {
                    dialog.showDialogAbout();
                } else if (itemId == R.id.menu_logout) {
                    startActivity(new Intent(getActivity(), LogoutActivity.class));
                    finish();
                } else if (itemId == R.id.menu_invitations) {
                    startActivity(new Intent(getActivity(), InviteActivity.class));
                }
                return true;
            }
        });
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_contacts, popupMenu.getMenu());
        if (Utils.hasInternetConnection()) {
            if (!personsManager.hasPersons()) threeBalls.setVisibility(VISIBLE);
            DatabaseReference database = FirebaseDatabase.getInstance().getReference().child(getString(R.string.app_name).toLowerCase());
            DatabaseReference userRef = database.child(Constants.USERS).child(user.getUsername()).child("persons");
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Iterable<DataSnapshot> iterable = dataSnapshot.getChildren();
                        List<Person> list = new ArrayList<>();
                        for (DataSnapshot snapshot : iterable) {
                            Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                            list.add(new Person((String) map.get("username"), (String) map.get("fcm_register_id"), (Long) map.get("timestamp")));
                        }
                        personsManager.comparePersons(list.toArray(new Person[0]));
                    } else {
                        buttonInvite.setVisibility(VISIBLE);
                    }
                    threeBalls.setVisibility(INVISIBLE);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    threeBalls.setVisibility(INVISIBLE);
                    buttonRetry.setVisibility(VISIBLE);
                }
            });
        }
    }

    public void update() {
        Person[] persons = personsManager.getPersons();

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {
        popupMenu.dismiss();
    }

    @Override
    public void onDestroy() {

    }
}
