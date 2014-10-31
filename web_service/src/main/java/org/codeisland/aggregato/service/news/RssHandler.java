package org.codeisland.aggregato.service.news;

import org.codeisland.aggregato.service.storage.News;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class RssHandler extends DefaultHandler {

    private final SimpleDateFormat rfc882 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    private final String language;

    private StringBuilder buffer;
    private News current_item;

    public RssHandler(String language){
        this.language = language;
        this.buffer = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName){
            case "item":
                current_item = new News(language);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (current_item == null) return; // Skip the Channel information at the beginning...
        switch (qName){
            case "item":
                ofy().save().entity(current_item);
                break;
            case "title":
                current_item.setTitle(buffer.toString());
                break;
            case "link":
                current_item.setLink(buffer.toString());
                break;
            case "description":
                current_item.setDescription(buffer.toString());
                break;
            case "pubDate":
                try {
                    current_item.setPubDate(rfc882.parse(buffer.toString().trim()));
                } catch (ParseException e) {
                    // Couldn't parse the date... Fuck it !?!
                }
                break;
        }
        this.buffer.setLength(0); // Clear the buffer
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        this.buffer.append(ch, start, length);
    }
}
