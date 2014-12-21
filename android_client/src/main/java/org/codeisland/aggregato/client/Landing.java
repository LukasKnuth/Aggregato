package org.codeisland.aggregato.client;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import org.codeisland.aggregato.client.adapter.SeriesAdapter;
import org.codeisland.aggregato.client.network.Endpoint;
import org.codeisland.aggregato.tvseries.tvseries.model.Series;

import java.io.IOException;
import java.util.List;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class Landing extends ActionBarActivity {

    private ListView series_list;
    private EditText name;
    private SeriesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.landing);

        this.series_list = (ListView) this.findViewById(R.id.series_list);
        this.name = (EditText) this.findViewById(R.id.name);
        this.adapter = new SeriesAdapter(Landing.this);
        series_list.setAdapter(adapter);

        this.name.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_SEARCH){
                    new SeriesTask().execute(name.getText().toString());
                    return true;
                }
                return false;
            }
        });

        this.series_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                Series series = adapter.get(pos);
                Intent i = new Intent(Landing.this, SeriesDetail.class);
                i.putExtras(Endpoint.bundleSeries(series));
                startActivity(i);
            }
        });

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.contains(getString(R.string.settings_key_gcm_id))
                && prefs.getBoolean(getString(R.string.settings_key_notifications), true)){
            // Not registered for Notifications yet.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setCancelable(false)
                    .setTitle(R.string.activity_landing_enable_notifications_dialog_title)
                    .setMessage(R.string.activity_landing_enable_notifications_dialog_message)
                    .setIcon(R.drawable.dialog_notification)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(Landing.this, Settings.class));
                        }
                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            prefs.edit().
                                    putBoolean(getString(R.string.settings_key_notifications), false).
                                    apply();
                        }
                    }).create();
            dialog.show();
        }
    }

    public class SeriesTask extends AsyncTask<String, Void, List<Series>>{

        @Override
        protected List<Series> doInBackground(String... name) {
            try {
                return Endpoint.getTvAPI().findSeries(name[0]).execute().getItems();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Series> shows) {
            if (shows != null){
                adapter.replaceAll(shows);
            } else {
                adapter.clearAll();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.landing_actionbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.landing_actionbar_watchlist:
                startActivity(new Intent(this, Watchlist.class));
                return true;
            case R.id.landing_actionbar_settings:
                startActivity(new Intent(this, Settings.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
