package org.codeisland.aggregato.service.storage;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.users.User;
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
public class Series implements Mergeable<Series>{

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
    private String name;
    private String description;
    private int season_count;
    private Date start_date;
    private Date end_date;
    private @Index String name_normalized; // We need this for case-insensitive filtering
    private Map<String, String> identifiers = new HashMap<>();

    private @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) BlobKey poster;
    private @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) BlobKey backdrop;
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

        this.name = name;
        this.name_normalized = name.toUpperCase();
        this.season_count = season_count;
        this.start_date = start_date;
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
        if (!this.identifiers.equals(other.identifiers)){
            for (Map.Entry<String, String> id : other.identifiers.entrySet()) {
                if (this.identifiers.containsKey(id.getKey())){
                    // Key is already there, check the values!
                    if (!this.identifiers.get(id.getKey()).equals(id.getValue())){
                        // Values are different!
                        logger.warning(String.format(
                                "Identifiers differ for key '%s': Mine is '%s', Other is '%s'",
                                id.getKey(), this.identifiers.get(id.getKey()), id.getValue()
                        ));
                        // TODO Maybe do some basic checking (if mine is empty or null and other isn't...)
                    }
                } else {
                    // Key is not in our map...
                    this.identifiers.put(id.getKey(), id.getValue());
                    was_modified = true;
                }
            }
        }
        if (other.poster != null && !other.poster.equals(this.poster)){
            this.setPoster(other.poster);
            was_modified = true;
        }
        if (other.backdrop != null && !other.backdrop.equals(this.backdrop)){
            this.setBackdrop(other.backdrop);
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
            if (this.seasons.get(i).get().merge(season)){
                // If anything was changed during merge, schedule for update
                this.modified_seasons.add(season);
            }
        }
    }

    @OnSave
    private void saveSeasons(){
        if (this.modified_seasons.size() != 0){
            ofy().save().entities(this.modified_seasons);
        }
    }

    public void subscribe(User subscriber){
        this.subscribers.add(subscriber.getUserId());
    }

    public void unsubscribe(User subscriber){
        this.subscribers.remove(subscriber.getUserId());
    }

    public List<String> getSubscribers() {
        return subscribers;
    }

    public String getName() {
        return name;
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
    public BlobKey getPoster() {
        return poster;
    }

    public String getPosterLink(){
        return CloudStorage.serveImage(this.poster, CloudStorage.ImageType.POSTER);
    }

    public void setPoster(BlobKey poster) {
        this.poster = poster;
    }

    public BlobKey getBackdrop() {
        return backdrop;
    }

    public String getBackdropLink(){
        return CloudStorage.serveImage(this.backdrop, CloudStorage.ImageType.BACKDROP);
    }

    public void setBackdrop(BlobKey backdrop) {
        this.backdrop = backdrop;
    }

    public int getSeasonCount() {
        return season_count;
    }

    public String optIdentifier(String key, String fallback){
        if (identifiers.containsKey(key)){
            return identifiers.get(key);
        } else {
            return fallback;
        }
    }

    public void putIdentifier(String key, String identifier){
        // TODO Fetchers should be able to add new Identifiers without saving the series themselfs. Maybe a global was_modified flag?
        identifiers.put(key, identifier);
    }

    public String getId(){
        return key;
    }

    @Override
    public String toString() {
        return String.format("Series '%s' has %s seasons", name, season_count);
    }
}
