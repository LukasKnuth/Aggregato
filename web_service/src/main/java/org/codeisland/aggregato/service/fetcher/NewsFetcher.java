package org.codeisland.aggregato.service.fetcher;

import org.codeisland.aggregato.service.storage.News;
import org.codeisland.aggregato.service.storage.tv.Series;

import java.util.Set;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public interface NewsFetcher {

    public Set<News> getNews(Series series);

}
