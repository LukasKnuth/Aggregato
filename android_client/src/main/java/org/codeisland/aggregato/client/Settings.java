package org.codeisland.aggregato.client;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.codeisland.aggregato.client.network.Endpoint;
import org.codeisland.aggregato.tvseries.tvseries.Tvseries;

import java.io.IOException;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class Settings extends PreferenceActivity {
    // TODO: Add own Toolbar to implement back navigation by tapping on "< Settings" in title.

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private final static String SENDER_ID = "203492774524";
    private CheckBoxPreference notification_pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences); // TODO: Deprecated, use PreferenceFragments
        notification_pref = (CheckBoxPreference) findPreference(getString(R.string.settings_key_notifications));
        notification_pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (preference == notification_pref && o instanceof Boolean) {
                    Boolean activate_notifications = (Boolean) o;
                    if (activate_notifications) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Settings.this);
                        // TODO We should also check if the app is a new version and update the GCM-ID!
                        if (prefs.getString(getString(R.string.settings_key_gcm_id), null) == null) {
                            // Not yet registered, check if we have PlayServices:
                            if (playServicesAvailable()) {
                                // Everything is fine, perform registration:
                                Tvseries.SeriesAPI api = Endpoint.getAuthenticatedTvAPI(Settings.this);
                                if (api != null) {
                                    new RegisterNotification().execute(api);
                                }
                            }
                            return false;
                        }
                    }
                }
                return true;
            }
        });
    }

    /**
     * Checks if Google Play Services are available on the device and shows dialogs to inform the user
     *  about what he can do.
     */
    private boolean playServicesAvailable(){
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS){
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)){
                // Device is not supported, but the user can enable the support
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                // Device is not supported at all!
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                AlertDialog dialog = builder.setTitle(R.string.activity_settings_dialog_not_supported_title).
                        setMessage(R.string.activity_settings_dialog_not_supported_message).
                        setIcon(android.R.drawable.ic_dialog_alert).
                        setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).create();
                dialog.show();
            }
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Endpoint.ACCOUNT_SELECT_REQUEST){
            if (resultCode == RESULT_CANCELED){
                Toast.makeText(this, getString(R.string.activity_settings_toast_notifications_for_registered_only), Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_OK){
                Tvseries.SeriesAPI api = Endpoint.getAuthenticatedTvAPI(this);
                assert api != null;
                new RegisterNotification().execute(api);
            }
        }
    }

    private class RegisterNotification extends AsyncTask<Tvseries.SeriesAPI, Void, Boolean>{

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(Settings.this);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.activity_settings_process_dialog_registering_push_notifications));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (progressDialog != null){
                progressDialog.dismiss();
            }
            if (success){
                Toast.makeText(Settings.this, getString(R.string.activity_settings_toast_registration_successful), Toast.LENGTH_SHORT).show();
                notification_pref.setChecked(true);
            } else {
                // TODO Do this automatically?
                Toast.makeText(Settings.this, getString(R.string.activity_settings_toast_registration_failed), Toast.LENGTH_SHORT).show();
                notification_pref.setChecked(false);
            }
        }

        @Override
        protected Boolean doInBackground(Tvseries.SeriesAPI... seriesAPIs) {
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(Settings.this);
            try {
                String reg_id = gcm.register(SENDER_ID);
                seriesAPIs[0].registerGcmId(reg_id).execute();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Settings.this);
                prefs.edit().putString(getString(R.string.settings_key_gcm_id), reg_id).apply();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
