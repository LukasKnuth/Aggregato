package org.codeisland.aggregato.service;

import com.google.appengine.api.users.User;
import org.codeisland.aggregato.service.frontend.FrontendHandler;
import org.codeisland.aggregato.service.frontend.HandlerResult;
import org.codeisland.aggregato.service.storage.tv.Series;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class MySubscriptions extends FrontendHandler {

    @Override
    protected HandlerResult handleGet(HttpServletRequest req) throws ServletException, IOException {
        final String thisUrl = req.getRequestURI();

        if (req.getUserPrincipal() != null){
            // User is logged in!
            final User currentUser = getUserService().getCurrentUser();
            final List<Series> userSubscriptions = ofy().load().type(Series.class).
                    filter("subscribers", currentUser.getEmail()).list();
            final List<Series> seriesList = ofy().load().type(Series.class).list();

            return HandlerResult.createFromTemplate("Subscriptions", "templates/subscriptions.html", new Object() {
                List<Series> series = seriesList;
                List<Series> subscriptions = userSubscriptions;
                String user = currentUser.getNickname();
                String logout_link = getUserService().createLogoutURL(thisUrl);
            });
        } else {
            // User is not logged in!
            return HandlerResult.createFromFormat("Subscriptions",
                    "<a href='%s'>Login first!</a>", getUserService().createLoginURL(thisUrl)
            );
        }
    }

    @Override
    protected HandlerResult handlePost(HttpServletRequest req) throws ServletException, IOException {
        if (req.getUserPrincipal() != null){
            // User is logged in!
            User currentUser = getUserService().getCurrentUser();

            String[] keys = req.getParameterValues("series");
            Collection<Series> series = ofy().load().type(Series.class).ids(keys).values();

            for (Series s : series){
                s.subscribe(currentUser);
            }
            ofy().save().entities(series);

            return HandlerResult.createFromString("Subscriptions", "Saved new series to subscriptions");
        } else {
            return HandlerResult.createFromString("Subscriptions", "you're not logged in...");
        }
    }
}
