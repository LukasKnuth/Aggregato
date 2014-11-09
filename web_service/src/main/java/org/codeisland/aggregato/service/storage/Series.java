package org.codeisland.aggregato.service.storage;

import com.google.appengine.api.users.User;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
@Entity
public class Series implements Mergeable<Series>{

    @Id Long key;
    String name;
    @Index String name_normalized; // We need this for case-insensitive filtering
    @Index int season_count; // need index for ordering!
    String tmdb_id; // TODO Store ID's somewhere seperate??
    @Index List<String> subscribers = new ArrayList<>(); // List of user-ID's

    public Series(String name, int season_count, String tmdb_id) {
        this.name = name;
        this.name_normalized = name.toUpperCase();
        this.season_count = season_count;
        this.tmdb_id = tmdb_id;
    }

    @Override
    public void merge(Series other) {
        if (other.season_count > this.season_count){
            this.season_count = other.season_count;
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

    public Series() {} // Objectify needs this one!

    public String getName() {
        return name;
    }

    public int getSeasons() {
        return season_count;
    }

    public String getTmdbId() {
        return tmdb_id;
    }

    public Long getId(){
        return key;
    }

    @Override
    public String toString() {
        return String.format("Series '%s' has %s seasons", name, season_count);
    }
}
