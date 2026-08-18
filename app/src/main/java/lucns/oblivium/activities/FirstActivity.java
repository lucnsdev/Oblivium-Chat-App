package lucns.oblivium.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import lucns.oblivium.R;
import lucns.oblivium.data.User;

public class FirstActivity extends Activity {

    private final String[] PERMISSIONS_RUNTIME = new String[]{
            Manifest.permission.POST_NOTIFICATIONS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_first);

        if (getDeniedPermissions().length > 0) {
            Intent intent = new Intent(this, PermissionActivity.class);
            intent.putExtra("permissions", PERMISSIONS_RUNTIME);
            startActivity(intent);
            finish();
            return;
        }

        User user = User.getInstance();
        if (user.hasCredentials()) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }

    private String[] getDeniedPermissions() {
        List<String> permissions = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        String packageName = getPackageName();
        for (String permission : PERMISSIONS_RUNTIME) {
            if (packageManager.checkPermission(permission, packageName) != PackageManager.PERMISSION_GRANTED)
                permissions.add(permission);
        }
        return permissions.toArray(new String[permissions.size()]);
    }
}
