package org.codeisland.aggregato.service.storage.components;

import org.codeisland.aggregato.service.storage.Mergeable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This component stores one or multiple key-value identifiers.
 * @author Lukas Knuth
 * @version 1.0
 */
public class IdentifierComponent extends EntityComponent implements Mergeable<IdentifierComponent> {

    private static final Logger logger = Logger.getLogger(IdentifierComponent.class.getName());

    private Map<String, String> identifiers = new HashMap<>();

    public IdentifierComponent(){}

    @Override
    public boolean merge(IdentifierComponent other) {
        boolean was_modified = false;
        if (!this.identifiers.equals(other.identifiers)){
            for (Map.Entry<String, String> id : other.identifiers.entrySet()) {
                if (this.identifiers.containsKey(id.getKey())){
                    // Key is already there, check the values!
                    if (!this.identifiers.get(id.getKey()).equals(id.getValue())){
                        // Values are different!
                        logger.warning(String.format(
                                "Identifiers differ for key '%s': Mine is '%s', Other is '%s'",
                                id.getKey(), this.identifiers.get(id.getKey()), id.getValue()
                        ));
                        // TODO Maybe do some basic checking (if mine is empty or null and other isn't...)
                    }
                } else {
                    // Key is not in our map...
                    this.identifiers.put(id.getKey(), id.getValue());
                    was_modified = true;
                }
            }
        }
        return was_modified;
    }

    public String opt(String key, String default_value){
        if (identifiers.containsKey(key)){
            return identifiers.get(key);
        } else {
            return default_value;
        }
    }

    public void put(String key, String identifier){
        this.identifiers.put(key, identifier);
    }
}
