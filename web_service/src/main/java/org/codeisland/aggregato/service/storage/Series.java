package org.codeisland.aggregato.service.storage;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
@Entity
public class Series implements Mergeable<Series>{

    private @Id Long key;
    private String name;
    private int season_count;
    private Date start_date;
    private Date end_date;

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) // Hide this from export via Endpoints.
    private @Index List<String> subscribers = new ArrayList<>(); // List of user-ID's
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private @Index String name_normalized; // We need this for case-insensitive filtering
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Map<String, String> identifiers = new HashMap<>();

    private Series() {} // Objectify needs this one!
    public Series(String name, int season_count, Date start_date) {
        this.name = name;
        this.name_normalized = name.toUpperCase();
        this.season_count = season_count;
        this.start_date = start_date;
    }

    @Override
    public void merge(Series other) {
        if (other.season_count > this.season_count){
            this.season_count = other.season_count;
        }
        if (other.end_date != null){
            if (this.end_date == null){
                this.end_date = other.end_date;
            } else if (other.end_date.after(this.end_date)){
                // Canceled series has been renewed...
                this.end_date = other.end_date;
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
                }
            }
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
