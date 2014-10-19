package org.codeisland.aggregato.service.storage;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
@Entity
public class Series {

    @Id Long key;
    String name;
    @Index String name_normalized; // We need this for case-insensitive filtering
    @Index int season_count; // need index for ordering!
    String tmdb_id; // TODO Store ID's somewhere seperate??

    public Series(String name, int season_count, String tmdb_id) {
        this.name = name;
        this.name_normalized = name.toUpperCase();
        this.season_count = season_count;
        this.tmdb_id = tmdb_id;
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

    @Override
    public String toString() {
        return String.format("Series '%s' has %s seasons", name, season_count);
    }
}
