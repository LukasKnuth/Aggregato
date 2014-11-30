package org.codeisland.aggregato.service.fetcher;

import org.codeisland.aggregato.service.storage.Season;
import org.codeisland.aggregato.service.storage.Series;

import java.util.List;

/**
 * Fetches Series and Episode information from a database.
 * @author Lukas Knuth
 * @version 1.0
 */
public interface SeriesFetcher {

    public Series getSeries(String name);

    public List<Season> getSeasons(Series series);
}
