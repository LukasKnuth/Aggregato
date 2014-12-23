package org.codeisland.aggregato.service.storage;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.api.server.spi.config.Nullable;
import com.google.appengine.api.blobstore.BlobKey;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.*;
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
public class Season implements Mergeable<Season>{

    private static final Logger logger = Logger.getLogger(Season.class.getName());
    static {
        ObjectifyService.register(Episode.class);
    }

    private @Id String key;
    private String name;
    private int season_nr;
    private Date air_date;
    private @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) BlobKey poster;
    private @Load(Series.COMPLETE_TREE.class) List<Ref<Episode>> episodes = new ArrayList<>();

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private @Load Ref<Series> series;

    private final @Ignore Set<Episode> modified_episodes = new HashSet<>();

    private Season(){}
    public Season(Series series, String name, int season_nr, @Nullable Date air_date) {
        this.key = series.getId()+"s"+season_nr;
        this.name = name;
        this.season_nr = season_nr;
        this.air_date = air_date;
        this.series = Ref.create(series);
    }

    public void putEpisode(Episode episode){
        if (episode == null){
            logger.warning(
                    String.format("Attempted putting of a NULL-Episode on %s, %s",
                            series.get().getName(), this.getName())
            );
            return;
        }
        Ref<Episode> episodeRef = Ref.create(episode);
        int i = this.episodes.indexOf(episodeRef);

        if (i == -1){
            // New Episode:
            if (episode.getEpisodeNumber() >= episodes.size()){
                episodes.add(Ref.create(episode));
            } else {
                episodes.add(episode.getEpisodeNumber(), Ref.create(episode));
            }
            this.modified_episodes.add(episode);
        } else {
            Episode stored = this.episodes.get(i).get();
            if (stored.merge(episode)) {
                this.modified_episodes.add(stored);
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
        return CloudStorage.serveImage(this.poster, CloudStorage.ImageType.POSTER);
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
        return air_date;
    }

    public List<Episode> getEpisodes() {
        Collection<Episode> eps = ofy().load().refs(this.episodes).values();
        return new ArrayList<>(eps);
    }

    public BlobKey getPoster() {
        return poster;
    }

    public void setPoster(BlobKey poster) {
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
        if (other.air_date != null && !other.air_date.equals(this.air_date)){
            this.air_date = other.air_date;
            was_modified = true;
        }
        if (other.poster != null && !other.poster.equals(this.poster)){
            this.setPoster(other.poster);
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
