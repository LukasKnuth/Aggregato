package org.codeisland.aggregato.service.util;

import com.google.android.gcm.server.*;
import com.google.api.server.spi.config.Nullable;
import org.codeisland.aggregato.service.storage.Watchlist;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class CloudMessaging {

    private static final Logger logger = Logger.getLogger(CloudMessaging.class.getName());
    private static final Sender sender = new Sender("AIzaSyApYMUf_hSlEVGUgpxfWWxXwvLR3Yfjk4Y");

    private static final int RETRY_COUNT = 2;

    private static final String TYPE_KEY = "type";
    private static final String COLLAPSE_WATCHLIST = "watchlist_update";

    /**
     * Represents an operation that is to be executed, after messages have been successfully send.
     */
    private static class Operation{
        private enum Type{RESENT, DELETE, UPDATE}
        public final Type type;
        public final String value;
        private Operation(Type type, @Nullable String value) {
            this.type = type;
            this.value = value;
        }
    }

    private CloudMessaging(){}

    /**
     * Notify all owners (if a GCM was registered) of the given watchlists about new entries.
     * @return all watchlists, that had their GCM id's updated. The caller is obligated to persist
     *  these changes himself!
     */
    public static Collection<Watchlist> notifyWatchlistUpdated(Collection<Watchlist> updated_watchlists){
        // Build the message:
        Message.Builder message_builder = new Message.Builder();
        message_builder.addData(TYPE_KEY, COLLAPSE_WATCHLIST);
        message_builder.collapseKey(COLLAPSE_WATCHLIST)
                .delayWhileIdle(false)
                .timeToLive((60 * 60 * 24)); // Lives for 24h

        // Get a GCM-ID -> Watchlist mapping:
        Map<String, Watchlist> gcm_to_watchlist = new HashMap<>();
        for (Watchlist watchlist : updated_watchlists){
            for (String gcm_id : watchlist.getGcmIds()) {
                gcm_to_watchlist.put(gcm_id, watchlist);
            }
        }
        if (gcm_to_watchlist.size() > 0){
            // Send the message:
            List<String> gcm_ids = new ArrayList<>(gcm_to_watchlist.keySet());
            Map<String, Operation> operations = notifyDevices(gcm_ids, message_builder.build());

            // Perform any Updates (if necessary):
            List<Watchlist> modified_lists = new LinkedList<>();
            for (Map.Entry<String, Operation> op : operations.entrySet()) {
                Watchlist watchlist = gcm_to_watchlist.get(op.getKey());
                switch (op.getValue().type){
                    case DELETE:
                        watchlist.deleteGcmId(op.getKey());
                        modified_lists.add(watchlist);
                        break;
                    case RESENT:
                        // TODO Resent this message...
                        break;
                    case UPDATE:
                        watchlist.updateGcmId(op.getKey(), op.getValue().value);
                        modified_lists.add(watchlist);
                }
            }
            return modified_lists;
        }
        return Collections.emptyList();
    }

    /**
     * Send the given message to the given GCM ids.
     * @return a given GCM-ID -> Operation mapping, informing the caller about actions he should
     *  perform on the original {@code gcm_ids}.
     */
    private static Map<String, Operation> notifyDevices(List<String> gcm_ids, Message message){
        try {
            MulticastResult send_result = sender.send(message, gcm_ids, RETRY_COUNT);
            if (send_result.getCanonicalIds() != 0 || send_result.getFailure() != 0){
                // Something went (partially) wrong
                Map<String, Operation> operations = new HashMap<>(
                        send_result.getFailure()+send_result.getCanonicalIds()
                );
                int i = 0;
                for (Result res : send_result.getResults()) {
                    if (res.getMessageId() != null && res.getCanonicalRegistrationId() != null){
                        // New registration ID for this device!
                        operations.put(gcm_ids.get(i), new Operation(Operation.Type.UPDATE, res.getCanonicalRegistrationId()));
                    } else {
                        if (Constants.ERROR_NOT_REGISTERED.equals(res.getErrorCodeName())){
                            // App was uninstalled, remove this gcm-id:
                            operations.put(gcm_ids.get(i), new Operation(Operation.Type.DELETE, null));
                        } else if (Constants.ERROR_UNAVAILABLE.equals(res.getErrorCodeName())){
                            // Retry sending...
                            operations.put(gcm_ids.get(i), new Operation(Operation.Type.RESENT, null));
                        } else {
                            // "Something" is wrong the the ID. Remove it
                            operations.put(gcm_ids.get(i), new Operation(Operation.Type.DELETE, null));
                        }
                    }
                    i++;
                }
                return operations;
            }
        } catch (InvalidRequestException e){
            if (e.getHttpStatusCode() == 400){
                logger.log(Level.SEVERE, "Malformed JSON request send", e);
            } else if (e.getHttpStatusCode() == 401){
                logger.log(Level.SEVERE, "Authentication error (wrong API key?)", e);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to send GCM messages!", e);
        }
        return Collections.emptyMap();
    }
}
