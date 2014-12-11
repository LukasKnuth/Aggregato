package org.codeisland.aggregato.client;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.gson.GsonFactory;
import com.squareup.picasso.Picasso;
import org.codeisland.aggregato.client.network.Endpoint;
import org.codeisland.aggregato.tvseries.tvseries.Tvseries;
import org.codeisland.aggregato.tvseries.tvseries.model.Season;
import org.codeisland.aggregato.tvseries.tvseries.model.Series;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class SeriesDetail extends Activity{

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy");

    private TextView description;
    private TextView name;
    private TextView air_date;
    private ImageView backdrop;
    private ImageView poster;
    private LinearLayout seasons;
    private Series series;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Picasso.with(SeriesDetail.this).setIndicatorsEnabled(true);

        this.series = Endpoint.extractSeries(getIntent().getExtras());

        this.setContentView(R.layout.series_page);
        this.poster = (ImageView) findViewById(R.id.series_poster);
        this.backdrop = (ImageView) findViewById(R.id.series_backdrop);

        backdrop.post(new Runnable() {
            @Override
            public void run() {
                Picasso.with(SeriesDetail.this).
                        load(series.getBackdropLink()+"=s"+backdrop.getWidth()).
                        resize(backdrop.getWidth(), backdrop.getHeight()).centerCrop().
                        into(backdrop);
            }
        });
        poster.post(new Runnable() {
            @Override
            public void run() {
                Picasso.with(SeriesDetail.this).
                        load(series.getPosterLink()+"=s"+poster.getHeight()).
                        resize(poster.getWidth(), poster.getHeight()).centerCrop().
                        into(poster);
            }
        });

        this.seasons = (LinearLayout) findViewById(R.id.series_seasons);
        new SeasonTask().execute(series.getId());

        this.description = (TextView) findViewById(R.id.series_description);
        this.name = (TextView) findViewById(R.id.series_name);
        this.air_date = (TextView) findViewById(R.id.series_air_date);

        name.setText(series.getName());
        String air_time = "";
        if (series.getStartDate() != null) {
            air_time += DATE_FORMAT.format(new Date(series.getStartDate().getValue())) + " - ";
        }
        if (series.getEndDate() != null){
            air_time += DATE_FORMAT.format(new Date(series.getEndDate().getValue()));
        }
        air_date.setText(air_time);
    }

    private class SeasonTask extends AsyncTask<String, Void, List<Season>>{

        @Override
        protected List<Season> doInBackground(String... series) {
            String series_id = series[0];
            try {
                Tvseries.Builder builder = new Tvseries.Builder(
                        AndroidHttp.newCompatibleTransport(), new GsonFactory(), null
                );
                Tvseries api = builder.build();
                return api.seriesAPI().listSeasons(series_id).execute().getItems();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Season> seasons) {
            LinearLayout season_list = SeriesDetail.this.seasons;
            season_list.removeAllViews();
            if (seasons != null){
                // TODO This might be slow... Speed it up?
                LayoutInflater inflater = (LayoutInflater) SeriesDetail.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                for (Season s : seasons) {
                    View v = inflater.inflate(R.layout.season_list_entry, season_list, false);
                    TextView name = (TextView) v.findViewById(R.id.season_list_entry_name);
                    name.setText(s.getName());
                    TextView sub_text = (TextView) v.findViewById(R.id.season_list_entry_sub_text);
                    String year_text = "";
                    if (s.getAirDate() != null){
                        year_text += DATE_FORMAT.format(new Date(s.getAirDate().getValue()))+", ";
                    }
                    sub_text.setText(year_text + s.getEpisodes().size() + " Episodes");
                    season_list.addView(v);
                }
            }
        }
    }
}
