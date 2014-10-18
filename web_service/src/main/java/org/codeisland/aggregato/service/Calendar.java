package org.codeisland.aggregato.service;

import com.googlecode.objectify.ObjectifyService;
import com.samskivert.mustache.Mustache;
import org.codeisland.aggregato.service.calendar.SeriesFetcher;
import org.codeisland.aggregato.service.calendar.TMDBFetcher;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Series;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class Calendar extends HttpServlet{

    static {
        ObjectifyService.register(Episode.class);
        ObjectifyService.register(Series.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");

        Mustache.compiler().
                compile(new InputStreamReader(new FileInputStream("templates/series.html"))).
                execute(new Object() {
                    List<Episode> episodes = ofy().load().type(Episode.class).list(); // Just set the name for the mustache section.
                }, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        SeriesFetcher fetcher = new TMDBFetcher();

        Series series = fetcher.getSeries("Chuck");
        ofy().save().entity(series).now();
        resp.getWriter().println(series);

        ofy().save().entities(fetcher.getEpisodes(series));

        resp.getWriter().println("Episodes successfully stored!");
    }
}
