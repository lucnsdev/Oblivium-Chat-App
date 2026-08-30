package lucns.oblivium.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import lucns.oblivium.R;
import lucns.oblivium.activities.CustomDialog;
import lucns.oblivium.data.User;
import lucns.oblivium.data.models.Message;

public class ConversationAdapter extends ArrayAdapter<Message> {

    private User user;
    private final LayoutInflater inflater;
    private final List<Message> list;
    private CustomDialog dialog;
    private View[] views;

    public ConversationAdapter(Context context) {
        super(context, 0);
        this.user = User.getInstance();
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
        views = new View[list.size()];
        reorder();
        notifyDataSetChanged();
    }

    @Override
    public void add(Message message) {
        list.add(message);
        views = new View[list.size()];
        reorder();
        notifyDataSetChanged();
    }

    public void remove(Message message) {
        list.remove(message);
        views = new View[list.size()];
        reorder();
        notifyDataSetChanged();
    }

    public void update(Message message) {
        for (int position = 0; position < list.size(); position++) {
            if (message.timestamp == list.get(position).timestamp) {
                list.add(position, message);
                update(message, position);
                break;
            }
        }
    }

    private void update(Message message, int index) {
        boolean i = message.username.equals(user.getUsername());
        if (views[index] == null) {
            views[index] = inflater.inflate(i ? R.layout.item_message_right : R.layout.item_message_left, null, false);
        }
        if (i) {
            ((ImageView) views[index].findViewById(R.id.iconStatus)).setImageResource(message.text.sent ? R.drawable.icon_double_check : R.drawable.icon_clock);
        }
        ((TextView) views[index].findViewById(R.id.textMessage)).setText(message.text.content);
        ((TextView) views[index].findViewById(R.id.textDateTime)).setText(getDateTime(message.timestamp));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Message message = list.get(position);
        update(message);
        return views[position];
    }

    private String getDateTime(long timestamp) {
        long difference = System.currentTimeMillis() - timestamp;
        if (difference < 60) return getString(R.string.few_seconds);
        else if (difference < 3600) return String.format(Locale.getDefault(), getString(R.string.format_minutes), difference / 60, difference / 60 == 1 ? "" : "s");
        else if (difference < 86400) return String.format(Locale.getDefault(), getString(R.string.format_hours), difference / 3600, difference / 3600 == 1 ? "" : "s");
        else return String.format(Locale.getDefault(), getString(R.string.format_days), difference / 86400, difference / 86400 == 1 ? "" : "s");
    }

    private String getString(int res) {
        return getContext().getString(res);
    }
}
