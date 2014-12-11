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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class SeriesAdapter extends BaseAdapter implements ListAdapter {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy");
    private final List<Series> seriesList = new ArrayList<Series>();
    private final Context context;
    private final LayoutInflater inflater;

    public SeriesAdapter(Context context) {
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addAll(Collection<Series> series){
        this.seriesList.addAll(series);
        notifyDataSetChanged();
    }

    public void replaceAll(Collection<Series> series){
        this.seriesList.clear();
        this.addAll(series);
    }

    public void clearAll(){
        this.seriesList.clear();
        notifyDataSetChanged();
    }

    public Series get(int index){
        return this.seriesList.get(index);
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
            // Create a holder, so we don't have to find the views every time:
            Holder viewHolder = new Holder();
            viewHolder.title = (TextView) v.findViewById(R.id.series_list_entry_title);
            viewHolder.air_time = (TextView) v.findViewById(R.id.series_list_entry_air_time);
            v.setTag(viewHolder);
        }
        Holder holder = (Holder) v.getTag();
        Series series = this.seriesList.get(position);
        // Set new values:
        holder.title.setText(series.getName());
        String air_time = "";
        if (series.getStartDate() != null) {
            air_time += DATE_FORMAT.format(new Date(series.getStartDate().getValue())) + " - ";
        }
        if (series.getEndDate() != null){
            air_time += DATE_FORMAT.format(new Date(series.getEndDate().getValue()));
        }
        holder.air_time.setText(air_time);
        return v;
    }

    private static class Holder{
        public TextView title;
        public TextView air_time;
    }
}
