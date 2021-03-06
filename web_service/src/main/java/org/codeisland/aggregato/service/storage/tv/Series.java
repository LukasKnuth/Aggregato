package org.codeisland.aggregato.service.storage.tv;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.*;
import org.codeisland.aggregato.service.storage.Mergeable;
import org.codeisland.aggregato.service.storage.components.IdentifierComponent;
import org.codeisland.aggregato.service.storage.components.ImageComponent;
import org.codeisland.aggregato.service.storage.components.SearchableStringComponent;
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
public class Series implements Mergeable<Series> {

    /**
     * Loads the series and it's seasons, not the episodes.
     */
    public static class WITH_SEASONS{}
    /**
     * Loads all seasons and their episodes
     */
    public static class COMPLETE_TREE extends WITH_SEASONS{}

    private static final Logger logger = Logger.getLogger(Series.class.getName());

    private @Id String key;
    private SearchableStringComponent name;
    private String description;
    private int season_count;
    private Date start_date;
    private Date end_date;
    private IdentifierComponent identifiers;

    private @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) Date last_update;
    private @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) ImageComponent poster;
    private @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) ImageComponent backdrop;
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private @Load(WITH_SEASONS.class) List<Ref<Season>> seasons = new ArrayList<>();
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private @Index List<String> subscribers = new ArrayList<>(); // List of user-ID's

    private final @Ignore Set<Season> modified_seasons = new HashSet<>();

    private Series() {} // Objectify needs this one!
    public Series(String name, int season_count, Date start_date) {
        String name_key = name.replaceAll("[^\\w]", "_").toLowerCase();
        String date_key = String.valueOf(start_date.getYear()+1900);
        this.key = name_key+"_"+date_key;

        this.name = new SearchableStringComponent(name);
        this.season_count = season_count;
        this.start_date = start_date;
        this.last_update = new Date();
        this.identifiers = new IdentifierComponent();
        this.poster = ImageComponent.placeholder(CloudStorage.ImageType.POSTER);
        this.backdrop = ImageComponent.placeholder(CloudStorage.ImageType.BACKDROP);
    }

    @Override
    public boolean merge(Series other) {
        boolean was_modified = false;
        if (other.season_count > this.season_count){
            this.season_count = other.season_count;
            was_modified = true;
        }
        if (other.description != null){
            if (this.description == null || this.description.isEmpty()){
                this.description = other.description;
                was_modified = true;
            }
        }
        if (other.end_date != null){
            if (this.end_date == null){
                this.end_date = other.end_date;
                was_modified = true;
            } else if (other.end_date.after(this.end_date)){
                // Canceled series has been renewed...
                this.end_date = other.end_date;
                was_modified = true;
            }
        }
        if (this.start_date == null && other.start_date != null){
            this.start_date = other.start_date;
            was_modified = true;
        }
        if (this.identifiers.merge(other.identifiers)){
            was_modified = true;
        }
        if (this.poster.update(other.poster)){
            was_modified = true;
        }
        if (this.backdrop.update(other.backdrop)){
            was_modified = true;
        }
        return was_modified;
    }

    public List<Season> getSeasons(){
        Collection<Season> sns = ofy().load().refs(this.seasons).values();
        return new ArrayList<>(sns);
    }

    /**
     * Returns the season with the given season nr, or {@code null} if no such season is present.
     */
    public Season getSeason(int season_nr){
        Collection<Season> sns = ofy().load().refs(this.seasons).values();
        for (Season season : sns) {
            if (season.getSeasonNr() == season_nr){
                return season;
            }
        }
        return null;
    }

    /**
     * @return the latest season of this show or {@code null}, if there are no seasons yet.
     */
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Season getLatestSeason(){
        Collection<Season> sns = ofy().load().refs(this.seasons).values();
        int latest_season = -1;
        for (Season season : sns) {
            if (season.getEpisodes().size() > 0 && season.getSeasonNr() > latest_season){
                latest_season = season.getSeasonNr();
            }
        }
        if (latest_season == -1 || latest_season >= this.seasons.size()){
            return null;
        } else {
            return this.getSeason(latest_season);
        }
    }

    /**
     * Adds the season to this series, merging it in if it already exists.
     */
    public void putSeason(Season season){
        Ref<Season> seasonRef = Ref.create(season);
        int i = this.seasons.indexOf(seasonRef);
        if (i == -1){
            // New season:
            if (this.seasons.size() <= season.getSeasonNr()){
                this.seasons.add(seasonRef);
            } else {
                this.seasons.add(season.getSeasonNr(), seasonRef);
            }
            this.modified_seasons.add(season);
        } else {
            // Season already present:
            Season stored = this.seasons.get(i).get();
            // Put the episodes first:
            for (Episode episode : season.getModifiedEpisodes()) {
                stored.putEpisode(episode);
            }
            if (stored.merge(season)){
                // If anything was changed during merge, schedule for update
                this.modified_seasons.add(stored);
            }
        }
    }

    @OnSave
    private void saveSeasons(){
        if (this.modified_seasons.size() != 0){
            ofy().save().entities(this.modified_seasons);
            // A season or an episode or both was updated:
            this.last_update = new Date();
        }
    }

    public Date getLastUpdated(){
        if (this.last_update == null){
            // When migrating the old DB content...
            Season latest = this.getLatestSeason();
            if (latest != null){
                return latest.getAirDate();
            } else {
                // No seasons...
                return this.getStartDate();
            }
        } else {
            return this.last_update;
        }
    }

    public void subscribe(User subscriber){
        this.subscribers.add(subscriber.getEmail());
    }

    public void unsubscribe(User subscriber){
        this.subscribers.remove(subscriber.getEmail());
    }

    public List<String> getSubscribers() {
        return subscribers;
    }

    public String getName() {
        return name.get();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return start_date;
    }

    public Date getEndDate() {
        return end_date;
    }

    public void setEndDate(Date end_date) {
        this.end_date = end_date;
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public ImageComponent getPoster() {
        return poster;
    }

    public String getPosterLink(){
        return this.poster.getServingUrl();
    }

    public void setPoster(ImageComponent poster) {
        this.poster = poster;
    }

    public ImageComponent getBackdrop() {
        return backdrop;
    }

    public String getBackdropLink(){
        return this.backdrop.getServingUrl();
    }

    public void setBackdrop(ImageComponent backdrop) {
        this.backdrop = backdrop;
    }

    public int getSeasonCount() {
        return season_count;
    }

    public String optIdentifier(String key, String fallback){
        return this.identifiers.opt(key, fallback);
    }

    public void putIdentifier(String key, String identifier){
        // TODO Fetchers should be able to add new Identifiers without saving the series themselfs. Maybe a global was_modified flag?
        this.identifiers.put(key, identifier);
    }

    public String getId(){
        return key;
    }

    @Override
    public String toString() {
        return String.format("'%s' (ID=%s)", name, key);
    }
}
