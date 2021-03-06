package org.codeisland.aggregato.client;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
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
public class SearchSeries extends ActionBarActivity {

    private ListView series_list;
    private EditText name;
    private SeriesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.search_series);

        // Set up ap ActionBar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.search));

        this.series_list = (ListView) this.findViewById(R.id.series_list);
        this.name = (EditText) this.findViewById(R.id.name);
        this.adapter = new SeriesAdapter(SearchSeries.this);
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
                Intent i = new Intent(SearchSeries.this, SeriesDetail.class);
                i.putExtras(Endpoint.bundleSeries(series));
                startActivity(i);
            }
        });
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

}
