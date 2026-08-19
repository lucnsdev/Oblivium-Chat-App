package lucns.oblivium.activities.fragments;

import android.app.Activity;
import android.widget.TextView;

import lucns.oblivium.R;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.services.PersonsManager;
import lucns.oblivium.views.FragmentView;

public class FragmentConversation extends FragmentView {

    private PersonsManager personsManager;
    private TextView textPerson;
    private Person person;

    public FragmentConversation(Activity activity, PersonsManager personsManager) {
        super(activity);
        this.personsManager = personsManager;
    }

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_conversation);
        textPerson = findViewById(R.id.textPerson);
    }

    public void setPerson(Person person) {
        this.person = person;
        textPerson.setText(person.username);
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
