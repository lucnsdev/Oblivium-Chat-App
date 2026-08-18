package lucns.oblivium.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lucns.oblivium.R;
import lucns.oblivium.activities.CustomDialog;
import lucns.oblivium.data.User;
import lucns.oblivium.data.models.Invite;
import lucns.oblivium.services.firebase.FirebaseMessagingSender;
import lucns.oblivium.utils.Constants;
import lucns.oblivium.utils.Notify;
import lucns.oblivium.utils.Utils;

public class InvitationsAdapter extends ArrayAdapter<Invite> {

    private static final int TYPE_RECEIVED = 0;
    private static final int TYPE_SENT = 1;

    private final LayoutInflater inflater;
    private final List<Invite> list;
    private CustomDialog dialog;
    private String appName;
    private User user;

    public InvitationsAdapter(Context context) {
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
    public Invite getItem(int position) {
        return list.get(position);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        Invite invite = getItem(position);
        return invite.sent ? TYPE_SENT : TYPE_RECEIVED;
    }

    private void reorder() {
        if (list.size() < 2) return;
        list.sort(new Comparator<Invite>() {
            @Override
            public int compare(Invite o1, Invite o2) {
                return Long.compare(o2.timestamp, o1.timestamp);
            }
        });
    }

    public void addAll(List<Invite> invitations) {
        list.addAll(invitations);
        reorder();
        notifyDataSetChanged();
    }

    @Override
    public void add(Invite invite) {
        list.add(invite);
        reorder();
        notifyDataSetChanged();
    }

    public void remove(Invite invite) {
        list.remove(invite);
        reorder();
        notifyDataSetChanged();
    }

    public void remove(String username) {
        for (Invite i : list) {
            if (i.username.equals(username)) {
                remove(i);
                break;
            }
        }

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Invite invite = getItem(position);
        int viewType = getItemViewType(position);

        if (convertView == null) {
            if (viewType == TYPE_SENT) {
                convertView = inflater.inflate(R.layout.item_invite_sent, parent, false);
            } else {
                convertView = inflater.inflate(R.layout.item_invite_received, parent, false);
            }
        }

        if (invite.username.equals(appName)) convertView.findViewById(R.id.iconVerified).setVisibility(View.VISIBLE);
        TextView textUsername = convertView.findViewById(R.id.textUsername);
        TextView textDatetime = convertView.findViewById(R.id.textDatetime);

        textUsername.setText(invite.username);
        textDatetime.setText(Utils.retrieveTime(invite.timestamp));

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.buttonAccept) {
                    dialog.showDialogConfirmation(
                            getContext().getString(R.string.confirmation),
                            String.format(Locale.getDefault(), getContext().getString(R.string.accept_description), invite.username),
                            getContext().getString(R.string.accept),
                            getContext().getString(R.string.back),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                    if (v.getId() == R.id.buttonOne) {
                                        if (!Utils.hasInternetConnection()) {
                                            Notify.showToast(R.string.error_no_connection);
                                            return;
                                        }
                                        inviteAccept(invite.username);
                                    }
                                }
                            });
                } else if (id == R.id.buttonReject) {
                    dialog.showDialogConfirmation(
                            getContext().getString(R.string.confirmation),
                            String.format(Locale.getDefault(), getContext().getString(R.string.reject_description), invite.username),
                            getContext().getString(R.string.reject),
                            getContext().getString(R.string.back),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                    if (v.getId() == R.id.buttonOne) {
                                        if (!Utils.hasInternetConnection()) {
                                            Notify.showToast(R.string.error_no_connection);
                                            return;
                                        }
                                        inviteReject(invite.username);
                                    }
                                }
                            });
                } else if (id == R.id.buttonCancel) {
                    dialog.showDialogConfirmation(
                            getContext().getString(R.string.confirmation),
                            String.format(Locale.getDefault(), getContext().getString(R.string.cancel_description), invite.username),
                            getContext().getString(R.string.invite_cancel),
                            getContext().getString(R.string.back),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                    if (v.getId() == R.id.buttonOne) {
                                        if (!Utils.hasInternetConnection()) {
                                            Notify.showToast(R.string.error_no_connection);
                                            return;
                                        }
                                        inviteCancel(invite.username);
                                    }
                                }
                            });
                }
            }
        };

        if (viewType == TYPE_SENT) {
            Button buttonCancel = convertView.findViewById(R.id.buttonCancel);
            buttonCancel.setOnClickListener(onClickListener);
        } else {
            Button buttonAccept = convertView.findViewById(R.id.buttonAccept);
            Button buttonReject = convertView.findViewById(R.id.buttonReject);
            buttonAccept.setOnClickListener(onClickListener);
            buttonReject.setOnClickListener(onClickListener);
        }

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

    private void excludeInvite(String username) {
        Map<String, Object> map = new HashMap<>();
        map.put(username + "/invitations/" + user.getUsername(), null);
        map.put(user.getUsername() + "/invitations/" + username, null);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(appName).child(Constants.USERS);
        databaseReference.updateChildren(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        remove(username);
                        dialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        dialog.dismiss();
                        dialog.showDialogConnectionFailure();
                    }
                });
    }

    private void inviteAccept(String username) {
        dialog.showDialogWait(R.string.invite_accepting);
        Map<String, Object> map = new HashMap<>();
        map.put(username + "/invitations/" + user.getUsername(), null);
        map.put(user.getUsername() + "/invitations/" + username, null);
        map.put(username + "/contacts", user.getUsername());
        map.put(user.getUsername() + "/contacts", username);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(appName).child(Constants.USERS);
        databaseReference.updateChildren(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        remove(username);
                        dialog.dismiss();
                        sendNotification(username);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        dialog.dismiss();
                        dialog.showDialogConnectionFailure();
                    }
                });
    }

    private void inviteReject(String username) {
        dialog.showDialogWait(R.string.invite_rejecting);
        excludeInvite(username);
    }

    private void inviteCancel(String username) {
        dialog.showDialogWait(R.string.invite_canceling);
        excludeInvite(username);
    }

    private void sendNotification(String username) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(appName);
        databaseReference = databaseReference.child(Constants.USERS).child(username).child("fcm_register_id");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String id = dataSnapshot.getValue(String.class);
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(Constants.ACTION, Constants.ACTION_INVITE_ACCEPTED);
                    jsonObject.put(Constants.USERNAME, user.getUsername());
                    jsonObject.put(Constants.TIMESTAMP, String.valueOf(Instant.now().getEpochSecond()));
                    jsonObject.put(Constants.ID, id);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
                FirebaseMessagingSender sender = new FirebaseMessagingSender(getContext(), null);
                sender.setDestineRegisterId(id);
                sender.put(jsonObject);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}