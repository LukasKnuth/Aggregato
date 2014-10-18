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
    @Index String name;
    @Index int season_count; // need index for ordering!

    public Series(String name, int season_count) {
        this.name = name;
        this.season_count = season_count;
    }

    public Series() {} // Objectify needs this one!

    public String getName() {
        return name;
    }

    public int getSeasons() {
        return season_count;
    }

    @Override
    public String toString() {
        return String.format("Series '%s' has %s seasons", name, season_count);
    }
}
