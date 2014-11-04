package org.codeisland.aggregato.service;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.samskivert.mustache.Mustache;
import org.codeisland.aggregato.service.storage.Episode;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class Calendar extends HttpServlet{

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");

        Mustache.compiler().
                compile(new InputStreamReader(new FileInputStream("templates/calendar.html"))).
                execute(new Object() {
                    List<Episode> episodes = ofy().load().type(Episode.class).order("episode_number").list(); // Just set the name for the mustache section.
                }, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String series_name = req.getParameter("series_name");
        if (series_name != null && !series_name.isEmpty()){
            Queue queue = QueueFactory.getQueue("tvshows");
            queue.add(withUrl("/tasks/series_worker").param("series_name", series_name));

            resp.getWriter().println("Execution is queued!");
        } else {
            resp.getWriter().println("No Series given!");
        }
    }
}