package lucns.oblivium.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import lucns.oblivium.R;
import lucns.oblivium.utils.AppPreferences;

public class ColorSelectActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_color_select);

        if (AppPreferences.hasKey("theme_color")) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
    }
}