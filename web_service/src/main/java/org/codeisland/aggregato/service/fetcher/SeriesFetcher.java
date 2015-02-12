package org.codeisland.aggregato.service.fetcher;

import org.codeisland.aggregato.service.storage.tv.Season;
import org.codeisland.aggregato.service.storage.tv.Series;

import java.util.List;

/**
 * Fetches Series and Episode information from a database.
 * @author Lukas Knuth
 * @version 1.0
 */
public interface SeriesFetcher {

    public Series getSeries(String name);

    public List<Season> getSeasons(Series series);

    /**
     * Get new information about the series, it's seasons and episodes, and merge it into the given series.
     * @return whether the update changed anything in this series.
     */
    public boolean update(Series series);
}