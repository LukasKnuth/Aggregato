package org.codeisland.aggregato.service.storage.tv;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.api.server.spi.config.Nullable;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.*;
import org.codeisland.aggregato.service.storage.Mergeable;
import org.codeisland.aggregato.service.storage.components.ImageComponent;
import org.codeisland.aggregato.service.storage.components.PublicationDateComponent;
import org.codeisland.aggregato.service.util.CloudStorage;

import java.util.*;
import java.util.logging.Logger;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
@Entity
@Cache
public class Season implements Mergeable<Season> {

    private static final Logger logger = Logger.getLogger(Season.class.getName());
    static {
        ObjectifyService.register(Episode.class);
    }

    private @Id String key;
    private String name;
    private int season_nr;
    private PublicationDateComponent air_date;
    private @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) ImageComponent poster;
    private @Load(Series.COMPLETE_TREE.class) List<Ref<Episode>> episodes = new ArrayList<>();

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private @Load Ref<Series> series;

    private final @Ignore Set<Episode> modified_episodes = new HashSet<>();

    private Season(){}
    public Season(Series series, String name, int season_nr, @Nullable Date air_date) {
        this.key = series.getId()+"s"+season_nr;
        this.name = name;
        this.season_nr = season_nr;
        this.air_date = new PublicationDateComponent(air_date);
        this.series = Ref.create(series);
        this.poster = ImageComponent.placeholder(CloudStorage.ImageType.POSTER);
    }

    public void putEpisode(Episode episode){
        Ref<Episode> episodeRef = Ref.create(episode);
        int i = this.episodes.indexOf(episodeRef);

        if (i == -1){
            // New Episode:
            if (episode.getEpisodeNumber() >= episodes.size()){
                episodes.add(episodeRef);
            } else {
                episodes.add(episode.getEpisodeNumber(), episodeRef);
            }
            this.modified_episodes.add(episode);
        } else {
            Episode stored = this.episodes.get(i).get();
            if (stored != null){
                // Episode was found in DB, merge it:
                if (stored.merge(episode)) {
                    this.modified_episodes.add(stored);
                }
            } else {
                // Episode wasn't found in DB (maybe it's new since the last update), check the modified episodes:
                for (Episode modified_episode : this.modified_episodes) {
                    if (modified_episode.equals(episode)) {
                        modified_episode.merge(episode);
                        break;
                    }
                }
            }
        }
    }

    @OnSave
    private void saveEpisodes(){
        if (this.modified_episodes.size() != 0){
            ofy().save().entities(this.modified_episodes);
        }
    }

    public String getPosterLink(){
        return poster.getServingUrl();
    }

    public Series getSeries(){
        return this.series.get();
    }

    public String getName() {
        return name;
    }

    public int getSeasonNr() {
        return season_nr;
    }

    public String getId() {
        return key;
    }

    /**
     * The date when the first episode of this season aired.
     */
    public Date getAirDate() {
        return air_date.get();
    }

    public List<Episode> getEpisodes() {
        Collection<Episode> eps = ofy().load().refs(this.episodes).values();
        return new ArrayList<>(eps);
    }

    Collection<Episode> getModifiedEpisodes(){
        return this.modified_episodes;
    }

    public ImageComponent getPoster() {
        return poster;
    }

    public void setPoster(ImageComponent poster) {
        this.poster = poster;
    }

    public Episode getEpisode(int episode_nr){
        return episodes.get(episode_nr).get();
    }

    @Override
    public boolean merge(Season other) {
        boolean was_modified = !(this.modified_episodes.isEmpty());
        if (this.name == null && other.name != null){
            this.name = other.name;
            was_modified = true;
        }
        if (this.air_date.merge(other.air_date)){
            was_modified = true;
        }
        if (this.poster.update(other.getPoster())){
            was_modified = true;
        }
        return was_modified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Season season = (Season) o;

        if (season_nr != season.season_nr) return false;
        if (series != null ? !series.equals(season.series) : season.series != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = season_nr;
        result = 31 * result + (series != null ? series.hashCode() : 0);
        return result;
    }
}
