package org.codeisland.aggregato.client.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import org.codeisland.aggregato.client.R;
import org.codeisland.aggregato.tvseries.tvseries.model.Series;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class SeriesAdapter extends BaseAdapter implements ListAdapter {

    private final List<Series> seriesList = new ArrayList<Series>();
    private final Context context;
    private final LayoutInflater inflater;

    public SeriesAdapter(Context context) {
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void add(Series series){
        this.seriesList.add(series);
    }

    public void addAll(Collection<Series> series){
        this.seriesList.addAll(series);
    }

    @Override
    public int getCount() {
        return this.seriesList.size();
    }

    @Override
    public Object getItem(int i) {
        return this.seriesList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View recycle, ViewGroup parent) {
        View v = null;
        if (recycle != null){
            v = recycle;
        } else {
            v = inflater.inflate(R.layout.series_list_entry, parent, false);
            Holder viewHolder = new Holder();
            viewHolder.title = (TextView) v.findViewById(R.id.series_list_entry_title);
            viewHolder.seasons = (TextView) v.findViewById(R.id.series_list_entry_seasons);
            v.setTag(viewHolder);
        }
        Holder holder = (Holder) v.getTag();
        holder.title.setText(this.seriesList.get(position).getName());
        holder.seasons.setText(this.seriesList.get(position).getSeasons()+" seasons");
        return v;
    }

    private static class Holder{
        public TextView title;
        public TextView seasons;
    }
}
