package org.codeisland.aggregato.service.workers;

import org.codeisland.aggregato.service.fetcher.FetchManager;
import org.codeisland.aggregato.service.storage.News;
import org.codeisland.aggregato.service.storage.tv.Series;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class NewsWorker extends HttpServlet{

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String series_name = req.getParameter("series_name");
        if (series_name != null){
            Series series = ofy().load().type(Series.class).
                    filter("name_normalized", series_name.toUpperCase()).first().now();
            if (series != null){
                Set<News> news = FetchManager.INSTANCE.getNews(series);

                ofy().save().entities(news);
            } else {
                Logger logger = Logger.getLogger(this.getClass().getName());
                logger.warning(String.format("Series '%s' was not found in our database!", series_name));
            }
        }
        // All done:
        resp.setStatus(200);
    }
}
