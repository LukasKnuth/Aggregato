package org.codeisland.aggregato.client;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import org.codeisland.aggregato.client.adapter.WatchlistAdapter;
import org.codeisland.aggregato.client.network.Endpoint;
import org.codeisland.aggregato.client.view.SwipeDismissListViewTouchListener;
import org.codeisland.aggregato.tvseries.tvseries.Tvseries;
import org.codeisland.aggregato.tvseries.tvseries.model.Episode;

import java.io.IOException;
import java.util.List;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class Watchlist extends ActionBarActivity {

    private WatchlistAdapter adapter;
    private Tvseries.SeriesAPI api;
    private RelativeLayout progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.watchlist_page);

        ListView watchlist = (ListView) findViewById(R.id.watchlist_list);
        this.adapter = new WatchlistAdapter(this);
        watchlist.setAdapter(adapter);

        progress = (RelativeLayout) findViewById(R.id.watchlist_progress);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            SwipeDismissListViewTouchListener swipe = new SwipeDismissListViewTouchListener(
                    watchlist, new SwipeDismissListViewTouchListener.DismissCallbacks() {
                @Override
                public boolean canDismiss(int position) {
                    return true;
                }

                @Override
                public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                    for (int pos : reverseSortedPositions) {
                        new RemoveTask().execute(adapter.get(pos));
                    }
                    adapter.removeAt(reverseSortedPositions);
                }
            });
            watchlist.setOnTouchListener(swipe);
            watchlist.setOnScrollListener(swipe.makeScrollListener());
        } else {
            // TODO implement this with long press I guess....
        }

        api = Endpoint.getAuthenticatedTvAPI(this);
        if (api != null){
            new WatchlistTask().execute(api);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Endpoint.ACCOUNT_SELECT_REQUEST){
            if (resultCode == RESULT_OK){
                api = Endpoint.getAuthenticatedTvAPI(this);
                new WatchlistTask().execute(api);
            } else {
                // Nothing to do here...
                Toast.makeText(this, "The watchlist is only available, if you're logged in.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * Loads the watchlist for the activity.
     * TODO Replace this with a Loader at some point...
     */
    private class WatchlistTask extends AsyncTask<Tvseries.SeriesAPI, Void, List<Episode>>{

        @Override
        protected List<Episode> doInBackground(Tvseries.SeriesAPI... seriesAPIs) {
            try {
                return seriesAPIs[0].getWatchlist().execute().getItems();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Episode> episodes) {
            if (episodes != null){
                adapter.replaceAll(episodes);
            } else {
                adapter.clearAll();
            }
            progress.setVisibility(View.GONE);
        }
    }

    /**
     * Remove a Episode from the watchlist.
     */
    private class RemoveTask extends AsyncTask<Episode, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Episode... episodes) {
            assert Watchlist.this.api != null; // should, since we need it to get the list items in the first place
            try {
                Watchlist.this.api.removeFromWatchlist(episodes[0].getId()).execute();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success){
                Toast.makeText(
                        Watchlist.this,
                        "Couldn't remove the episode from the Watchlist. Connection problems?",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}
