package org.codeisland.aggregato.service;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.ObjectifyService;
import com.samskivert.mustache.Mustache;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Watchlist;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class MyWatchlist extends HttpServlet{

    static {
        ObjectifyService.register(Watchlist.class);
        ObjectifyService.register(Episode.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final UserService userService = UserServiceFactory.getUserService();
        final String thisUrl = req.getRequestURI();

        if (req.getUserPrincipal() != null){
            // User is logged in!
            final User currentUser = userService.getCurrentUser();
            final Watchlist wlist = ofy().load().type(Watchlist.class).filter("user_id", currentUser.getUserId()).first().now();
            final List<Episode> episodeList = ofy().load().type(Episode.class).list();

            resp.setCharacterEncoding("UTF-8");
            Mustache.compiler().
                    compile(new InputStreamReader(new FileInputStream("templates/watchlist.html"))).
                    execute(new Object() {
                        List<Episode> watchlist = (wlist != null) ? wlist.getWatchlist() : Collections.<Episode>emptyList();
                        String user = currentUser.getNickname();
                        List<Episode> episodes = episodeList;
                        String logout_link = userService.createLogoutURL(thisUrl);
                    }, resp.getWriter());
        } else {
            // User is not logged in!
            resp.getWriter().format("<a href='%s'>Login first!</a>", userService.createLoginURL(thisUrl));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getUserPrincipal() != null){
            // User is logged in!
            UserService userService = UserServiceFactory.getUserService();
            User currentUser = userService.getCurrentUser();

            Watchlist watchlist = ofy().load().type(Watchlist.class).filter("user_id", currentUser.getUserId()).first().now();
            if (watchlist == null){
                // No Watchlist yet:
                watchlist = new Watchlist(currentUser);
            }

            String[] episodes = req.getParameterValues("episodes");
            for (String ep : episodes){
                Long episode_id = Long.parseLong(ep);
                Episode episode = ofy().load().type(Episode.class).id(episode_id).now();
                watchlist.addItem(episode);
            }

            ofy().save().entities(watchlist);
            resp.getWriter().println("Saved new episodes to watchlist");
        } else {
            resp.getWriter().println("you're not logged in...");
        }
    }
}
