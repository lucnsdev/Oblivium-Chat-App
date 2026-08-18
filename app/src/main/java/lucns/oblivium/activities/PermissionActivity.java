package lucns.oblivium.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import lucns.oblivium.R;
import lucns.oblivium.data.User;
import lucns.oblivium.utils.Notify;

public class PermissionActivity extends Activity {

    private String[] PERMISSIONS_RUNTIME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Intent intent = getIntent();
        PERMISSIONS_RUNTIME = intent.getStringArrayExtra("permissions");
        if (PERMISSIONS_RUNTIME == null) finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PERMISSIONS_RUNTIME != null) requestPermissions();
    }

    private void requestPermissions() {
        String[] deniedPermissions = getDeniedPermissions();
        if (deniedPermissions.length > 0) {
            requestPermissions(deniedPermissions, 1234);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                Notify.showToast(permissions[i]);
                finish();
                break;
            }
        }
    }
}