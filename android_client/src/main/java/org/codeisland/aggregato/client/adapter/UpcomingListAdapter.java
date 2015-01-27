package org.codeisland.aggregato.client.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import org.codeisland.aggregato.client.R;
import org.codeisland.aggregato.tvseries.tvseries.model.Episode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Lukas Knuth
 * @author AndroHN
 * @version 1.0
 */
public class UpcomingListAdapter extends BaseAdapter implements ListAdapter {

    private static final SimpleDateFormat DATE_DAY = new SimpleDateFormat("dd");
    private static final SimpleDateFormat DATE_MONTH = new SimpleDateFormat("MMM");
    private final List<Episode> episodesList = new ArrayList<Episode>();
    private final LayoutInflater inflater;

    public UpcomingListAdapter(Context context) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addAll(Collection<Episode> series){
        this.episodesList.addAll(series);
        notifyDataSetChanged();
    }

    public void replaceAll(Collection<Episode> series){
        this.episodesList.clear();
        this.addAll(series);
    }

    public void clearAll(){
        this.episodesList.clear();
        notifyDataSetChanged();
    }

    public Episode get(int index){
        return this.episodesList.get(index);
    }

    @Override
    public int getCount() {
        return this.episodesList.size();
    }

    @Override
    public Object getItem(int i) {
        return this.episodesList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View recycle, ViewGroup parent) {
        View v;
        if (recycle != null){
            v = recycle;
        } else {
            v = inflater.inflate(R.layout.upcoming_list_entry, parent, false);
            // Create a holder, so we don't have to find the views every time:
            Holder viewHolder = new Holder();
            viewHolder.day = (TextView) v.findViewById(R.id.upcoming_list_entry_day);
            viewHolder.month = (TextView) v.findViewById(R.id.upcoming_list_entry_month);
            viewHolder.seriesTitle = (TextView) v.findViewById(R.id.upcoming_list_entry_series_title);
            viewHolder.episodeTitle = (TextView) v.findViewById(R.id.upcoming_list_entry_episode_title);
            viewHolder.episodeNr = (TextView) v.findViewById(R.id.upcoming_list_entry_episode_nr);
            viewHolder.episodeDescriptionIcon = (ImageView) v.findViewById(R.id.upcoming_list_entry_episode_description_icon);
            v.setTag(viewHolder);
        }
        Holder holder = (Holder) v.getTag();
        Episode episode = this.episodesList.get(position);
        // Set new values:
        holder.day.setText(DATE_DAY.format(new Date(episode.getAirDate().getValue())));
        holder.month.setText(DATE_MONTH.format(new Date(episode.getAirDate().getValue())));
        holder.seriesTitle.setText(episode.getSeriesName());
        holder.episodeTitle.setText(episode.getTitle());
        holder.episodeNr.setText(String.format("S%02d E%02d", episode.getSeasonNumber(), episode.getEpisodeNumber()));
        if (episode.getDescription() == null) {
            holder.episodeDescriptionIcon.setVisibility(View.GONE);
        }

        return v;
    }

    private static class Holder{
        public TextView day;
        public TextView month;
        public TextView seriesTitle;
        public TextView episodeTitle;
        public TextView episodeNr;
        public ImageView episodeDescriptionIcon;
    }
}
