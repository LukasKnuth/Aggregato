package org.codeisland.aggregato.service.api;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Series;
import org.codeisland.aggregato.service.workers.QueueManager;

import java.util.Collections;
import java.util.List;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * This is the series specific portion of the backend api.
 * @author Lukas Knuth
 * @version 1.0
 */
@Api(
        name = "tvseries",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "tvseries.aggregato.codeisland.org",
                ownerName = "Aggregato",
                packagePath = ""
        ),
        scopes = {Constants.EMAIL_SCOPE}, // Makes use of OAuth
        clientIds = { Constants.ANDROID_CLIENT_ID, Constants.IOS_CLIENT_ID, Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID },
        audiences = { Constants.ANDROID_AUDIENCE }
)
public class SeriesAPI {

    public List<Series> findSeries(@Named("name") String name){
        String name_normalized = name.toUpperCase();
        List<Series> seriesList = ofy().load().type(Series.class).
                filter("name_normalized >=", name_normalized).
                filter("name_normalized <", name_normalized + "\uFFFD").
                list();
        if (seriesList.size() == 0){
            // Not found, add a crawl-job:
            QueueManager.queueSeries(name);
        }
        return seriesList;
    }

    public List<Episode> listEpisodes(@Named("series_id") Long series_id){
        Series show = ofy().load().type(Series.class).id(series_id).now();
        if (show != null){
            return ofy().load().type(Episode.class).filter("series", show).list();
        } else {
            return Collections.emptyList();
        }
    }

    /*
    ------------------ Watchlist -----------------
     */

    public void addToWatchlist(User user, @Named("episode_id") Long episode_id){
        throw new RuntimeException("Not yet implemented!");
    }

    public void addSeriesToWatchlist(User user, @Named("series_id") Long series_id){
        throw new RuntimeException("Not yet implemented!");
    }

    public void removeFromWatchlist(User user, @Named("episode_id") Long episode_id){
        throw new RuntimeException("Not yet implemented!");
    }

    public List<Episode> getWatchlist(User user){
        throw new RuntimeException("Not yet implemented!");
    }

    /*
    ------------------ Subscriptions ---------------
     */

    public void addSubscription(User user, @Named("series_id") Long series_id){
        throw new RuntimeException("Not yet implemented!");
    }

    public void cancelSubscription(User user, @Named("series_id") Long series_id){
        throw new RuntimeException("Not yet implemented!");
    }

    public List<Series> getSubscriptions(User user){
        throw new RuntimeException("Not yet implemented!");
    }

    /*
    ----------------- News -----------------
     */

    public void getNews(@Named("series_id") Long series_id){
        throw new RuntimeException("Not yet implemented!");
    }
}
