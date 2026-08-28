package lucns.oblivium.activities.fragments;

import android.app.Activity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import lucns.oblivium.R;
import lucns.oblivium.activities.MainActivity;
import lucns.oblivium.adapters.ConversationAdapter;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.services.ConversationStorageManager;
import lucns.oblivium.services.PacketSenderManager;
import lucns.oblivium.services.PersonsManager;
import lucns.oblivium.utils.Constants;
import lucns.oblivium.utils.Utils;
import lucns.oblivium.views.FragmentView;
import lucns.oblivium.views.HorizontalIndeterminateThreeBalls;
import lucns.oblivium.views.IndeterminateThreeBalls;

public class FragmentConversation extends FragmentView {
    private TextView textUsername;
    private Person person;
    private ConversationStorageManager conversationStorageManager;
    private PacketSenderManager packetSenderManager;
    private final String appName;
    private ListView listView;
    private TextView textEmpty;
    private EditText editText;
    private LinearLayout rootEditText;
    private ConversationAdapter listAdapter;
    private HorizontalIndeterminateThreeBalls threeBalls;
    private IdCatcher idCatcher;

    public FragmentConversation(Activity activity) {
        super(activity);
        this.listAdapter = new ConversationAdapter(activity);
        this.conversationStorageManager = new ConversationStorageManager(activity);
        this.conversationStorageManager.setCallback(new ConversationStorageManager.Callback() {
            @Override
            public void onConversationAvailable() {
                if (person.conversation == null || person.conversation.length == 0) {
                    threeBalls.setVisibility(INVISIBLE);
                    listView.setVisibility(INVISIBLE);
                    textEmpty.setVisibility(VISIBLE);
                    return;
                }
                textEmpty.setVisibility(INVISIBLE);
                threeBalls.setVisibility(INVISIBLE);
                listAdapter.setAll(person.conversation);
                listView.setVisibility(VISIBLE);
            }
        });
        this.appName = activity.getString(R.string.app_name).toLowerCase();

        setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {

            float lastY;

            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int keyboardHeight = insets.getInsets(WindowInsets.Type.ime()).bottom;
                if (keyboardHeight > 0) {
                    lastY = rootEditText.getY();
                    rootEditText.setY(lastY - keyboardHeight);
                } else {
                    rootEditText.setY(lastY);
                }
                return insets;
            }
        });
    }

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_conversation);
        textUsername = findViewById(R.id.textUsername);
        textEmpty = findViewById(R.id.textEmpty);
        listView = findViewById(R.id.listView);
        listView.setAdapter(listAdapter);
        threeBalls = findViewById(R.id.threeBalls);
        editText = findViewById(R.id.editText);
        rootEditText = findViewById(R.id.rootEditText);

        View.OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.buttonBack) {
                    ((MainActivity) getActivity()).goToPersons();
                }
            }
        };
        findViewById(R.id.buttonBack).setOnClickListener(onClickListener);
    }

    public void setPerson(Person person) {
        this.person = person;
        if (person.username.equals(getString(R.string.app_name).toLowerCase())) findViewById(R.id.iconVerified).setVisibility(VISIBLE);
        textUsername.setText("@" + person.username);
        threeBalls.setVisibility(VISIBLE);
        listView.setVisibility(INVISIBLE);
        if (idCatcher != null) idCatcher.cancel();
        if (Utils.hasInternetConnection()) {
            idCatcher = new IdCatcher(person);
            idCatcher.request();
        }
        conversationStorageManager.setPerson(person);
        conversationStorageManager.requestConversation();
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onDestroy() {

    }

    private class IdCatcher {

        private final Person p;
        private boolean canceled;

        protected IdCatcher(Person person) {
            this.p = person;
        }

        protected void cancel() {
            canceled = true;
        }

        protected void request() {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(appName);
            databaseReference = databaseReference.child(Constants.USERS).child(p.username).child("fcm_register_id");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String id = dataSnapshot.getValue(String.class);
                    if (id != null && id.equals(p.registerId)) {
                        PersonsManager.getInstance(getActivity()).writePerson(p);
                        p.registerId = id;
                    }
                    if (canceled) return;

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }

    @Override
    public boolean onBackPressed() {
        listAdapter.removeAll();
        listView.setVisibility(INVISIBLE);
        ((MainActivity) getActivity()).goToPersons();
        return false;
    }
}
