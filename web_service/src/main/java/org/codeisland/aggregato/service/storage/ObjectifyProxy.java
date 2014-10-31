package org.codeisland.aggregato.service.storage;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

/**
 * A proxy for the normal {@link com.googlecode.objectify.ObjectifyService},
 *  which takes care of setting up everything needed for the Database.
 * @author Lukas Knuth
 * @version 1.0
 */
public class ObjectifyProxy {

    /**
     * Register all Entities.
     */
    static {
        factory().register(Series.class);
        factory().register(Episode.class);
        factory().register(News.class);
        factory().register(Watchlist.class);
    }

    public static Objectify ofy(){
        return ObjectifyService.ofy();
    }

    public static ObjectifyFactory factory(){
        return ObjectifyService.factory();
    }
}
