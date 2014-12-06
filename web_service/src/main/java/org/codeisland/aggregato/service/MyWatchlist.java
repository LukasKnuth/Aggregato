package org.codeisland.aggregato.service;

import com.google.appengine.api.users.User;
import org.codeisland.aggregato.service.frontend.FrontendHandler;
import org.codeisland.aggregato.service.frontend.HandlerResult;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Watchlist;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class MyWatchlist extends FrontendHandler {

    @Override
    protected HandlerResult handleGet(HttpServletRequest req) throws ServletException, IOException {
        final String thisUrl = req.getRequestURI();

        if (req.getUserPrincipal() != null){
            // User is logged in!
            final User currentUser = getUserService().getCurrentUser();
            final Watchlist wlist = ofy().load().type(Watchlist.class).id(currentUser.getUserId()).now();
            final List<Episode> episodeList = ofy().load().type(Episode.class).list();

            return HandlerResult.createFromTemplate("Watchlist", "templates/watchlist.html", new Object() {
                Set<Episode> watchlist = (wlist != null) ? wlist.getWatchlist() : Collections.<Episode>emptySet();
                String user = currentUser.getNickname();
                List<Episode> episodes = episodeList;
                String logout_link = getUserService().createLogoutURL(thisUrl);
            });
        } else {
            // User is not logged in!
            return HandlerResult.createFromFormat("Login first",
                    "<a href='%s'>Login first!</a>", getUserService().createLoginURL(thisUrl)
            );
        }
    }

    @Override
    protected HandlerResult handlePost(HttpServletRequest req) throws ServletException, IOException {
        if (req.getUserPrincipal() != null){
            // User is logged in!
            User currentUser = getUserService().getCurrentUser();

            Watchlist watchlist = ofy().load().type(Watchlist.class).id(currentUser.getUserId()).now();
            if (watchlist == null){
                // No Watchlist yet:
                watchlist = new Watchlist(currentUser);
            }

            String[] keys = req.getParameterValues("episodes");
            Collection<Episode> episodes = ofy().load().type(Episode.class).ids(keys).values();
            for (Episode ep : episodes){
                watchlist.addItem(ep);
            }

            ofy().save().entities(watchlist);
            return HandlerResult.createFromString("Watchlist", "Saved new episodes to watchlist");
        } else {
            return HandlerResult.createFromString("Watchlist", "you're not logged in...");
        }
    }
}
