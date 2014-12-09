package org.codeisland.aggregato.client;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.gson.GsonFactory;
import org.codeisland.aggregato.client.adapter.SeriesAdapter;
import org.codeisland.aggregato.tvseries.tvseries.Tvseries;
import org.codeisland.aggregato.tvseries.tvseries.model.Series;

import java.io.IOException;
import java.util.List;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class Landing extends Activity {

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
    }

    public class SeriesTask extends AsyncTask<String, Void, List<Series>>{

        @Override
        protected List<Series> doInBackground(String... name) {
            Tvseries.Builder builder = new Tvseries.Builder(
                    AndroidHttp.newCompatibleTransport(), new GsonFactory(), null
            );

            Tvseries tvseries = builder.build();

            try {
                return tvseries.seriesAPI().findSeries(name[0]).execute().getItems();
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
