package org.codeisland.aggregato.service;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.codeisland.aggregato.service.frontend.TemplateEngine;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Watchlist;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
public class MyWatchlist extends HttpServlet{

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final UserService userService = UserServiceFactory.getUserService();
        final String thisUrl = req.getRequestURI();

        if (req.getUserPrincipal() != null){
            // User is logged in!
            final User currentUser = userService.getCurrentUser();
            final Watchlist wlist = ofy().load().type(Watchlist.class).id(currentUser.getUserId()).now();
            final List<Episode> episodeList = ofy().load().type(Episode.class).list();

            resp.setCharacterEncoding("UTF-8");
            TemplateEngine.writeTemplate(resp.getWriter(), "Watchlist", "templates/watchlist.html", new Object() {
                Set<Episode> watchlist = (wlist != null) ? wlist.getWatchlist() : Collections.<Episode>emptySet();
                String user = currentUser.getNickname();
                List<Episode> episodes = episodeList;
                String logout_link = userService.createLogoutURL(thisUrl);
            });
        } else {
            // User is not logged in!
            TemplateEngine.writeFormat(resp.getWriter(), "Login first",
                    "<a href='%s'>Login first!</a>", userService.createLoginURL(thisUrl)
            );
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getUserPrincipal() != null){
            // User is logged in!
            UserService userService = UserServiceFactory.getUserService();
            User currentUser = userService.getCurrentUser();

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
            TemplateEngine.writeString(resp.getWriter(), "Watchlist", "Saved new episodes to watchlist");
        } else {
            TemplateEngine.writeString(resp.getWriter(), "Watchlist", "you're not logged in...");
        }
    }
}
