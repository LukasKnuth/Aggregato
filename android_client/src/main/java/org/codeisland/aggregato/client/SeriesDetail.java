package org.codeisland.aggregato.client;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.nirhart.parallaxscroll.views.ParallaxScrollView;
import com.squareup.picasso.Picasso;
import org.codeisland.aggregato.client.network.Endpoint;
import org.codeisland.aggregato.tvseries.tvseries.Tvseries;
import org.codeisland.aggregato.tvseries.tvseries.model.Episode;
import org.codeisland.aggregato.tvseries.tvseries.model.Season;
import org.codeisland.aggregato.tvseries.tvseries.model.Series;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Lukas Knuth
 * @author AndroHN
 * @version 1.0
 */
public class SeriesDetail extends ActionBarActivity {

    private static final SimpleDateFormat DATE_YEAR = new SimpleDateFormat("yyyy");
    private static final SimpleDateFormat DATE_FULL = new SimpleDateFormat("MM/dd/yyyy");

    private ActionBar actionBar;
    private ProgressBar seriesDetailProgress;
    private ParallaxScrollView seriesDetailParallax;
    private ImageView seriesDetailBackdrop;
    private ImageView seriesDetailPoster;
    private TextView seriesDetailName;
    private TextView seriesDetailAirDate;
    private TextView seriesDetailDescription;
    private LinearLayout seriesDetailSeasons;
    private String seriesName;
    private String seriesId;
    private Tvseries.SeriesAPI api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.series_detail);

        // Get views
        this.seriesDetailProgress = (ProgressBar) findViewById(R.id.series_detail_progress);
        this.seriesDetailParallax = (ParallaxScrollView) findViewById(R.id.series_detail_parallax);
        this.seriesDetailBackdrop = (ImageView) findViewById(R.id.series_detail_backdrop);
        this.seriesDetailPoster = (ImageView) findViewById(R.id.series_detail_poster);
        this.seriesDetailName = (TextView) findViewById(R.id.series_detail_name);
        this.seriesDetailAirDate = (TextView) findViewById(R.id.series_detail_air_date);
        this.seriesDetailDescription = (TextView) findViewById(R.id.series_detail_description);
        this.seriesDetailSeasons = (LinearLayout) findViewById(R.id.series_detail_seasons);

        // Set up ap ActionBar
        this.actionBar = getSupportActionBar();
        this.actionBar.setTitle(getString(R.string.activity_series_detail_title));

        // Set subscribe button onClick
        Button subscribeButton = (Button) findViewById(R.id.series_detail_button_subscribe);
        subscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SeriesDetail.this.api = Endpoint.getAuthenticatedTvAPI(SeriesDetail.this);
                if (SeriesDetail.this.api != null){
                    new SubscribeTask().execute(SeriesDetail.this.api);
                }
            }
        });

        // Check if series data was submitted in extraData
        Intent i = getIntent();
        if (i.getBooleanExtra("FROM_EPISODE", false)) {
            // TODO: Progressbar stuck while loading the layout...

            // Hide parallax view and show progressbar
            this.seriesDetailParallax.setVisibility(View.GONE);
            this.seriesDetailProgress.setVisibility(View.VISIBLE);

            // Get series name
            this.seriesName = i.getStringExtra("SERIES_NAME");

            // Load series details
            new LoadSeriesDataTask().execute();
        } else {
            // Show series data from last activity
            Series series = Endpoint.extractSeries(getIntent().getExtras());
            this.showSeriesDetail(series);
        }
    }

    /**
     * Shows the series details
     */
    public void showSeriesDetail(final Series series) {
        // Hide progressbar and show parallax view
        this.seriesDetailProgress.setVisibility(View.GONE);
        this.seriesDetailParallax.setVisibility(View.VISIBLE);

        // Store series id
        this.seriesId = series.getId();

        // Load backdrop and poster
        this.seriesDetailBackdrop.post(new Runnable() {
            @Override
            public void run() {
                Picasso.with(SeriesDetail.this).
                        load(series.getBackdropLink() + "=s" + SeriesDetail.this.seriesDetailBackdrop.getWidth()).
                        placeholder(R.drawable.backdrop_placeholder).
                        resize(SeriesDetail.this.seriesDetailBackdrop.getWidth(), SeriesDetail.this.seriesDetailBackdrop.getHeight()).
                        centerCrop().into(SeriesDetail.this.seriesDetailBackdrop);
            }
        });
        this.seriesDetailPoster.post(new Runnable() {
            @Override
            public void run() {
                Picasso.with(SeriesDetail.this).
                        load(series.getPosterLink() + "=s" + SeriesDetail.this.seriesDetailPoster.getHeight()).
                        placeholder(R.drawable.poster_placeholder).
                        resize(SeriesDetail.this.seriesDetailPoster.getWidth(), SeriesDetail.this.seriesDetailPoster.getHeight()).
                        centerCrop().into(SeriesDetail.this.seriesDetailPoster);
            }
        });

        // Set name, air date and description
        this.seriesDetailName.setText(series.getName());
        String airDate = "";
        if (series.getStartDate() != null) {
            airDate += DATE_YEAR.format(new Date(series.getStartDate().getValue())) + " - ";
        }
        if (series.getEndDate() != null){
            airDate += DATE_YEAR.format(new Date(series.getEndDate().getValue()));
        }
        this.seriesDetailAirDate.setText(airDate);
        this.seriesDetailDescription.setText(series.getDescription());

        // Load seasons data
        new LoadSeasonsDataTask().execute(series.getId());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Endpoint.ACCOUNT_SELECT_REQUEST){
            if (resultCode == RESULT_OK){
                this.api = Endpoint.getAuthenticatedTvAPI(this);
                new SubscribeTask().execute(this.api);
            } else {
                Toast.makeText(this, getString(R.string.activity_series_detail_error_subscription_only_with_login), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Loads the series data.
     */
    public class LoadSeriesDataTask extends AsyncTask<Void, Void, Series> {

        @Override
        protected Series doInBackground(Void... params) {
            try {
                // TODO: Find series by id, not by name!

                // Load series data
                List<Series> seriesList = Endpoint.getTvAPI().findSeries(SeriesDetail.this.seriesName).execute().getItems();
                if (seriesList.size() > 0) {
                    return seriesList.get(0);
                } else {
                    return null;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Series series) {
            if (series != null) {
                // Show the series details
                SeriesDetail.this.showSeriesDetail(series);
            } else {
                // TODO: Display error or go back and show toast that the series could not be found
            }
        }
    }

    /**
     * Loads seasons and episodes.
     */
    private class LoadSeasonsDataTask extends AsyncTask<String, Void, List<Season>>{

        @Override
        protected List<Season> doInBackground(String... series) {
            String series_id = series[0];
            try {
                Tvseries.SeriesAPI api = Endpoint.getTvAPI();
                return api.listSeasons(series_id).execute().getItems();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Season> seasons) {
            LinearLayout seasonsList = SeriesDetail.this.seriesDetailSeasons;
            seasonsList.removeAllViews();
            if (seasons != null){
                // TODO This might be slow... Speed it up?

                LayoutInflater inflater = (LayoutInflater) SeriesDetail.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                // List seasons
                for (Season s : seasons) {
                    // Get views
                    View viewCaption = inflater.inflate(R.layout.series_detail_seasons_caption_entry, seasonsList, false);
                    TextView captionTitle = (TextView) viewCaption.findViewById(R.id.series_detail_seasons_caption_title);
                    TextView captionInfo = (TextView) viewCaption.findViewById(R.id.series_detail_seasons_caption_info);

                    // Set season title
                    captionTitle.setText(s.getName());

                    // Set season info text
                    int episodeCount = s.getEpisodes().size();
                    String infoText = Integer.toString(episodeCount) + " ";
                    if (episodeCount == 1) {
                        infoText += getString(R.string.episode);
                    } else {
                        infoText += getString(R.string.episodes);
                    }
                    if (s.getAirDate() != null){
                        infoText += ", " + DATE_YEAR.format(new Date(s.getAirDate().getValue()));
                    }
                    captionInfo.setText(infoText);

                    // Add to list
                    seasonsList.addView(viewCaption);

                    // List season episodes
                    for (Episode e : s.getEpisodes()) {
                        // Get views
                        View viewEpisode = inflater.inflate(R.layout.series_detail_seasons_episode_entry, seasonsList, false);
                        LinearLayout episodeLayout = (LinearLayout) viewEpisode.findViewById(R.id.series_detail_seasons_episode);
                        TextView episodeTitle = (TextView) viewEpisode.findViewById(R.id.series_detail_seasons_episode_title);
                        TextView episodeAirDate = (TextView) viewEpisode.findViewById(R.id.series_detail_seasons_episode_airdate);
                        ImageView episodeDescriptionIcon = (ImageView) viewEpisode.findViewById(R.id.series_detail_seasons_episode_description_icon);

                        // Set episode name
                        episodeTitle.setText(e.getEpisodeNumber() + ". " + e.getTitle());

                        // Set air date
                        String airDate;
                        if (e.getAirDate() == null) {
                            airDate = "";
                        } else {
                            airDate = DATE_FULL.format(new Date(e.getAirDate().getValue()));
                        }
                        episodeAirDate.setText(airDate);

                        // Hide description icon if not available
                        if (e.getDescription() == null) {
                            episodeDescriptionIcon.setVisibility(View.INVISIBLE);
                        }

                        // Set onClick to show episode description
                        // TODO: Implement touch effects like a real ListView has
                        final Episode ep = e;
                        episodeLayout.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View view) {
                                 // Load dialog layout
                                 LayoutInflater inflater = (LayoutInflater) SeriesDetail.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                 View v = inflater.inflate(R.layout.dialog_long_text, null);

                                 // Set description
                                 String episodeDescription;
                                 if (ep.getDescription() == null) {
                                     episodeDescription = getString(R.string.activity_upcoming_no_episode_description);
                                 } else {
                                     episodeDescription = ep.getDescription();
                                 }
                                 TextView descriptionText = (TextView) v.findViewById(R.id.dialog_long_text_content);
                                 descriptionText.setText(episodeDescription);

                                 // Show dialog
                                 AlertDialog.Builder ad = new AlertDialog.Builder(SeriesDetail.this);
                                 ad.setIcon(R.drawable.ic_action_about);
                                 ad.setTitle(getString(R.string.activity_upcoming_dialog_episode_description_title));
                                 ad.setView(v);
                                 ad.setPositiveButton(getString(R.string.close), new DialogInterface.OnClickListener() {
                                     @Override
                                     public void onClick(DialogInterface dialogInterface, int i) {
                                         // Close dialog
                                     }
                                 });
                                 ad.show();
                             }
                         });

                        // Add to list
                        seasonsList.addView(viewEpisode);
                    }
                }
            }
        }
    }

    /**
     * Subscribes to the series
     */
    private class SubscribeTask extends AsyncTask<Tvseries.SeriesAPI, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Tvseries.SeriesAPI... seriesAPIs) {
            try {
                seriesAPIs[0].addSubscription(SeriesDetail.this.seriesId).execute();

                // TODO: Check result for errors

                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean status) {
            if (status){
                Toast.makeText(SeriesDetail.this, getString(R.string.activity_series_detail_subscribe_ok), Toast.LENGTH_LONG).show();
                AlertDialog.Builder builder = new AlertDialog.Builder(SeriesDetail.this);
                AlertDialog dialog = builder.setCancelable(false)
                        .setTitle(getString(R.string.activity_series_detail_dialog_add_to_watchlist_title))
                        .setMessage(getString(R.string.activity_series_detail_dialog_add_to_watchlist_text))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new AddToWatchlistTask().execute();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Nothing
                            }
                        })
                        .create();
                dialog.show();
            } else {
                Toast.makeText(SeriesDetail.this, getString(R.string.activity_series_detail_subscribe_failed), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Adds series to watchlist
     */
    private class AddToWatchlistTask extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                SeriesDetail.this.api.addSeriesToWatchlist(SeriesDetail.this.seriesId).execute();

                // TODO: Check result for errors

                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean status) {
            if (status){
                Toast.makeText(SeriesDetail.this, getString(R.string.activity_series_detail_add_to_watchlist_ok), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(SeriesDetail.this, getString(R.string.activity_series_detail_add_to_watchlist_failed), Toast.LENGTH_LONG).show();
            }
        }
    }

}