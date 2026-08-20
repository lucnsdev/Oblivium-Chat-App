package lucns.oblivium.activities.fragments;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import lucns.oblivium.R;
import lucns.oblivium.activities.MainActivity;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.services.PersonsManager;
import lucns.oblivium.views.FragmentView;

public class FragmentConversation extends FragmentView {

    private PersonsManager personsManager;
    private TextView textUsername;
    private Person person;

    public FragmentConversation(Activity activity, PersonsManager personsManager) {
        super(activity);
        this.personsManager = personsManager;
    }

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_conversation);
        textUsername = findViewById(R.id.textUsername);
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
    }

    public void update() {

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
}
