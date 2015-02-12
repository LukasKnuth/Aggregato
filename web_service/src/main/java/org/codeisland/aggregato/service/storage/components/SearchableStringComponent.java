package org.codeisland.aggregato.service.storage.components;

import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import org.codeisland.aggregato.service.storage.Mergeable;

/**
 * Represents a <b>case-insensitive</b>, searchable string.
 * @author Lukas Knuth
 * @version 1.0
 */
public class SearchableStringComponent
        extends EntityComponent implements Mergeable<SearchableStringComponent>{

    private @Index String normalized;
    private @Unindex String display;

    private SearchableStringComponent(){}
    public SearchableStringComponent(String value) {
        this.set(value);
    }

    @Override
    public boolean merge(SearchableStringComponent other) {
        if (!this.display.equals(other.display)){
            this.set(other.display);
            return true;
        } else {
            return false;
        }
    }

    public String get(){
        return this.display;
    }

    public void set(String value){
        this.display = value;
        this.normalized = value.toUpperCase();
    }
}
