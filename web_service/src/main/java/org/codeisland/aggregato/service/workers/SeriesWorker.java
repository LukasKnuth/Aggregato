package org.codeisland.aggregato.service.workers;

import org.codeisland.aggregato.service.fetcher.FetchManager;
import org.codeisland.aggregato.service.fetcher.SeriesFetcher;
import org.codeisland.aggregato.service.storage.tv.Season;
import org.codeisland.aggregato.service.storage.tv.Series;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class SeriesWorker extends HttpServlet {

    // TODO Refactor this, so it will update if the series already exist.
    // TODO When that ^ is done, make the update-cron-job schedule new tasks for every series.

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String series_name = req.getParameter("series_name");
        SeriesFetcher fetcher = FetchManager.INSTANCE;

        Series series = ofy().load().type(Series.class).
                filter("name_normalized", series_name.toUpperCase()).first().now();
        if (series == null){
            // Not yet in the Database, find it!
            try {
                series = fetcher.getSeries(series_name);
            } catch (Exception e){
                if (e.getCause() instanceof IOException){
                    // Only re-run the task if we have connection errors!
                    throw e;
                }
            }
            if (series != null) {
                ofy().save().entities(series).now();
            } else {
                Logger logger = Logger.getLogger(this.getClass().getName());
                logger.warning(String.format("Series '%s' not found in ANY database!", series_name));

                resp.setStatus(200);
                return;
            }
        }
        // Series is there, load the episodes:
        List<Season> fetched_seasons = Collections.emptyList();
        try {
            fetched_seasons = fetcher.getSeasons(series);
        } catch (Exception e){
            if (e.getCause() instanceof IOException){
                // Only re-run the task if we have connection errors!
                throw e;
            }
        }
        List<Season> stored_seasons = series.getSeasons();

        // Diff
        List<Season> new_seasons = new ArrayList<>(fetched_seasons);
        new_seasons.removeAll(stored_seasons);
        if (!new_seasons.isEmpty()){
            for (Season season : new_seasons){
                series.putSeason(season);
            }
            ofy().save().entities(series);
        }

        // All done:
        resp.setStatus(200);
    }
}
