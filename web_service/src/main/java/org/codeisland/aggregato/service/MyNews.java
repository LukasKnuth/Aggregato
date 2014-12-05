package org.codeisland.aggregato.service;

import org.codeisland.aggregato.service.frontend.TemplateEngine;
import org.codeisland.aggregato.service.storage.News;
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
public class MyNews extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");

        TemplateEngine.writeTemplate(resp.getWriter(), "News", "templates/news.html", new Object() {
            List<News> news = ofy().load().type(News.class).list(); // Just set the name for the mustache section.
        });
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String series_name = req.getParameter("series_name");
        if (series_name != null && !series_name.isEmpty()){
            QueueManager.queueNews(series_name);
            TemplateEngine.writeString(resp.getWriter(), "News", "Execution is queued!");
        } else {
            TemplateEngine.writeString(resp.getWriter(), "News", "No Series given!");
        }
    }
}
