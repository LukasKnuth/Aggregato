package org.codeisland.aggregato.service.storage;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.*;

import javax.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
@Entity
@Cache
public class Episode implements Mergeable<Episode>{

    static {
        // We need this here explicitly, otherwise Ref.create throws an exception in the constructor if
        // ObjectifyProxy hasn't been used yet!
        ObjectifyService.register(Series.class);
    }
    public static final SimpleDateFormat AIR_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    private @Id String key;
    private @Index String air_date;
    private @Index String title;
    private String description;
    private int episode_number;
    private int season_number;

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private @Load Ref<Season> season;

    private Episode() {} // Objectify needs this, visibility doesn't madder
    public Episode(Season season, String title, int episode_number, int season_number, @Nullable Date air_date, @Nullable String description) {
        if (air_date != null){
            this.air_date = AIR_FORMAT.format(air_date);
        }
        this.key = season.getId()+"e"+episode_number;
        if (title == null || title.isEmpty()){
            this.title = String.format("s%se%s", season_number, episode_number);
        } else {
            this.title = title;
        }
        this.description = description;
        this.episode_number = episode_number;
        this.season_number = season_number;
        this.season = Ref.create(season);
    }

    public Date getAirDate() {
        if (this.air_date == null){
            return null;
        } else {
            try {
                return AIR_FORMAT.parse(this.air_date);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getTitle() {
        return title;
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Series getSeries(){
        return season.get().getSeries();
    }

    public String getSeriesName(){
        // TODO This causes the Series to be loaded, which costs extra. Better way??
        return this.getSeries().getName();
    }

    public Season getSeason(){
        return season.get();
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

    public String getId() {
        return key;
    }

    @Override
    public boolean merge(Episode other) {
        boolean was_modified = false;
        if (other.air_date != null && !other.air_date.equals(this.air_date)){
            this.air_date = other.air_date;
            was_modified = true;
        }
        if (other.description != null){
            if (this.description == null || this.description.isEmpty()){
                this.description = other.description;
                was_modified = true;
            }
        }
        if (!this.title.equals(other.title)){
            this.title = other.title;
            was_modified = true;
        }
        return was_modified;
    }

    public static Comparator<Episode> BY_DATE = new Comparator<Episode>() {
        @Override
        public int compare(Episode e1, Episode e2) {
            if (e1.getAirDate().after(e2.getAirDate())){
                return 1;
            } else if (e2.getAirDate().after(e1.getAirDate())){
                return -1;
            } else {
                return 0;
            }
        }
    };

    @Override
    public String toString() {
        return String.format("%s aired on %s as episode %s of season %s",
                this.title, this.air_date, this.episode_number, this.season_number
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Episode episode = (Episode) o;

        if (episode_number != episode.episode_number) return false;
        if (season_number != episode.season_number) return false;
        if (season != null ? !season.equals(episode.season) : episode.season != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = episode_number;
        result = 31 * result + season_number;
        result = 31 * result + (season != null ? season.hashCode() : 0);
        return result;
    }
}
