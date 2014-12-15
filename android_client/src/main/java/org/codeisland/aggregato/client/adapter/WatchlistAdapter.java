package org.codeisland.aggregato.client.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import org.codeisland.aggregato.client.R;
import org.codeisland.aggregato.tvseries.tvseries.model.Episode;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class WatchlistAdapter extends BaseAdapter implements ListAdapter {

    private final List<Episode> watchlist;
    private final LayoutInflater inflater;
    private final DateFormat dateFormat;

    public WatchlistAdapter(Context context){
        this.watchlist = new ArrayList<Episode>();
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.dateFormat = android.text.format.DateFormat.getDateFormat(context);
    }
    public WatchlistAdapter(Context context, Collection<Episode> initial){
        this(context);
        this.watchlist.addAll(initial);
    }

    public void replaceAll(Collection<Episode> episodes){
        watchlist.clear();
        watchlist.addAll(episodes);
        notifyDataSetChanged();
    }

    public void clearAll(){
        watchlist.clear();
        notifyDataSetChanged();
    }

    public void removeAt(int[] positions){
        for (int pos : positions){
            watchlist.remove(pos);
        }
        notifyDataSetChanged();
    }

    public Episode get(int position){
        return watchlist.get(position);
    }

    @Override
    public int getCount() {
        return watchlist.size();
    }

    @Override
    public Object getItem(int i) {
        return watchlist.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int pos, View recycle, ViewGroup parent) {
        View v;
        if (recycle != null){
            v = recycle;
        } else {
            v = this.inflater.inflate(R.layout.watchlist_list_entry, parent, false);
            Holder holder = new Holder();
            holder.name = (TextView) v.findViewById(R.id.watchlist_list_entry_name);
            holder.subtext = (TextView) v.findViewById(R.id.watchlist_list_entry_subtext);
            holder.airDate = (TextView) v.findViewById(R.id.watchlist_list_entry_air_date);
            v.setTag(holder);
        }
        Holder holder = (Holder) v.getTag();
        Episode episode = this.watchlist.get(pos);
        holder.name.setText(episode.getTitle());
        holder.subtext.setText(String.format("s%se%s", episode.getSeasonNumber(), episode.getEpisodeNumber()));
        holder.airDate.setText("Aired on "+this.dateFormat.format(new Date(episode.getAirDate().getValue())));
        return v;
    }

    private static class Holder{
        private TextView name;
        private TextView subtext;
        private TextView airDate;
    }
}
