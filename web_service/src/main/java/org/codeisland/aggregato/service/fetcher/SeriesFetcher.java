package org.codeisland.aggregato.service.fetcher;

import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Series;

import java.util.List;

/**
 * Fetches Series and Episode information from a database.
 * @author Lukas Knuth
 * @version 1.0
 */
public interface SeriesFetcher {

    public Series getSeries(String name);

    public List<Episode> getEpisodes(Series series);

    // TODO need to distinguish between initial load and update!
}
