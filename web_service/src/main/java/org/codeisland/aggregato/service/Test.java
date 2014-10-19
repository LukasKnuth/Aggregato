package org.codeisland.aggregato.service;

import com.googlecode.objectify.ObjectifyService;
import org.codeisland.aggregato.service.storage.Series;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class Test extends HttpServlet {

    static {
        ObjectifyService.register(Series.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");

        String name = req.getParameter("name");
        if (name != null){
            Series series = ofy().load().type(Series.class).filter("name", name).first().now();
            if (series != null){
                resp.getWriter().println(String.format("%s has %s seasons!", series.getName(), series.getSeasons()));
            } else {
                resp.getWriter().println("Series not found...");
            }
        } else {
            resp.getWriter().println("No name was given...");
            resp.getWriter().println("<form action='test' method='get'><input type='text' name='name' /><input type='submit' /></form>");
            resp.getWriter().println("<form action='test' method='post'><input type='text' name='name' /><input type='number' name='seasons' /><input type='submit' value='Add' /></form>");

            resp.getWriter().println("<ul>");
            for (Series s : ofy().load().type(Series.class).order("season_count").list()) {
                resp.getWriter().println(String.format("<li>%s (%s seasons)</li>", s.getName(), s.getSeasons()));
            }
            resp.getWriter().println("</ul>");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        String seasons = req.getParameter("seasons");
        if (name != null && seasons != null){
            int se_count = Integer.parseInt(seasons);
            Series series = new Series(name, se_count, null);
            ofy().save().entity(series).now();

            resp.getWriter().println(name + " is stored!");
        } else {
            resp.getWriter().println(String.format("Parameters not present! (Season: %s|Name: %s)", seasons, name));
        }
    }
}
