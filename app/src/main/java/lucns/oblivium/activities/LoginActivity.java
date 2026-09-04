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
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import lucns.oblivium.utils.Utils;

public class LoginActivity extends Activity {

    private ViewChangerController viewChangerController;
    private Button button, buttonBack;
    private RelativeLayout rootForm, rootProgress;
    private LinearLayout rootEditText;
    private CustomDialog dialog;
    private TextView textTitle;
    private EditText editText, editTextPassword;
    private DatabaseReference database;
    private String username, password, remotePassword;
    private User user;
    private TextView textRecoverPassword;
    private String deviceId;
    private int step;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_login);

        deviceId = Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        user = User.getInstance();
        dialog = new CustomDialog(this);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            ((TextView) findViewById(R.id.textVersion)).setText(String.format(Locale.getDefault(), getString(R.string.format_version), versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        database = FirebaseDatabase.getInstance().getReference().child(getString(R.string.app_name).toLowerCase());

        rootForm = findViewById(R.id.rootForm);
        rootProgress = findViewById(R.id.rootProgress);
        viewChangerController = new ViewChangerController(rootForm, rootProgress);

        textTitle = findViewById(R.id.textTitle);
        textTitle.setText(R.string.enter_username);
        textRecoverPassword = findViewById(R.id.textRecoverPassword);
        buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToUserInput();
            }
        });
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeKeyboard();
                button.setEnabled(false);
                changeViews(true);
            }
        });
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (step == 0) {
                    button.setEnabled(editText.getText().length() > 3);
                } else {
                    button.setEnabled(editTextPassword.getText().length() > 3);
                }
            }
        };
        rootEditText = findViewById(R.id.rootEditText);
        editText = findViewById(R.id.editText);
        editTextPassword = findViewById(R.id.editTextPassword);
        if (user.getUsername() != null) editText.setText(user.getUsername());
        editText.addTextChangedListener(textWatcher);
        editTextPassword.addTextChangedListener(textWatcher);

        checkDarkList();
    }

    private void backToUserInput() {
        step = 0;
        closeKeyboard();
        viewChangerController.change(true, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                username = null;
                password = null;
                remotePassword = null;
                rootEditText.setVisibility(View.VISIBLE);
                buttonBack.setVisibility(View.INVISIBLE);
                editTextPassword.setVisibility(View.INVISIBLE);
                textRecoverPassword.setVisibility(View.INVISIBLE);
                editTextPassword.getText().clear();
                textTitle.setText(R.string.enter_username);
                button.setEnabled(true);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Utils.singlePulse();
                        viewChangerController.change(false);
                        openKeyboard();
                    }
                }, 250);
            }
        });
    }

    private void changeViews(boolean showLoading) {
        viewChangerController.change(showLoading, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (showLoading) checkLogin();
            }
        });
    }

    private void checkLogin() {
        switch (step) {
            case 0:
                username = editText.getText().toString();
                if (username.isEmpty()) return;
                getRemotePassword();
                break;
            case 1:
                password = editTextPassword.getText().toString();
                if (password.isEmpty()) return;
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
                            Utils.singlePulse();
                            changeViews(false);
                            dialog.showWrongPasswordDialog(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                    if (v.getId() == R.id.buttonTwo) {
                                        backToUserInput();
                                    } else {
                                        password = null;
                                        openKeyboard();
                                    }
                                }
                            });
                        }
                    }
                }, 1000);
                break;
        }
    }

    private void getRemotePassword() {
        DatabaseReference userRef = database.child(Constants.USERS).child(username).child("password");
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                step = 1;
                Utils.singlePulse();
                if (dataSnapshot.exists()) {
                    remotePassword = dataSnapshot.getValue(String.class);
                    button.setText(R.string.sign_in);
                    textTitle.setText(R.string.enter_password);
                } else {
                    remotePassword = null;
                    button.setText(R.string.sign_up);
                    textTitle.setText(R.string.setup_password);
                }
                textRecoverPassword.setVisibility(View.VISIBLE);
                editTextPassword.setVisibility(View.VISIBLE);
                rootEditText.setVisibility(View.INVISIBLE);
                buttonBack.setVisibility(View.VISIBLE);
                changeViews(false);
                openKeyboard();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                username = null;
                Utils.singlePulse();
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
        DatabaseReference userRef = database.child("dark_list").child(deviceId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Utils.singlePulse();
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
                Utils.singlePulse();
                showDialogBadConnection();
                return;
            }
            String registerId = task.getResult();
            new TimeRegister("fcm_registration").setLastUpdate();
            long now = System.currentTimeMillis();
            user.setUsername(username);
            user.setLoginTimestamp(now);
            user.setRegisterId(registerId);
            user.setSigned();
            user.save();

            Map<String, Object> map = new HashMap<>();
            map.put("password", password);
            map.put("login_timestamp", now);
            map.put("access_timestamp", now);
            map.put("fcm_register_id", user.getRegisterId());
            map.put("device_id", deviceId);

            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Utils.singlePulse();
                    user.removePending();
                    user.save();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Utils.singlePulse();
                    showDialogBadConnection();
                }
            });
            database.child(Constants.USERS).child(username).updateChildren(map);
        });
    }

    private void showDialogBadConnection() {
        dialog.showDialogConnectionFailure(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                password = null;
                remotePassword = null;
                editTextPassword.getText().clear();
                dialog.dismiss();
                openKeyboard();
            }
        });
    }

    private void closeKeyboard() {
        EditText e;
        if (step == 0) {
            e = editText;
        } else {
            e = editTextPassword;
        }
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(e.getWindowToken(), 0);
    }

    private void openKeyboard() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                EditText e;
                if (step == 0) {
                    e = editText;
                } else {
                    e = editTextPassword;
                }
                e.requestFocus();
                e.setSelection(e.getText().length());
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(e, 0);
            }
        }, 250);
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