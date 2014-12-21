package org.codeisland.aggregato.service.storage;

import com.google.api.server.spi.config.Nullable;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.*;

import java.util.*;

import static org.codeisland.aggregato.service.storage.ObjectifyProxy.ofy;

/**
 * <p>A list of {@link org.codeisland.aggregato.service.storage.Episode}s that a given user
 *  wants to watch in the future.</p>
 * <p>Entries are either manually added to the list or automatically, if the user subscribed
 *  to a series.</p>
 * @author Lukas Knuth
 * @version 1.0
 */
@Entity
@Cache
public class Watchlist {

    private @Id @Index String user_id;
    private @Load Set<Ref<Episode>> watchlist = new HashSet<>();
    private List<String> gcm_ids = new ArrayList<>();

    private Watchlist(){} // Objectify needs this, protection doesn't madder!
    public Watchlist(User user){
        this(user.getEmail());
    }
    public Watchlist(String user_email){
        // TODO the user E-Mail can change, but ID doesn't work: https://code.google.com/p/googleappengine/issues/detail?id=8848
        this.user_id = user_email;
    }

    public Set<Episode> getWatchlist() {
        Collection<Episode> wlist = ofy().load().refs(this.watchlist).values();
        return new HashSet<>(wlist);
    }

    public String getId(){
        return this.user_id;
    }

    /**
     * Adds an episode to the watchlist. Duplicate items are ignored.
     * @return whether the watchlist was modified (the Episode was not already on the list) or not.
     */
    public boolean addItem(Episode episode){
        // TODO Store if this was added manually or automatically (for display-order on devices) ??
        return this.watchlist.add(Ref.create(episode));
    }

    /**
     * Remove the given episode from the watchlist. If the episode was not present in the set,
     *  the call is ignored.
     * @return whether the watchlist was modified (the Episode was on the list) or not.
     */
    public boolean removeItem(Episode episode){
        return this.watchlist.remove(Ref.create(episode));
    }

    public void deleteGcmId(String id){
        this.gcm_ids.remove(id);
    }

    public void updateGcmId(@Nullable String old_id, String new_id){
        if (old_id == null){
            // Just insert:
            this.gcm_ids.add(new_id);
        } else {
            // Update the id:
            int i = this.gcm_ids.indexOf(old_id);
            if (i == -1){
                this.gcm_ids.add(new_id);
            } else {
                this.gcm_ids.set(i, new_id);
            }
        }
    }

    public List<String> getGcmIds(){
        return this.gcm_ids;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Watchlist watchlist = (Watchlist) o;

        if (!user_id.equals(watchlist.user_id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return user_id.hashCode();
    }
}
