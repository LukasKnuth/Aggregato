package org.codeisland.aggregato.service.storage.components;

import com.googlecode.objectify.annotation.Ignore;
import org.codeisland.aggregato.service.storage.Mergeable;

import javax.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * <p>This component stores a date that represents the publication of something.</p>
 * <p>Therefor, the {@link #merge(PublicationDateComponent)}-method will always change
 *  the date, if the other one is valid and differs.</p>
 * @author Lukas Knuth
 * @version 1.0
 */
public class PublicationDateComponent
        extends EntityComponent implements Mergeable<PublicationDateComponent>{

    @Ignore
    public static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    private String date;

    private PublicationDateComponent(){};
    public PublicationDateComponent(@Nullable Date date){
        if (date != null) {
            this.date = FORMAT.format(date);
        }
    }

    @Override
    public boolean merge(PublicationDateComponent other) {
        if (other.date != null && !other.date.equals(this.date)){
            this.date = other.date;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PublicationDateComponent that = (PublicationDateComponent) o;

        if (date != null ? !date.equals(that.date) : that.date != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return date != null ? date.hashCode() : 0;
    }

    public Date get(){
        if (this.date == null){
            return null;
        } else {
            try {
                return FORMAT.parse(this.date);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void set(@Nullable Date date){
        if (date != null) {
            this.date = FORMAT.format(date);
        }
    }
}
