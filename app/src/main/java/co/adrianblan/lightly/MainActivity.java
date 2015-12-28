package co.adrianblan.lightly;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;

import butterknife.ButterKnife;
import butterknife.Bind;
import butterknife.OnCheckedChanged;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1;
    private static final boolean isOverlayServiceActiveDefaultValue = true;
    private static final int seekBarDayProgressDefaultValue = 80;
    private static final int seekBarNightProgressDefaultValue = 20;

    private boolean isOverlayServiceActive;

    @Bind(R.id.switch_enabled)
    Switch switchEnabled;
    @Bind(R.id.seekbar_day)
    SeekBar seekBarDay;
    @Bind(R.id.seekbar_night)
    SeekBar seekBarNight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Restore data from SharedPreferences
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        isOverlayServiceActive = sharedPreferences.getBoolean("isOverlayServiceActive",
                isOverlayServiceActiveDefaultValue);

        switchEnabled.setChecked(isOverlayServiceActive);

        // Update SeekBars
        int progress = sharedPreferences.getInt("seekBarDayProgress", seekBarDayProgressDefaultValue);
        seekBarDay.setProgress(progress);

        progress = sharedPreferences.getInt("seekBarNightProgress", seekBarNightProgressDefaultValue);
        seekBarNight.setProgress(progress);

        SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean userInitiated) {

                // If our day brightness is darker than night brightness, update
                if(seekBarDay.getProgress() < seekBarNight.getProgress() && userInitiated) {
                    seekBarDay.setProgress(progress);
                    seekBarNight.setProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekBarDay.setOnSeekBarChangeListener(seekBarListener);
        seekBarNight.setOnSeekBarChangeListener(seekBarListener);

        // We request permissions, if we don't have them
        if(!hasDrawOverlayPermission()) {
            requestDrawOverlayPermission();
        }

        // If the service was active before, start it again
        if(isOverlayServiceActive) {
            startOverlayService();
        }
    }

    protected void startOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        startService(intent);
        isOverlayServiceActive = true;
    }

    protected void stopOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        stopService(intent);
        isOverlayServiceActive = false;
    }

    @OnCheckedChanged(R.id.switch_enabled)
    public void onCheckedChanged(boolean isChecked) {
        if(isChecked) {
            startOverlayService();
        } else {
            stopOverlayService();
        }
    }

    /**
     * Returns whether we have the permission to draw overlays.
     *
     * In Marshmallow or higher this has to be done programatically at runtime, however for earlier
     * versions they are accepted on install. Can only be false if on Marshmallow or higher.
     */
    public boolean hasDrawOverlayPermission() {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(getApplicationContext());
        } else {
            // If the version is lower than M and app is running, the permission is already granted.
            return true;
        }
    }

    /**
     * Requests permission for drawing an overlay.
     *
     * Will only run if we do not already have the permission, AND if we are running on
     * Marshmallow or higher.
     */
    public void requestDrawOverlayPermission() {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(getApplicationContext())) {

                // Send an intent, requesting the permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                /**
                 * The user has denied the permission request.
                 * Display an alert dialog informing them of their consequences.
                 */
                if (!Settings.canDrawOverlays(this)) {

                    new AlertDialog.Builder(this)
                            .setTitle(R.string.permission_denied_title)
                            .setMessage(R.string.permission_denied_body)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();

        editor.putBoolean("isOverlayServiceActive", isOverlayServiceActive);
        editor.putInt("seekBarDayProgress", seekBarDay.getProgress());
        editor.putInt("seekBarNightProgress", seekBarNight.getProgress());

        editor.commit();
    }
}
