package org.codeisland.aggregato.service;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.samskivert.mustache.Mustache;
import org.codeisland.aggregato.service.storage.Series;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class MySubscriptions extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final UserService userService = UserServiceFactory.getUserService();
        final String thisUrl = req.getRequestURI();

        if (req.getUserPrincipal() != null){
            // User is logged in!
            final User currentUser = userService.getCurrentUser();
            final List<Series> userSubscriptions = ofy().load().type(Series.class).
                    filter("subscribers", currentUser.getUserId()).list();
            final List<Series> seriesList = ofy().load().type(Series.class).list();

            resp.setCharacterEncoding("UTF-8");
            Mustache.compiler().
                    compile(new InputStreamReader(new FileInputStream("templates/subscriptions.html"))).
                    execute(new Object() {
                        List<Series> series = seriesList;
                        List<Series> subscriptions = userSubscriptions;
                        String user = currentUser.getNickname();
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

            String[] series_ids = req.getParameterValues("series");
            List<Long> keys = new ArrayList<>(series_ids.length);
            for (String s_id : series_ids){
                keys.add(Long.parseLong(s_id));
            }
            Collection<Series> series = ofy().load().type(Series.class).ids(keys).values();

            for (Series s : series){
                s.subscribe(currentUser);
            }
            ofy().save().entities(series);

            resp.getWriter().println("Saved new series to subscriptions");
        } else {
            resp.getWriter().println("you're not logged in...");
        }
    }
}
