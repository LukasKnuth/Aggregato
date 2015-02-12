package org.codeisland.aggregato.service;

import org.codeisland.aggregato.service.frontend.FrontendHandler;
import org.codeisland.aggregato.service.frontend.HandlerResult;
import org.codeisland.aggregato.service.storage.tv.Episode;
import org.codeisland.aggregato.service.workers.QueueManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class Calendar extends FrontendHandler {

    @Override
    protected HandlerResult handleGet(HttpServletRequest req) throws ServletException, IOException {
        return HandlerResult.createFromTemplate("Calendar", "templates/calendar.html", new Object() {
            List<Episode> episodes = ofy().load().type(Episode.class).list(); // Just set the name for the mustache section.
        });
    }

    @Override
    protected HandlerResult handlePost(HttpServletRequest req) throws ServletException, IOException {
        String series_name = req.getParameter("series_name");
        if (series_name != null && !series_name.isEmpty()){
            QueueManager.queueSeries(series_name);
            return HandlerResult.createFromString("News", "Execution is queued!");
        } else {
            return HandlerResult.createFromString("News", "No Series given!");
        }
    }
}
