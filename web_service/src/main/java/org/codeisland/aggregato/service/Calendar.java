package org.codeisland.aggregato.service;

import org.codeisland.aggregato.service.frontend.TemplateEngine;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.workers.QueueManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class Calendar extends HttpServlet{

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");

        TemplateEngine.writeTemplate(resp.getWriter(), "Calendar", "templates/calendar.html", new Object() {
            List<Episode> episodes = ofy().load().type(Episode.class).list(); // Just set the name for the mustache section.
        });
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String series_name = req.getParameter("series_name");
        if (series_name != null && !series_name.isEmpty()){
            QueueManager.queueSeries(series_name);
            TemplateEngine.writeString(resp.getWriter(), "News", "Execution is queued!");
        } else {
            TemplateEngine.writeString(resp.getWriter(), "News", "No Series given!");
        }
    }
}
