package org.codeisland.aggregato.service.storage;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.Date;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
@Entity
public class News {

    private String title;
    private @Id String link;
    private String description;
    private @Index Date pubDate;
    private @Index String language;
    private Ref<Series> series;

    /**
     * <p>Creates a new {@code News}-object with the given properties.</p>
     * <p>Unknown properties can be supplied as {@code null}-values, only {@code link} and {@code series} are
     *  mandatory.</p>
     * @throws java.lang.IllegalArgumentException if either {@code link} or {@code series} are {@code null}.
     */
    public static News create(Series series, String link, String language, String title, Date pubDate, String description){
        if (link == null){
            throw new IllegalArgumentException("The link can't be null! Did you set it since the last news object was build?");
        } else if (series == null){
            throw new IllegalArgumentException("The series can't be null (obviously...) !");
        }
        return new News(title, link, description, pubDate, language, series);
    }

    private News(){} // For Objectify

    private News(String title, String link, String description, Date pubDate, String language, Series series) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.pubDate = pubDate;
        this.language = language;
        this.series = Ref.create(series);
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public String getDescription() {
        return description;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public String getLanguage() {
        return language;
    }

    /**
     * Returns the Series associated with these news. This will trigger one additional
     *  database request!
     */
    public Series getSeries() {
        return series.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        News news = (News) o;

        if (!link.equals(news.link)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return link.hashCode();
    }
}
