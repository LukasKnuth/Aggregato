package org.codeisland.aggregato.service.util;

import com.google.api.server.spi.config.Nullable;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.tools.cloudstorage.*;
import com.google.common.io.ByteStreams;
import org.codeisland.aggregato.service.storage.Season;
import org.codeisland.aggregato.service.storage.Series;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Helper-class to interact with the Google Cloud Storage.
 * @author Lukas Knuth
 * @version 1.0
 */
public class CloudStorage {

    private static final ImagesService images_service = ImagesServiceFactory.getImagesService();
    private static final BlobstoreService blobstore = BlobstoreServiceFactory.getBlobstoreService();
    private static final GcsService cloud_storage = GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());
    private static final MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    private static final Logger logger = Logger.getLogger(CloudStorage.class.getName());

    private static final String DEFAULT_GCS_BUCKET = AppIdentityServiceFactory.getAppIdentityService().getDefaultGcsBucketName();

    private CloudStorage(){}

    /*
        The fundamental problem with this approach is, that getServingUrl(BlobKey) is is slow as fuck.
        TODO Store the image-link (an whether it's a real image or a placeholder) in the Entities.
        This comes at the expense of re-creating the entire DB ??
        Maybe make an Image class that holds the link-string or null and implement methods on it like
         needsUpdate() and such, to make decisions in the rest of the code more obvious.
    */

    public enum ImageType{
        POSTER("placeholders/placeholder_poster.png"),
        BACKDROP("placeholders/placeholder_backdrop.png");

        private final String placeholder;
        private ImageType(String placeholder){
            this.placeholder = placeholder;
        }
        private String getCacheKey(){
            return this.toString()+"_"+this.placeholder;
        }
    }

    /**
     * Serve the given image-blob from the Google Cloud Storage via the Image API.
     * @return the url used for serving the given image.
     */
    public static String serveImage(@Nullable BlobKey blob, ImageType type){
        if (blob == null){
            // Return the placeholder instead:
            return getPlaceholderUrl(type);
        } else {
            String cache_key = type.toString()+"_"+blob.getKeyString();
            if (memcache.contains(cache_key)){
                return (String) memcache.get(cache_key);
            } else {
                ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blob);
                String servingUrl = images_service.getServingUrl(options);
                memcache.put(cache_key, servingUrl);
                return servingUrl;
            }
        }
    }

    private static String getPlaceholderUrl(ImageType type){
        if (memcache.contains(type.getCacheKey())){
            return (String) memcache.get(type.getCacheKey());
        } else {
            String placeholder_filename = String.format("/gs/%s/%s", DEFAULT_GCS_BUCKET, type.placeholder);
            ServingUrlOptions options = ServingUrlOptions.Builder.withGoogleStorageFileName(placeholder_filename);
            String serving_url;
            try {
                serving_url = images_service.getServingUrl(options);
                memcache.put(type.getCacheKey(), serving_url);
            } catch (IllegalArgumentException e){
                logger.log(Level.SEVERE,
                        String.format("The placeholder for %s was not valid: '%s'", type, placeholder_filename),
                        e
                );
                serving_url = "";
            }
            return serving_url;
        }
    }

    public static BlobKey saveImage(String url, ImageType type, Series series){
        String image_name = String.format("series/%s_%s", series.getId(), type);
        GcsFilename file = new GcsFilename(DEFAULT_GCS_BUCKET, image_name);
        try {
            return CloudStorage.saveImage(url, file);
        } catch (IOException e) {
            return null;
        }
    }
    public static BlobKey saveImage(String url, ImageType type, Season season){
        String image_name = String.format("season/%s_%s", season.getId(), type);
        GcsFilename file = new GcsFilename(DEFAULT_GCS_BUCKET, image_name);
        try {
            return CloudStorage.saveImage(url, file);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * <p>Stores the image at the given {@code url} to the Cloud Storage and returns
     *  a serve-able {@code BlobKey}.</p>
     * @see #serveImage(com.google.appengine.api.blobstore.BlobKey, org.codeisland.aggregato.service.util.CloudStorage.ImageType)
     */
    private static BlobKey saveImage(String url, GcsFilename file) throws IOException {
        try {
            GcsOutputChannel outputChannel = cloud_storage.createOrReplace(file, GcsFileOptions.getDefaultInstance());
            URL image_url = new URL(url);
            InputStream in = image_url.openStream();
            try {
                ByteStreams.copy(in, Channels.newOutputStream(outputChannel));
                outputChannel.close();

                return blobstore.createGsBlobKey(String.format(
                        "/gs/%s/%s", file.getBucketName(), file.getObjectName()
                ));
            } finally {
                if (in != null) in.close();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, String.format("Couldn't get an image from: %s", url), e);
            throw e;
        }
    }

}
