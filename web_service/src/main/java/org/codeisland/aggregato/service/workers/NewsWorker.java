package org.codeisland.aggregato.service.workers;

import org.codeisland.aggregato.service.news.RssHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class NewsWorker extends HttpServlet{

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String series_name = req.getParameter("series_name");
        if (series_name != null){
            String series_name_encoded = series_name.toLowerCase().replaceAll("[ ]", "-");
            String url = String.format("http://www.serienjunkies.de/rss/serie/%s.xml", series_name_encoded);
            try {
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

                URL link = new URL(url);
                BufferedReader r = new BufferedReader(new InputStreamReader(link.openStream()));
                InputSource in = new InputSource(r);
                in.setEncoding("ISO-8859-1"); // TODO This is bongus. Need to convert here!

                parser.parse(in, new RssHandler("de")); // TODO Callback whenever a news-item is found
            } catch (ParserConfigurationException | SAXException e) {
                resp.getWriter().println(e);
            }
        }
        // All done:
        resp.setStatus(200);
    }
}
