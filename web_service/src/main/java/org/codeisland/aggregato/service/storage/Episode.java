package org.codeisland.aggregato.service.storage;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;

import java.util.Date;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
@Entity
public class Episode {

    private @Id Long key;
    private Date air_date;
    private @Index String title;
    private @Load Ref<Series> series; // Loads along with this episode

    public Episode() {}

    public Episode(Series series, Date air_date, String title) {
        this.air_date = air_date;
        this.title = title;
        this.series = Ref.create(series);
    }

    public Date getAirDate() {
        return air_date;
    }

    public String getTitle() {
        return title;
    }

    public Series getSeries(){
        return series.get();
    }
}
