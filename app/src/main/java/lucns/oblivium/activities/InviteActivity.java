package lucns.oblivium.activities;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lucns.oblivium.R;
import lucns.oblivium.adapters.InvitationsAdapter;
import lucns.oblivium.animations.ViewChangerController;
import lucns.oblivium.data.User;
import lucns.oblivium.data.models.Invite;
import lucns.oblivium.utils.Constants;
import lucns.oblivium.utils.Notify;
import lucns.oblivium.utils.Utils;

public class InviteActivity extends Activity {

    private ViewChangerController viewChangerController;
    private CustomDialog dialog;
    private RelativeLayout rootContent, rootProgress;
    private Button buttonRetry;
    private ListView listVew;
    private TextView textStatus;
    private User user;
    private InvitationsAdapter listAdapter;
    private String appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_invite);

        appName = getString(R.string.app_name).toLowerCase();
        user = User.getInstance();
        rootProgress = findViewById(R.id.rootProgress);
        rootContent = findViewById(R.id.rootContent);
        buttonRetry = findViewById(R.id.buttonRetry);
        listVew = findViewById(R.id.listView);
        textStatus = findViewById(R.id.textStatus);
        viewChangerController = new ViewChangerController(rootContent, rootProgress);
        listAdapter = new InvitationsAdapter(this);
        listVew.setAdapter(listAdapter);

        dialog = new CustomDialog(this);
        if (getIntent().getAction() != null) showDialogInvite(null);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.buttonBack) {
                    finish();
                } else if (v.getId() == R.id.fab) {
                    showDialogInvite(null);
                }
                if (v.getId() == R.id.buttonRetry) {
                    loadInvitations();
                }
            }
        };
        findViewById(R.id.buttonBack).setOnClickListener(onClickListener);
        findViewById(R.id.fab).setOnClickListener(onClickListener);

        if (Utils.hasInternetConnection()) {
            loadInvitations();
        } else {
            textStatus.setText(R.string.error_no_connection);
            textStatus.setVisibility(View.VISIBLE);
        }
    }

    private void loadInvitations() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(getString(R.string.app_name).toLowerCase());
        databaseReference = databaseReference.child(Constants.USERS).child(user.getUsername()).child("invitations");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<Invite> invitations = new ArrayList<>();
                    for (DataSnapshot userData : dataSnapshot.getChildren()) {
                        String username = userData.getKey();
                        Long timestampVal = userData.child("timestamp").getValue(Long.class);
                        Boolean sentVal = userData.child("sent").getValue(Boolean.class);
                        long timestamp = timestampVal != null ? timestampVal : 0L;
                        boolean sent = Boolean.TRUE.equals(sentVal);
                        invitations.add(new Invite(username, timestamp, sent));
                    }
                    if (invitations.isEmpty()) {
                        textStatus.setText(R.string.no_invitations);
                        textStatus.setVisibility(View.VISIBLE);
                        return;
                    }
                    textStatus.setVisibility(View.INVISIBLE);
                    listVew.setVisibility(View.VISIBLE);
                    listAdapter.addAll(invitations);
                } else {
                    textStatus.setText(R.string.no_invitations);
                    textStatus.setVisibility(View.VISIBLE);
                    viewChangerController.change(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                viewChangerController.change(false);
                dialog.showDialogConnectionFailure();
            }
        });
    }

    private void showDialogInvite(String initialText) {
        String[] username = new String[1];
        dialog.showDialogInvite(initialText, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.buttonTwo) {
                    dialog.dismiss();
                    return;
                }
                if (username[0] == null || username[0].isEmpty() || username[0].length() < 5 || username[0].equals(user.getUsername())) {
                    Notify.showToast(R.string.error_invalid_username);
                    return;
                }
                dialog.dismiss();
                dialog.showDialogWait(R.string.inviting);
                if (username[0].startsWith("@")) username[0] = username[0].substring(1);

                if (!Utils.hasInternetConnection()) {
                    Notify.showToast(R.string.error_no_connection);
                    return;
                }
                inviteFriend(username[0]);
            }
        }, new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                username[0] = s.toString();
            }
        });
    }

    private void inviteFriend(String username) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(getString(R.string.app_name).toLowerCase());
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    sendInvitations(username);
                } else {
                    dialog.dismiss();
                    dialog.showDialogConsent(R.string.error_user_not_exists, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            showDialogInvite("@" + username);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                dialog.dismiss();
                dialog.showDialogConnectionFailure();
            }
        });
    }

    private void sendInvitations(String username) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> map = new HashMap<>();
        map.put(username + "/invitations/" + user.getUsername() + "/timestamp", now);
        map.put(username + "/invitations/" + user.getUsername() + "/sent", false);
        map.put(user.getUsername() + "/invitations/" + username + "/timestamp", now);
        map.put(user.getUsername() + "/invitations/" + username + "/sent", true);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(appName).child(Constants.USERS);
        databaseReference.updateChildren(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        listAdapter.add(new Invite(username, now, true));
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

    @Override
    protected void onPause() {
        super.onPause();
        dialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
