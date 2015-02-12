package org.codeisland.aggregato.service.storage.components;

import com.google.appengine.api.blobstore.BlobKey;
import com.googlecode.objectify.annotation.Unindex;
import org.codeisland.aggregato.service.util.CloudStorage;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * <p>A component, that holds a single image, stored via Google Cloud Storage.</p>
 * @author Lukas Knuth
 * @version 1.0
 */
@Unindex
public class ImageComponent extends EntityComponent{

    private BlobKey image;
    private Date last_update;
    private String serving_url;

    private ImageComponent(){}
    public ImageComponent(String serving_url, Date creation, BlobKey image) {
        this.serving_url = serving_url;
        this.last_update = creation;
        this.image = image;
    }

    public static ImageComponent placeholder(CloudStorage.ImageType type){
        return new ImageComponent(
                CloudStorage.serveImage(null, type), new Date(), null
        );
    }

    /**
     * <p>Updates this {@code ImageComponent} with the given one.</p>
     * <p>This is <b>not</b> like a merge, in that it will not check any condition (other than to see
     *  if {@code new_image} is null) and simply update the internal values of this component to reflect
     *  the ones of the given component, since the actual picture should already be changed.</p>
     * @return whether anything was changed internally.
     */
    public boolean update(@Nullable ImageComponent new_image) {
        if (new_image != null && new_image.image != null){
            this.last_update = new_image.last_update;
            this.serving_url = new_image.serving_url;
            this.image = new_image.image;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks, whether this the stored image should be updated.
     */
    public boolean needsUpdate(){
        return (this.image == null);
    }

    public String getServingUrl(){
        return this.serving_url;
    }
}
