package org.codeisland.aggregato.service.fetcher.impl;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.codeisland.aggregato.service.fetcher.NewsFetcher;
import org.codeisland.aggregato.service.storage.News;
import org.codeisland.aggregato.service.storage.Series;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link org.codeisland.aggregato.service.fetcher.NewsFetcher} for the RSS/Atom feeds of
 *  <a href="http://www.serienjunkies.de/">Serienjunkies.de</a>.
 * @author Lukas Knuth
 * @version 1.0
 */
public class SerienjunkiesFetcher implements NewsFetcher {

    public static final String LANGUAGE = "de";

    @Override
    public Set<News> getNews(Series series) {
        Set<News> news = new HashSet<>();

        String series_name_encoded = series.getName().toLowerCase().replaceAll("[ ]", "-");
        String url = String.format("http://www.serienjunkies.de/rss/serie/%s.atom", series_name_encoded);
        try {
            URL feedUrl = new URL(url);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));

            for (SyndEntry entry : feed.getEntries()){
                // Make the news:
                news.add(News.create(
                        series, entry.getLink(), LANGUAGE, entry.getTitle(),
                        entry.getPublishedDate(), entry.getDescription().getValue()
                ));
            }
        } catch (FeedException | IOException e) {
            e.printStackTrace();
        }
        return news;
    }
}