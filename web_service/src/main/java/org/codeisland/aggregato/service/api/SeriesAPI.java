package org.codeisland.aggregato.service.api;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;
import org.codeisland.aggregato.service.storage.*;
import org.codeisland.aggregato.service.workers.QueueManager;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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

    private static final String OAUTH_LOGIN_FAIL = "User is not logged in.";

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

    public List<Season> listSeasons(@Named("series_id") String series_id){
        Series show = ofy().load().group(Series.COMPLETE_TREE.class).
                type(Series.class).id(series_id).now();
        if (show != null){
            return show.getSeasons();
        } else {
            return Collections.emptyList();
        }
    }

    /*
    ------------------ Watchlist -----------------
     */

    public void addToWatchlist(User user, @Named("episode_id") String episode_id) throws OAuthRequestException {
        if (user == null){
            throw new OAuthRequestException(OAUTH_LOGIN_FAIL);
        }

        Watchlist watchlist = ofy().load().type(Watchlist.class).id(user.getEmail()).now();
        if (watchlist == null){
            watchlist = new Watchlist(user);
        }
        Episode episode = ofy().load().type(Episode.class).id(episode_id).now();

        watchlist.addItem(episode);
        ofy().save().entities(watchlist);
    }

    public void addSeriesToWatchlist(User user, @Named("series_id") String series_id) throws OAuthRequestException {
        if (user == null){
            throw new OAuthRequestException(OAUTH_LOGIN_FAIL);
        }

        Watchlist watchlist = ofy().load().type(Watchlist.class).id(user.getEmail()).now();
        if (watchlist == null){
            watchlist = new Watchlist(user);
        }
        Series series = ofy().load().group(Series.COMPLETE_TREE.class).
                type(Series.class).id(series_id).now();

        for (Season season : series.getSeasons()) {
            for (Episode episode : season.getEpisodes()) {
                watchlist.addItem(episode);
            }
        }
        ofy().save().entities(watchlist);
    }

    public void removeFromWatchlist(User user, @Named("episode_id") String episode_id) throws OAuthRequestException {
        if (user == null){
            throw new OAuthRequestException(OAUTH_LOGIN_FAIL);
        }

        Watchlist watchlist = ofy().load().type(Watchlist.class).id(user.getEmail()).now();
        if (watchlist == null){
            // Nothing to remove anything from...
            return;
        }
        Episode episode = ofy().load().type(Episode.class).id(episode_id).now();

        watchlist.removeItem(episode);
        ofy().save().entities(watchlist);
    }

    public Set<Episode> getWatchlist(User user){
        if (user == null){
            return Collections.emptySet();
        }
        Watchlist watchlist = ofy().load().type(Watchlist.class).id(user.getEmail()).now();
        return (watchlist != null) ? watchlist.getWatchlist() : Collections.<Episode>emptySet();
    }

    /*
    ------------------ Subscriptions ---------------
     */

    public void addSubscription(User user, @Named("series_id") String series_id) throws OAuthRequestException {
        if (user == null){
            throw new OAuthRequestException(OAUTH_LOGIN_FAIL);
        }

        Series series = ofy().load().type(Series.class).id(series_id).now();
        series.subscribe(user);
        ofy().save().entities(series);
    }

    public void cancelSubscription(User user, @Named("series_id") String series_id) throws OAuthRequestException {
        if (user == null){
            throw new OAuthRequestException(OAUTH_LOGIN_FAIL);
        }

        Series series = ofy().load().type(Series.class).id(series_id).now();
        series.unsubscribe(user);
        ofy().save().entities(series);
    }

    public List<Series> getSubscriptions(User user){
        if (user == null){
            return Collections.emptyList();
        }

        return ofy().load().type(Series.class).filter("subscribers", user.getEmail()).list();
    }

    /*
    ----------------- News -----------------
     */

    public List<News> getNews(@Named("series_id") String series_id){
        Series series = ofy().load().type(Series.class).id(series_id).now();
        List<News> news = ofy().load().type(News.class).
                filter("series", series).order("pubDate").limit(20).
                list();
        return news;
    }
}
