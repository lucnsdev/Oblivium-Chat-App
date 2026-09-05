package lucns.oblivium.activities.fragments;

import android.app.Activity;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lucns.oblivium.R;
import lucns.oblivium.activities.CustomDialog;
import lucns.oblivium.activities.InviteActivity;
import lucns.oblivium.activities.LogoutActivity;
import lucns.oblivium.activities.MainActivity;
import lucns.oblivium.adapters.PersonsAdapter;
import lucns.oblivium.data.User;
import lucns.oblivium.data.models.Message;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.services.PersonsManager;
import lucns.oblivium.utils.Constants;
import lucns.oblivium.utils.TimeRegister;
import lucns.oblivium.utils.Utils;
import lucns.oblivium.views.slider.FragmentView;
import lucns.oblivium.views.HorizontalIndeterminateThreeBalls;

public class FragmentPersons extends FragmentView {

    private PersonsManager personsManager;
    private PopupMenu popupMenu;
    private CustomDialog dialog;
    private String appName;
    private RelativeLayout buttonInvite;
    private ListView listView;
    private TextView textTime;
    private HorizontalIndeterminateThreeBalls threeBalls;
    private PersonsAdapter listAdapter;

    public FragmentPersons(Activity activity, PersonsManager personsManager) {
        super(activity);
        this.personsManager = personsManager;
    }

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_persons);

        appName = getActivity().getString(R.string.app_name).toLowerCase();
        User user = User.getInstance();
        dialog = new CustomDialog(getActivity());
        threeBalls = findViewById(R.id.threeBalls);

        listAdapter = new PersonsAdapter(getActivity());
        buttonInvite = findViewById(R.id.buttonInvite);
        textTime = findViewById(R.id.textTime);
        listView = findViewById(R.id.listView);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Person person = listAdapter.getItem(position);
                ((MainActivity) getActivity()).goToConversation(person);
            }
        });
        personsManager.requestPersons();

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
        //findViewById(R.id.buttonBack).setOnClickListener(onClickListener);
        ImageButton buttonMenu = findViewById(R.id.buttonMenu);
        buttonMenu.setOnClickListener(onClickListener);
        ((TextView) findViewById(R.id.textUsername)).setText("@" + user.getUsername());
        if (user.getUsername().equals(appName)) findViewById(R.id.iconVerified).setVisibility(VISIBLE);

        ContextThemeWrapper darkWrapper = new ContextThemeWrapper(getActivity(), R.style.PopUpMenuTheme);
        popupMenu = new PopupMenu(darkWrapper, buttonMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_about) {
                    dialog.showDialogAbout();
                } else if (itemId == R.id.menu_logout) {
                    dialog.showDialogConfirmation(R.string.confirmation, R.string.sign_out, new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (v.getId() == R.id.buttonOne) {
                                new TimeRegister("fcm_registration").delete();
                                listView.setVisibility(INVISIBLE);
                                textTime.setText(R.string.no_contact);
                                buttonInvite.setVisibility(INVISIBLE);
                                listAdapter.removeAll();
                                personsManager.deleteAll();
                                if (!Utils.hasInternetConnection()) {
                                    user.logout();
                                    finish();
                                    return;
                                }
                                startActivity(new Intent(getActivity(), LogoutActivity.class));
                                finish();
                            }
                        }
                    });
                } else if (itemId == R.id.menu_invitations) {
                    startActivity(new Intent(getActivity(), InviteActivity.class));
                }
                return true;
            }
        });
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_contacts, popupMenu.getMenu());
        if (Utils.hasInternetConnection()) {
            if (!personsManager.hasPersons()) {
                buttonRetry.setVisibility(INVISIBLE);
                buttonInvite.setVisibility(INVISIBLE);
                threeBalls.setVisibility(VISIBLE);
            }
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
                            list.add(new Person(snapshot.getKey(), null, (Long) map.get("timestamp")));
                        }
                        personsManager.comparePersons(list.toArray(new Person[0]));
                    } else if (!personsManager.hasPersons()) {
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

    public void updatePerson(Person person) {
        listAdapter.update(person);
    }

    public void updatePersonByMessage(Message message) {
        for (int i = 0; i < listAdapter.getCount(); i++) {
            Person person = listAdapter.getItem(i);
            if (person.username.equals(message.username)) {
                person.lastMessage = message;
                listAdapter.update(person);
                break;
            }
        }
    }

    public void update() {
        Person[] persons = personsManager.getPersons();
        if (persons == null || persons.length == 0) {
            textTime.setText(R.string.no_contact);
            listView.setVisibility(INVISIBLE);
            threeBalls.setVisibility(INVISIBLE);
            buttonInvite.setVisibility(VISIBLE);
            return;
        }
        textTime.setText(String.format(Locale.getDefault(), getString(R.string.persons_count), persons.length, persons.length == 1 ? "" : "s"));
        listView.setVisibility(VISIBLE);
        buttonInvite.setVisibility(INVISIBLE);
        listAdapter.setAll(Arrays.asList(persons));
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

    @Override
    public boolean onBackPressed() {
        if (getActivity().isFinishing()) return true;
        getActivity().finish();
        return false;
    }
}
