package org.codeisland.aggregato.service.storage;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;

import java.util.Date;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
@Entity
public class Episode implements Mergeable<Episode>{

    private @Id Long key;
    private @Index Date air_date;
    private @Index String title;
    private String description;
    private @Index int episode_number;
    private int season_number;
    private @Index @Load Ref<Series> series; // Loads along with this episode

    public Episode() {}

    public Episode(Series series, String title, int episode_number, int season_number, Date air_date, String description) {
        this.air_date = air_date;
        this.title = title;
        this.description = description;
        this.episode_number = episode_number;
        this.season_number = season_number;
        this.series = Ref.create(series);
    }

    public Date getAirDate() {
        return air_date;
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
