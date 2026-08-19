package lucns.oblivium.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.installations.FirebaseInstallations;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lucns.oblivium.R;
import lucns.oblivium.animations.ViewChangerController;
import lucns.oblivium.data.User;
import lucns.oblivium.utils.Constants;
import lucns.oblivium.utils.TimeRegister;

public class LoginActivity extends Activity {

    private ViewChangerController viewChangerController;
    private Button button;
    private RelativeLayout rootForm, rootProgress;
    private CustomDialog dialog;
    private TextView textTitle;
    private EditText editText;
    private DatabaseReference database;
    private String username, password, remotePassword;
    private User user;
    private TextView textRecoverPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_login);

        user = User.getInstance();
        dialog = new CustomDialog(this);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            ((TextView) findViewById(R.id.textVersion)).setText(String.format(Locale.getDefault(), getString(R.string.format_version), versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        database = FirebaseDatabase.getInstance().getReference();

        rootForm = findViewById(R.id.rootForm);
        rootProgress = findViewById(R.id.rootProgress);
        viewChangerController = new ViewChangerController(rootForm, rootProgress);

        textTitle = findViewById(R.id.textTitle);
        textTitle.setText(R.string.enter_username);
        textRecoverPassword = findViewById(R.id.textRecoverPassword);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText.setEnabled(false);
                button.setEnabled(false);
                changeViews(true);
            }
        });

        editText = findViewById(R.id.editText);
        editText.setHint(R.string.username);
        if (user.getUsername() != null) {
            editText.setText("@" + user.getUsername());
            button.setEnabled(true);
        }
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                button.setEnabled(editText.getText().length() > 4);
            }
        });

        checkDarkList();
    }

    private void changeViews(boolean showB) {
        viewChangerController.change(showB, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                checkLogin();
            }
        });
    }

    private void checkLogin() {
        if (editText.getText().length() > 4) {
            if (username == null) {
                username = editText.getText().toString();
                if (username.startsWith("@")) username = username.substring(1);
                getRemotePassword();
            } else if (password == null) {
                password = editText.getText().toString();
                if (remotePassword == null) {
                    putUserData();
                    return;
                }
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (password.equals(remotePassword)) {
                            putUserData();
                        } else {
                            changeViews(false);
                            dialog.showWrongPasswordDialog(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                    editText.setEnabled(true);
                                    if (v.getId() == R.id.buttonTwo) {
                                        textTitle.setText(R.string.enter_username);
                                        textRecoverPassword.setVisibility(View.INVISIBLE);
                                        editText.setHint(R.string.username);
                                        editText.setText("@" + username);
                                        editText.setInputType(InputType.TYPE_CLASS_TEXT);
                                        username = null;
                                        password = null;
                                        remotePassword = null;
                                    } else {
                                        password = null;
                                        button.setEnabled(true);
                                    }
                                    openKeyboard();
                                }
                            });
                        }
                    }
                }, 1000);
            }
        }
    }

    private void getRemotePassword() {
        DatabaseReference userRef = database.child(Constants.USERS).child(username);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    remotePassword = dataSnapshot.child("password").getValue(String.class);
                    Log.d("Lucas", "User exists");
                } else {
                    Log.d("Lucas", "User not exists");
                    remotePassword = null;
                }
                textRecoverPassword.setVisibility(View.VISIBLE);
                textTitle.setText(R.string.enter_password);
                editText.setEnabled(true);
                editText.setHint(R.string.password);
                editText.getText().clear();
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                changeViews(false);
                openKeyboard();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                username = null;
                changeViews(false);
                dialog.showDialogConnectionFailure(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        openKeyboard();
                    }
                });
            }
        });
    }

    private void checkDarkList() {
        String deviceId = Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        DatabaseReference userRef = database.child("dark_list").child(deviceId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    dialog.showBanDialog(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            finish();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void putUserData() {
        FirebaseInstallations.getInstance().getId().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                showDialogBadConnection();
                return;
            }
            String registerId = task.getResult();
            new TimeRegister("fcm_registration").setLastUpdate();
            long now = System.currentTimeMillis();
            user.setUsername(username);
            user.setLoginTimestamp(now);
            user.setRegisterId(registerId);
            user.save();

            Map<String, Object> map = new HashMap<>();
            map.put("password", password);
            map.put("login_timestamp", now);
            map.put("access_timestamp", now);
            map.put("fcm_register_id", user.getRegisterId());

            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    user.removePending();
                    user.save();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    showDialogBadConnection();
                }
            });
            databaseReference.child(Constants.USERS).child(username).updateChildren(map);
        });
    }

    private void showDialogBadConnection() {
        dialog.showDialogConnectionFailure(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                password = null;
                remotePassword = null;
                editText.getText().clear();
                openKeyboard();
            }
        });
    }

    private void openKeyboard() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                editText.setEnabled(true);
                editText.requestFocus();
                editText.setSelection(editText.getText().length());
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(editText, 0);
            }
        }, 100);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openKeyboard();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }
}