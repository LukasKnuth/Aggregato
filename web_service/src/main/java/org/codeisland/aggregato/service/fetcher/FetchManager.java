package org.codeisland.aggregato.service.fetcher;

import org.codeisland.aggregato.service.fetcher.impl.TMDBFetcher;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Series;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>This manager automates the process of fetching information from all available
 *  fetchers and merging them together.</p>
 * <p>If you're exclusively interested in a specific fetcher, use the implementations
 *  under {@link org.codeisland.aggregato.service.fetcher.impl}</p>
 * @author Lukas Knuth
 * @version 1.0
 */
public enum FetchManager implements SeriesFetcher{

    INSTANCE;

    private List<SeriesFetcher> seriesFetchers = new LinkedList<>();

    private FetchManager(){
        seriesFetchers.add(new TMDBFetcher());
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
}
