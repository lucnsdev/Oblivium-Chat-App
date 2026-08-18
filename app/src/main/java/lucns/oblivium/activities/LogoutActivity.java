package lucns.oblivium.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.firebase.messaging.FirebaseMessaging;

import lucns.oblivium.R;
import lucns.oblivium.data.User;

public class LogoutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.loading);

        RelativeLayout rootProgress = findViewById(R.id.rootProgress);
        rootProgress.setVisibility(View.VISIBLE);

        User user = User.getInstance();
        FirebaseMessaging.getInstance().unregister().addOnCompleteListener(task -> {
            //if (task.isSuccessful()) ;
            user.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
