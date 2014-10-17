package org.codeisland.aggregato.service;

import com.googlecode.objectify.ObjectifyService;
import com.samskivert.mustache.Mustache;
import org.codeisland.aggregato.service.news.RssHandler;
import org.codeisland.aggregato.service.storage.News;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class NewsParser extends HttpServlet{

    static {
        ObjectifyService.register(News.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");

        Mustache.compiler().
                compile(new InputStreamReader(new FileInputStream("templates/news.html"))).
                execute(new Object() {
                    List<News> news = ofy().load().type(News.class).list(); // Just set the name for the mustache section.
                }, resp.getWriter());

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getParameter("update") != null){
            try {
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                parser.parse("http://www.serienjunkies.de/rss/news.xml", new RssHandler("en"));
            } catch (ParserConfigurationException | SAXException e) {
                resp.getWriter().println(e);
            }
            resp.getWriter().println("Updated news catalog!");
        }
    }
}
