package org.codeisland.aggregato.client.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.codeisland.aggregato.client.R;
import org.codeisland.aggregato.client.SearchSeries;
import org.codeisland.aggregato.client.SeriesDetail;
import org.codeisland.aggregato.client.adapter.UpcomingListAdapter;
import org.codeisland.aggregato.client.network.Endpoint;
import org.codeisland.aggregato.tvseries.tvseries.model.Episode;

import java.io.IOException;
import java.util.List;

/**
 * @author AndroHN
 * @version 1.0
 */
public class UpcomingTab extends Fragment {

    private boolean isFirstLoadDone;
    private boolean filterUserSubscribed;
    private ProgressBar upcomingTabProgress;
    private LinearLayout upcomingTabLoginInfo;
    private ListView upcomingTabList;
    private LinearLayout upcomingTabListEmpty;
    private TextView upcomingTabListEmptyText;
    private UpcomingListAdapter listAdapter;

    public void setFilterUserSubscribed(boolean filterUserSubscribed) {
        this.filterUserSubscribed = filterUserSubscribed;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.upcoming_tab, container, false);

        // Get views
        this.upcomingTabProgress = (ProgressBar) v.findViewById(R.id.upcoming_tab_progress);
        this.upcomingTabLoginInfo = (LinearLayout) v.findViewById(R.id.upcoming_tab_login_info);
        this.upcomingTabList = (ListView) v.findViewById(R.id.upcoming_tab_list);
        this.upcomingTabListEmpty = (LinearLayout) v.findViewById(R.id.upcoming_tab_list_empty);
        this.upcomingTabListEmptyText = (TextView) v.findViewById(R.id.upcoming_tab_list_empty_text);

        // Set login button onClick
        Button loginButton = (Button) v.findViewById(R.id.upcoming_tab_login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Try to get authenticated API, will show login activity
                Endpoint.getAuthenticatedTvAPI(getActivity());
            }
        });

        // Set search button onClick
        Button searchButton = (Button) v.findViewById(R.id.upcoming_tab_list_empty_search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the search
                startActivity(new Intent(getActivity(), SearchSeries.class));
            }
        });

        // Set episodes list adapter
        this.listAdapter = new UpcomingListAdapter(container.getContext());
        this.upcomingTabList.setAdapter(this.listAdapter);

        // Set episodes list onClick to show episode description
        this.upcomingTabList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                // Load dialog layout
                LayoutInflater inflater = (LayoutInflater) container.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View v = inflater.inflate(R.layout.dialog_long_text, container, false);

                // Set description
                Episode episode = UpcomingTab.this.listAdapter.get(pos);
                String episodeDescription;
                if (episode.getDescription() == null) {
                    episodeDescription = getString(R.string.activity_upcoming_no_episode_description);
                } else {
                    episodeDescription = episode.getDescription();
                }
                TextView descriptionText = (TextView) v.findViewById(R.id.dialog_long_text_content);
                descriptionText.setText(episodeDescription);

                // Show dialog
                AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
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

        // Set episodes list onLongClick to show series details
        this.upcomingTabList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long id) {
                Episode episode = UpcomingTab.this.listAdapter.get(pos);
                Intent i = new Intent(getActivity(), SeriesDetail.class);
                i.putExtra("FROM_EPISODE", true);
                i.putExtra("SERIES_NAME", episode.getSeriesName());
                startActivity(i);
                return true;
            }
        });

        // Load data only when fragment will be visible after creating
        if (!this.isFirstLoadDone && this.getUserVisibleHint()) {
            this.isFirstLoadDone = true;
            new LoadUpcomingEpisodesTask().execute();
        }

        return v;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // TODO: Maybe there is a better way to do the loading on first time getting visible...

        // Load data only when fragment is really displayed for the first time
        if (!this.isFirstLoadDone && isVisibleToUser && getView() != null) {
            this.isFirstLoadDone = true;
            new LoadUpcomingEpisodesTask().execute();
        }
    }

    /**
     * Loads the list of upcoming episodes.
     */
    public class LoadUpcomingEpisodesTask extends AsyncTask<Void, Void, List<Episode>> {

        @Override
        protected List<Episode> doInBackground(Void... params) {
            try {
                if (UpcomingTab.this.filterUserSubscribed) {
                    // Load upcoming episodes for user subscribed series
                    return Endpoint.getAuthenticatedTvAPI(getActivity()).getUpcomingEpisodes().execute().getItems();
                } else {
                    // Load upcoming episodes for all series
                    return Endpoint.getTvAPI().getUpcomingEpisodes().execute().getItems();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Episode> episodes) {
            boolean userLoginState = Endpoint.getLoginStatus(getActivity());

            if (episodes != null) {
                // Shows the login info
                if (!UpcomingTab.this.filterUserSubscribed && !userLoginState) {
                    UpcomingTab.this.upcomingTabLoginInfo.setVisibility(View.VISIBLE);
                }

                // Fill and show list
                UpcomingTab.this.listAdapter.replaceAll(episodes);
                UpcomingTab.this.upcomingTabList.setVisibility(View.VISIBLE);
            } else {
                // Clear and hide list
                UpcomingTab.this.listAdapter.clearAll();
                UpcomingTab.this.upcomingTabList.setVisibility(View.GONE);

                // Show placeholder for empty list
                if (UpcomingTab.this.filterUserSubscribed) {
                    UpcomingTab.this.upcomingTabListEmptyText.setText(getString(R.string.activity_upcoming_list_empty_subscribed));
                } else {
                    UpcomingTab.this.upcomingTabListEmptyText.setText(getString(R.string.activity_upcoming_list_empty_all));
                }
                UpcomingTab.this.upcomingTabListEmpty.setVisibility(View.VISIBLE);
            }

            // Hide progressbar
            UpcomingTab.this.upcomingTabProgress.setVisibility(View.GONE);
        }
    }
}