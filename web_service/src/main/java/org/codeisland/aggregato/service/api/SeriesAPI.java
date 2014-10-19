package org.codeisland.aggregato.service.api;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.googlecode.objectify.ObjectifyService;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Series;

import java.util.Collections;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

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

    static {
        ObjectifyService.register(Series.class);
        ObjectifyService.register(Episode.class);
    }

    public List<Series> findSeries(@Named("name") String name){
        String name_normalized = name.toUpperCase();
        return ofy().load().type(Series.class).
                filter("name_normalized >=", name_normalized).
                filter("name_normalized <", name_normalized+"\uFFFD").
                list();
    }

    public List<Episode> listEpisodes(@Named("series") String series){
        List<Series> shows = findSeries(series);
        if (shows.size() > 0){
            return ofy().load().type(Episode.class).filter("series", shows.get(0)).list();
        } else {
            return Collections.emptyList();
        }
    }
}
