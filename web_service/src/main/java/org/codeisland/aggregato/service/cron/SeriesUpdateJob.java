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

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * Checks all available databases for new information on currently stored series.
 * @author Lukas Knuth
 * @version 1.0
 */
public class SeriesUpdateJob extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SeriesFetcher fetcher = FetchManager.INSTANCE;
        List<Series> series = ofy().load().group(Series.COMPLETE_TREE.class).type(Series.class).list();
        List<Series> changed_series = new LinkedList<>();

        for (Series s : series){
           if (fetcher.update(s)){
                // Only if something changed!
                changed_series.add(s);
           }
        }

        // Save all changes
        if (!changed_series.isEmpty()){
            ofy().save().entities(changed_series);
        }
    }
}
