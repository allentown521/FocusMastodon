package allen.town.focus.twitter.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import androidx.preference.PreferenceManager;
import android.provider.Settings;

import allen.town.focus.twitter.R;

import allen.town.focus_common.views.AccentMaterialDialog;

public class PermissionModelUtils {

    public static final String[] NECESSARY_PERMISSIONS = new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.POST_NOTIFICATIONS
    };

    private static final String PERMISSION_CHECK_PREF = "marshmallow_permission_check";

    private Context context;
    private SharedPreferences sharedPrefs;

    public PermissionModelUtils(Context context) {
        this.context = context;
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean needPermissionCheck() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                sharedPrefs.getBoolean(PERMISSION_CHECK_PREF, true);
    }

    public void showPermissionExplanationThenAuthorization() {
        requestPermissions();
        sharedPrefs.edit().putBoolean(PERMISSION_CHECK_PREF, false).commit();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            ((Activity)context).requestPermissions(NECESSARY_PERMISSIONS, 1);
    }

    public void showStorageIssue(Throwable e) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new AccentMaterialDialog(
                context,
                R.style.MaterialAlertDialogTheme
        )
                    .setTitle(R.string.storage_permission)
                    .setMessage(R.string.no_storage_permission)
                    .setPositiveButton(R.string.ok, (dialog, which) -> ((Activity) context).requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1))
                    .setNegativeButton(R.string.Focus_for_Mastodon_settings, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + context.getApplicationContext().getPackageName()));
                        context.startActivity(intent);
                    })
                    .create().show();
        } else {
            new AccentMaterialDialog(
                context,
                R.style.MaterialAlertDialogTheme
        )
                    .setTitle("Something went wrong")
                    .setMessage("Here is the description: " + e.getMessage())
                    .setPositiveButton(R.string.ok, (dialog, which) -> { })
                    .create().show();
        }
    }

    public void showVideoRecorderPermissions() {
        new AccentMaterialDialog(
                context,
                R.style.MaterialAlertDialogTheme
        )
                .setTitle(R.string.video_permissions)
                .setMessage(R.string.no_video_permission)
                .setPositiveButton(R.string.ok, (dialog, which) -> { })
                .setNegativeButton(R.string.Focus_for_Mastodon_settings, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + context.getApplicationContext().getPackageName()));
                    context.startActivity(intent);
                })
                .create().show();
    }

    public void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ((Activity) context).requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    public void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ((Activity) context).requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
}
