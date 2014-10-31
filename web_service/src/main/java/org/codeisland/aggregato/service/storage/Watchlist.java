package org.codeisland.aggregato.service.storage;

import com.google.appengine.api.users.User;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
@Entity
public class Watchlist {

    private @Id Long key;
    private @Index String user_id;
    private List<Episode> watchlist = new ArrayList<>();

    public Watchlist(){} // Objectify needs this!

    public Watchlist(User user){
        this.user_id = user.getUserId();
    }

    public List<Episode> getWatchlist() {
        return watchlist;
    }

    public void addItem(Episode episode){
        this.watchlist.add(episode);
    }

    public void removeItem(Episode episode){
        this.watchlist.remove(episode);
    }
}
