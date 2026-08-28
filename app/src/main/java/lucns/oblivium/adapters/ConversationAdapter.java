package lucns.oblivium.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import lucns.oblivium.R;
import lucns.oblivium.activities.CustomDialog;
import lucns.oblivium.data.models.Message;

public class ConversationAdapter extends ArrayAdapter<Message> {

    private final LayoutInflater inflater;
    private final List<Message> list;
    private CustomDialog dialog;

    public ConversationAdapter(Context context) {
        super(context, 0);
        this.inflater = LayoutInflater.from(context);
        this.list = new ArrayList<>();
        this.dialog = new CustomDialog((Activity) context);
    }

    private void reorder() {
        if (list.size() < 2) return;
        list.sort(new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                return Long.compare(o2.timestamp, o1.timestamp);
            }
        });
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Message getItem(int position) {
        return list.get(position);
    }

    public void removeAll() {
        list.clear();
        notifyDataSetChanged();
    }

    public void setAll(Message[] messages) {
        list.clear();
        addAll(Arrays.asList(messages));
    }

    public void addAll(List<Message> messages) {
        list.addAll(messages);
        reorder();
        notifyDataSetChanged();
    }

    @Override
    public void add(Message message) {
        list.add(message);
        reorder();
        notifyDataSetChanged();
    }

    public void remove(Message invite) {
        list.remove(invite);
        reorder();
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Message message = list.get(position);
        convertView = inflater.inflate(R.layout.item_person, parent, false);

        return convertView;
    }
}
