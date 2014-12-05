package org.codeisland.aggregato.service;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.codeisland.aggregato.service.frontend.TemplateEngine;
import org.codeisland.aggregato.service.storage.Series;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
            TemplateEngine.writeTemplate(resp.getWriter(), "Subscriptions", "templates/subscriptions.html", new Object() {
                List<Series> series = seriesList;
                List<Series> subscriptions = userSubscriptions;
                String user = currentUser.getNickname();
                String logout_link = userService.createLogoutURL(thisUrl);
            });
        } else {
            // User is not logged in!
            TemplateEngine.writeFormat(resp.getWriter(), "Subscriptions",
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

            String[] keys = req.getParameterValues("series");
            Collection<Series> series = ofy().load().type(Series.class).ids(keys).values();

            for (Series s : series){
                s.subscribe(currentUser);
            }
            ofy().save().entities(series);

            TemplateEngine.writeString(resp.getWriter(), "Subscriptions", "Saved new series to subscriptions");
        } else {
            TemplateEngine.writeString(resp.getWriter(), "Subscriptions", "you're not logged in...");
        }
    }
}
