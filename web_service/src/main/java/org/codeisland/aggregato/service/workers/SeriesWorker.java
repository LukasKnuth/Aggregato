package org.codeisland.aggregato.service.workers;

import org.codeisland.aggregato.service.fetcher.FetchManager;
import org.codeisland.aggregato.service.fetcher.SeriesFetcher;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Series;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class SeriesWorker extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String series_name = req.getParameter("series_name");
        SeriesFetcher fetcher = FetchManager.INSTANCE;

        Series series = ofy().load().type(Series.class).
                filter("name_normalized", series_name.toUpperCase()).first().now();
        if (series == null){
            // Not yet in the Database, find it!
            series = fetcher.getSeries(series_name);
            if (series != null) {
                ofy().save().entities(series).now();
            } else {
                Logger logger = Logger.getLogger(this.getClass().getName());
                logger.warning(String.format("Series '%s' not found in ANY database!", series_name));
            }
        }
        // Series is there, load the episodes:
        List<Episode> fetched_episodes = fetcher.getEpisodes(series);
        List<Episode> stored_episodes = ofy().load().type(Episode.class).filter("series", series).list();

        // Diff
        List<Episode> new_episodes = new ArrayList<>(fetched_episodes);
        new_episodes.removeAll(stored_episodes);
        ofy().save().entities(new_episodes);

        // All done:
        resp.setStatus(200);
    }
}
