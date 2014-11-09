package org.codeisland.aggregato.service.storage;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
@Entity
public class Episode implements Mergeable<Episode>{

    static {
        ObjectifyService.register(Series.class); // We need this here, otherwise Ref.create throws an exception in the constructor!
    }
    private static final SimpleDateFormat AIR_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    private @Id Long key;
    private @Index String air_date;
    private @Index String title;
    private String description;
    private @Index int episode_number;
    private int season_number;
    private @Index @Load Ref<Series> series; // Loads along with this episode

    private Episode() {} // Objectify needs this, visibility doesn't madder

    public Episode(Series series, String title, int episode_number, int season_number, Date air_date, String description) {
        this.air_date = AIR_FORMAT.format(air_date);
        this.title = title;
        this.description = description;
        this.episode_number = episode_number;
        this.season_number = season_number;
        this.series = Ref.create(series);
    }

    public Date getAirDate() {
        try {
            return AIR_FORMAT.parse(this.air_date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public String getTitle() {
        return title;
    }

    public Series getSeries(){
        return series.get();
    }

    public String getDescription() {
        return description;
    }

    public int getEpisodeNumber() {
        return episode_number;
    }

    public int getSeasonNumber() {
        return season_number;
    }

    public Long getId() {
        return key;
    }

    @Override
    public void merge(Episode other) {
        // TODO What to do here, if we're not sure?? (Admin tool for review?)
        if (other.air_date != null && this.air_date == null){
            this.air_date = other.air_date;
        }
        if (other.description != null){
            if (this.description == null || this.description.isEmpty()){
                this.description = other.description;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("%s aired on %s as episode %s of season %s",
                this.title, this.air_date.toString(), this.episode_number, this.season_number
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Episode episode = (Episode) o;

        if (air_date != null ? !air_date.equals(episode.air_date) : episode.air_date != null) return false;
        if (!series.equals(episode.series)) return false;
        if (title != null ? !title.equals(episode.title) : episode.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = air_date != null ? air_date.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + series.hashCode();
        return result;
    }
}
