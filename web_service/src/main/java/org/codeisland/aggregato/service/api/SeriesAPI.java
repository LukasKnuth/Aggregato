package org.codeisland.aggregato.service.api;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;
import org.codeisland.aggregato.service.storage.News;
import org.codeisland.aggregato.service.storage.Watchlist;
import org.codeisland.aggregato.service.storage.components.PublicationDateComponent;
import org.codeisland.aggregato.service.storage.tv.Episode;
import org.codeisland.aggregato.service.storage.tv.Season;
import org.codeisland.aggregato.service.storage.tv.Series;
import org.codeisland.aggregato.service.workers.QueueManager;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;

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
                filter("name.normalized >=", name_normalized).
                filter("name.normalized <", name_normalized + "\uFFFD").
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
    ------------------ Calendar ------------------
     */

    @ApiMethod(path = "calendar/upcoming")
    public List<Episode> getUpcomingEpisodes(User user) throws OAuthRequestException {
        MutableDateTime start = new MutableDateTime();
        start.addDays(-1); // Include today!
        start.setTime(0, 0, 0, 0);
        Date start_date = start.toDate();
        if (user == null){
            // Not logged in, show 10 episodes airing next:
            String date_str = PublicationDateComponent.FORMAT.format(start_date);
            return ofy().load().type(Episode.class).
                    filter("air_date.date >=", date_str).
                    limit(10).order("air_date.date").list();
        } else {
            // Logged in, use episodes from subscribed shows:
            List<Episode> upcoming = new ArrayList<>();
            List<Series> subscriptions = ofy().load().group(Series.COMPLETE_TREE.class).
                    type(Series.class).filter("subscribers", user.getEmail()).
                    list();

            for (Series sub : subscriptions) {
                List<Season> seasons = sub.getSeasons();
                Season current_season = seasons.get(seasons.size()-1);
                for (Episode episode : current_season.getEpisodes()) {
                    if (episode.getAirDate().after(start_date)){
                        upcoming.add(episode);
                        break;
                    }
                }
            }

            // Order the episodes by date:
            Collections.sort(upcoming, Episode.BY_DATE);

            return upcoming;
        }
    }

    @ApiMethod(path = "calendar/month")
    public List<Episode> episodesInMonth(User user, @Named("month") Date month_date) throws OAuthRequestException {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM");
        DateTime start = new DateTime(month_date).dayOfMonth().setCopy(1);
        DateTime end = start.monthOfYear().addToCopy(1);
        Interval month = new Interval(start, end);

        if (user == null){
            // Not logged in, show all Episodes from that month:
            return ofy().load().type(Episode.class).
                    filter("air_date.date >=", start.toString(formatter)).
                    filter("air_date.date <", end.toString(formatter)).
                    order("air_date.date").list();
        } else {
            // Logged in, show only episodes from subscribed shows:
            List<Episode> month_episodes = new LinkedList<>();
            List<Series> subscriptions = ofy().load().group(Series.COMPLETE_TREE.class).
                    type(Series.class).filter("subscribers", user.getEmail()).
                    list();

            DateTime air_date_time;
            DateTime episode_air_date;
            for (Series sub : subscriptions) {
                for (Season season : sub.getSeasons()) {
                    air_date_time = new DateTime(season.getAirDate());
                    if (air_date_time.isBefore(end)){
                        for (Episode episode : season.getEpisodes()) {
                            episode_air_date = new DateTime(episode.getAirDate());
                            if (month.contains(episode_air_date)){
                                month_episodes.add(episode);
                            }
                        }

                    }
                }

            }

            Collections.sort(month_episodes, Episode.BY_DATE);
            return month_episodes;
        }
    }

    /*
    ------------------ Watchlist -----------------
     */

    @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
    public void registerGcmId(User user, @Named("gcm_id") String gcm_id) throws OAuthRequestException {
        if (user == null){
            throw new OAuthRequestException(OAUTH_LOGIN_FAIL);
        }

        Watchlist watchlist = ofy().load().type(Watchlist.class).id(user.getEmail()).now();
        if (watchlist == null){
            watchlist = new Watchlist(user);
        }
        watchlist.updateGcmId(null, gcm_id);
        ofy().save().entities(watchlist);
    }

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
