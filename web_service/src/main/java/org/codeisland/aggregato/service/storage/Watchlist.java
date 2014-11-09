package org.codeisland.aggregato.service.storage;

import com.google.appengine.api.users.User;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>A list of {@link org.codeisland.aggregato.service.storage.Episode}s that a given user
 *  wants to watch in the future.</p>
 * <p>Entries are either manually added to the list or automatically, if the user subscribed
 *  to a series.</p>
 * @author Lukas Knuth
 * @version 1.0
 */
@Entity
public class Watchlist {

    private @Id @Index String user_id;
    private Set<Episode> watchlist = new HashSet<>();

    private Watchlist(){} // Objectify needs this, protection doesn't madder!
    public Watchlist(User user){
        this(user.getUserId());
    }
    public Watchlist(String user_id){
        this.user_id = user_id;
    }

    public Set<Episode> getWatchlist() {
        return watchlist;
    }

    /**
     * Adds an episode to the watchlist. Duplicate items are ignored.
     */
    public void addItem(Episode episode){
        // TODO Store if this was added manually or automatically (for display-order on devices) ??
        this.watchlist.add(episode);
    }

    /**
     * Remove the given episode from the watchlist. If the episode was not present in the set,
     *  the call is ignored.
     */
    public void removeItem(Episode episode){
        this.watchlist.remove(episode);
    }
}
