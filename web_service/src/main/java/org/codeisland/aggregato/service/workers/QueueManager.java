package org.codeisland.aggregato.service.workers;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class QueueManager {

    private QueueManager(){} // Static utility class

    public static void queueSeries(String series_name){
        Queue queue = QueueFactory.getQueue("tvshows");
        queue.add(withUrl("/tasks/series_worker").param("series_name", series_name));
    }

    public static void queueNews(String series_name){
        Queue queue = QueueFactory.getQueue("news");
        queue.add(withUrl("/tasks/news_worker").param("series_name", series_name));
    }
}
