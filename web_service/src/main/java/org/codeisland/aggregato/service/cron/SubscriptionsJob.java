package org.codeisland.aggregato.service.cron;

import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Watchlist;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class SubscriptionsJob extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        List<Episode> airing_today = ofy().load().type(Episode.class).
                filter("air_date", format.format(new Date())).
                list();
        for (Episode episode : airing_today){
            List<String> subscribers = episode.getSeries().getSubscribers();
            Map<String, Watchlist> subscriber_lists = ofy().load().type(Watchlist.class).ids(subscribers);

            Watchlist watchlist;
            for (String subscriber_id : subscribers){
                watchlist = subscriber_lists.get(subscriber_id);
                if (watchlist == null){
                    watchlist = new Watchlist(subscriber_id);
                    subscriber_lists.put(subscriber_id, watchlist);
                }
                watchlist.addItem(episode);
            }

            ofy().save().entities(subscriber_lists.values());
        }

    }
}
