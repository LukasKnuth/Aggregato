package org.codeisland.aggregato.client;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.gson.GsonFactory;
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
    private Button send;
    private EditText name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.landing);

        this.series_list = (ListView) this.findViewById(R.id.series_list);
        this.send = (Button) this.findViewById(R.id.send);
        this.name = (EditText) this.findViewById(R.id.name);

        this.send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SeriesTask().execute(name.getText().toString());
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
        protected void onPostExecute(List<Series> serieses) {
            if (serieses != null){
                series_list.setAdapter(
                        new ArrayAdapter<Series>(Landing.this, android.R.layout.simple_list_item_1, serieses)
                );
            }
        }
    }
}
