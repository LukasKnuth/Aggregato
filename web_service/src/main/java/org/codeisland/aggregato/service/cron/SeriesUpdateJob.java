package org.codeisland.aggregato.service.cron;

import org.codeisland.aggregato.service.fetcher.FetchManager;
import org.codeisland.aggregato.service.fetcher.SeriesFetcher;
import org.codeisland.aggregato.service.storage.Series;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * Checks all available databases for new information on currently stored series.
 * @author Lukas Knuth
 * @version 1.0
 */
public class SeriesUpdateJob extends HttpServlet {

    private static final Logger logger = Logger.getLogger(SeriesUpdateJob.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SeriesFetcher fetcher = FetchManager.INSTANCE;
        List<Series> series = ofy().load().group(Series.COMPLETE_TREE.class).type(Series.class).list();
        List<Series> changed_series = new LinkedList<>();

        for (Series s : series){
            try {
                if (fetcher.update(s)) {
                    // Only if something changed!
                    changed_series.add(s);
                }
            } catch (Exception e){
                logger.log(Level.SEVERE, String.format("There was a problem updating %s", series), e);
            }
        }

        // Save all changes
        if (!changed_series.isEmpty()){
            ofy().save().entities(changed_series);
        }
    }
}
