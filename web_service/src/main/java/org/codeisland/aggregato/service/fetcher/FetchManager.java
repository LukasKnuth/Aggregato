package org.codeisland.aggregato.service.fetcher;

import org.codeisland.aggregato.service.fetcher.impl.SerienjunkiesFetcher;
import org.codeisland.aggregato.service.fetcher.impl.TMDBFetcher;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.News;
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
    public List<Episode> getEpisodes(Series series) {
        List<Episode> collected = new ArrayList<>();
        for (SeriesFetcher fetcher : this.seriesFetchers){
            List<Episode> fetchedEpisodes = fetcher.getEpisodes(series);
            // Merge the episodes we already have into the ones we need
            int i;
            for (Episode e : fetchedEpisodes){
                i = collected.indexOf(e);
                if (i == -1){
                    // Episode is not in the list:
                    collected.add(e);
                } else {
                    // Episode already in the list:
                    collected.get(i).merge(e);
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
