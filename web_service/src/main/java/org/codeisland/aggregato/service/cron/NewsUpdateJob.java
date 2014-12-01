package org.codeisland.aggregato.service.cron;

import org.codeisland.aggregato.service.fetcher.FetchManager;
import org.codeisland.aggregato.service.fetcher.NewsFetcher;
import org.codeisland.aggregato.service.storage.News;
import org.codeisland.aggregato.service.storage.Series;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class NewsUpdateJob extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        NewsFetcher fetcher = FetchManager.INSTANCE;
        List<Series> series = ofy().load().type(Series.class).list();
        Set<News> news = new HashSet<>();

        for (Series s : series) {
            news.addAll(fetcher.getNews(s));
        }

        ofy().save().entities(news);
    }
}
