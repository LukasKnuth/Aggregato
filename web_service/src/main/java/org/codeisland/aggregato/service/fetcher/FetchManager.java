package org.codeisland.aggregato.service.fetcher;

import org.codeisland.aggregato.service.fetcher.impl.SerienjunkiesFetcher;
import org.codeisland.aggregato.service.fetcher.impl.TMDBFetcher;
import org.codeisland.aggregato.service.storage.News;
import org.codeisland.aggregato.service.storage.Season;
import org.codeisland.aggregato.service.storage.Series;

import java.util.*;

/**
 * <p>This manager automates the process of fetching information from all available
 *  fetchers and merging them together.</p>
 * <p>If you're exclusively interested in a specific fetcher, use the implementations
 *  under {@link org.codeisland.aggregato.service.fetcher.impl}</p>
 * @author Lukas Knuth
 * @version 1.0
 */
public enum FetchManager implements SeriesFetcher, NewsFetcher{

    INSTANCE;

    private List<SeriesFetcher> seriesFetchers = new LinkedList<>();
    private List<NewsFetcher> newsFetchers = new LinkedList<>();

    private FetchManager(){
        seriesFetchers.add(new TMDBFetcher());

        newsFetchers.add(new SerienjunkiesFetcher());
    }

    @Override
    public boolean update(Series series) {
        boolean was_modified = false;
        for (SeriesFetcher fetcher : this.seriesFetchers) {
            if (fetcher.update(series)){
                was_modified = true;
            }
        }
        return was_modified;
    }

    @Override
    public Series getSeries(String name) {
        Series collected = null;
        for (SeriesFetcher fetcher : this.seriesFetchers){
            if (collected == null){
                collected = fetcher.getSeries(name);
            } else {
                collected.merge(fetcher.getSeries(name));
            }
        }
        return collected;
    }

    @Override
    public List<Season> getSeasons(Series series) {
        List<Season> collected = new ArrayList<>();
        for (SeriesFetcher fetcher : this.seriesFetchers){
            List<Season> fetchedSeasons = fetcher.getSeasons(series);
            // Merge the seasons we already have into the ones we need
            int i;
            for (Season s : fetchedSeasons){
                i = collected.indexOf(s);
                if (i == -1){
                    // Season is not in the list:
                    collected.add(s);
                } else {
                    // Season already in the list:
                    collected.get(i).merge(s);
                }
            }
        }
        return collected;
    }

    @Override
    public Set<News> getNews(Series series) {
        Set<News> collected = new HashSet<>();
        for (NewsFetcher fetcher : this.newsFetchers){
            collected.addAll(fetcher.getNews(series));
        }
        return collected;
    }
}