package org.codeisland.aggregato.service.storage;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.*;

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

    private @Id Long key;
    private String name;
    private int season_count;
    private Date start_date;
    private Date end_date;
    private @Load(WITH_SEASONS.class) List<Ref<Season>> seasons = new ArrayList<>();

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) // Hide this from export via Endpoints.
    private @Index List<String> subscribers = new ArrayList<>(); // List of user-ID's
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private @Index String name_normalized; // We need this for case-insensitive filtering
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Map<String, String> identifiers = new HashMap<>();

    private final @Ignore Set<Season> modified_seasons = new HashSet<>();

    private Series() {} // Objectify needs this one!
    public Series(String name, int season_count, Date start_date) {
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
        if (!this.identifiers.equals(other.identifiers)){
            Logger logger = Logger.getLogger(Series.class.getName());

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
        return was_modified;
    }

    public List<Season> getSeasons(){
        Collection<Season> sns = ofy().load().refs(this.seasons).values();
        return new ArrayList<>(sns);
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

    public Date getStartDate() {
        return start_date;
    }

    public Date getEndDate() {
        return end_date;
    }

    public void setEndDate(Date end_date) {
        this.end_date = end_date;
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
        identifiers.put(key, identifier);
    }

    public Long getId(){
        return key;
    }

    @Override
    public String toString() {
        return String.format("Series '%s' has %s seasons", name, season_count);
    }
}
