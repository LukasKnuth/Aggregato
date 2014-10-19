package org.codeisland.aggregato.service.workers;

import com.googlecode.objectify.ObjectifyService;
import org.codeisland.aggregato.service.calendar.SeriesFetcher;
import org.codeisland.aggregato.service.calendar.TMDBFetcher;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Series;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class SeriesWorker extends HttpServlet {

    static {
        ObjectifyService.register(Series.class);
        ObjectifyService.register(Episode.class);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String series_name = req.getParameter("series_name");
        SeriesFetcher fetcher = new TMDBFetcher();

        Series series = ofy().load().type(Series.class).
                filter("name", series_name).first().now();
        if (series == null){
            // Not yet in the Database, find it!
            series = fetcher.getSeries(series_name);
            ofy().save().entities(series).now();
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
