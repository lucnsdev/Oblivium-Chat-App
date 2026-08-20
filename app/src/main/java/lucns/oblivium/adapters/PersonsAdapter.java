package lucns.oblivium.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import lucns.oblivium.R;
import lucns.oblivium.activities.CustomDialog;
import lucns.oblivium.data.User;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.utils.Utils;

public class PersonsAdapter extends ArrayAdapter<Person> {

    private final LayoutInflater inflater;
    private final List<Person> list;
    private CustomDialog dialog;
    private String appName;
    private User user;

    public PersonsAdapter(Context context) {
        super(context, 0);
        this.inflater = LayoutInflater.from(context);
        this.list = new ArrayList<>();
        this.dialog = new CustomDialog((Activity) context);
        this.appName = context.getString(R.string.app_name).toLowerCase();
        this.user = User.getInstance();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Person getItem(int position) {
        return list.get(position);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    private void reorder() {
        if (list.size() < 2) return;
        list.sort(new Comparator<Person>() {
            @Override
            public int compare(Person o1, Person o2) {
                return Long.compare(o2.timestamp, o1.timestamp);
            }
        });
    }

    public void removeAll() {
        list.clear();
        notifyDataSetChanged();
    }

    public void setAll(List<Person> invitations) {
        list.clear();
        addAll(invitations);
    }

    public void addAll(List<Person> invitations) {
        list.addAll(invitations);
        reorder();
        notifyDataSetChanged();
    }

    @Override
    public void add(Person invite) {
        list.add(invite);
        reorder();
        notifyDataSetChanged();
    }

    public void remove(Person invite) {
        list.remove(invite);
        reorder();
        notifyDataSetChanged();
    }

    public void remove(String username) {
        for (Person i : list) {
            if (i.username.equals(username)) {
                remove(i);
                break;
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Person person = list.get(position);
        convertView = inflater.inflate(R.layout.item_person, parent, false);

        if (person.username.equals(appName)) convertView.findViewById(R.id.iconVerified).setVisibility(View.VISIBLE);
        TextView textUsername = convertView.findViewById(R.id.textUsername);
        TextView textDatetime = convertView.findViewById(R.id.textDatetime);

        textUsername.setText(person.username);
        textDatetime.setText(Utils.retrieveTime(person.timestamp));

        if (getCount() == 1) {
            convertView.setBackgroundResource(R.drawable.item_rounded_background);
        } else if (position == getCount() - 1) {
            convertView.setBackgroundResource(R.drawable.item_background_end);
        } else if (position == 0) {
            convertView.setBackgroundResource(R.drawable.item_background_start);
        } else {
            convertView.setBackgroundResource(R.drawable.item_background);
        }
        return convertView;
    }
}
