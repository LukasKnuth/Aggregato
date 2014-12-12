package org.codeisland.aggregato.service.fetcher.impl;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import org.codeisland.aggregato.service.fetcher.SeriesFetcher;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Season;
import org.codeisland.aggregato.service.storage.Series;
import org.codeisland.aggregato.service.util.CloudStorage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class TMDBFetcher implements SeriesFetcher {

    // TODO Add a ConnectionPool or keep connections alive, so we don't have to open one for every request.

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final Logger logger = Logger.getLogger(TMDBFetcher.class.getName());

    private static final String BASE_URL = "http://api.themoviedb.org/";
    private static final String API_KEY = "f3737c9013174480c625c67f4d84d741";
    private static final String API_VERSION = "3";
    private static final int STATUS_CODE_OK = 1;

    private static final String IDENTIFIER_KEY = "TMDB";

    private static MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    /**
     * <p>Load a json object from the given {@code URL}.</p>
     * <p>The returned object can be checked via {@code instanceof} to see id it's an object or an array.</p>
     * @throws IOException if there was a problem establishing the connection or reading from
     *  it (wrong URL?)
     */
    private static Object jsonFromUrl(URL url) throws IOException {
        InputStream in = null;
        try {
            in = url.openStream();
            // TODO Maybe we should check for errors here? (https://www.themoviedb.org/documentation/api/status-codes)
            return new JSONTokener(in).nextValue();
        } finally {
            if (in != null){
                in.close();
            }
        }
    }

    /**
     * Get an TMDB id from the given series. If non is currently present, an ID will be found by
     *  querying the API.
     */
    private static int getTmdbId(Series series){
        try {
            return Integer.parseInt(series.optIdentifier(IDENTIFIER_KEY, ""));
        } catch (NumberFormatException e){
            int id = findSeries(series.getName());
            series.putIdentifier(IDENTIFIER_KEY, String.valueOf(id));
            return id;
        }
    }

    private static BlobKey storeImage(Series series, String partial_url, CloudStorage.ImageType type){
        return CloudStorage.saveImage(getFullImageUrl(partial_url, type), type, series);
    }

    private static BlobKey storeImage(Season season, String partial_url, CloudStorage.ImageType type){
        return CloudStorage.saveImage(getFullImageUrl(partial_url, type), type, season);
    }

    private static String getFullImageUrl(String partial_url, CloudStorage.ImageType type){
        String size = "";
        switch (type){
            case BACKDROP:
                size = "w780";
                break;
            case POSTER:
                size = "w1280";
                break;
        }
        return getImageBaseURL()+"/"+size+partial_url;
    }

    /**
     * Obtain the current base-url for fetching images from TMDB.
     */
    private static String getImageBaseURL(){
        final String KEY = "TMDB_IMG_BASE_URL";
        if (memcache.contains(KEY)){
            return (String) memcache.get(KEY);
        } else {
            try {
                URL url = new URL(BASE_URL+API_VERSION+"/configuration?api_key="+API_KEY);
                Object json = jsonFromUrl(url);
                if (!(json instanceof JSONObject)){
                    throw new RuntimeException("Expected a JSON Object...");
                }
                JSONObject image_config = ((JSONObject) json).getJSONObject("images");
                String img_base_url = image_config.getString("base_url");
                memcache.put(KEY, img_base_url, Expiration.byDeltaSeconds(60 * 60 * 48)); // 48 hours
                return img_base_url;
            } catch (IOException e) {
                throw new RuntimeException("Couldn't read Config info from API", e);
            }
        }
    }

    /**
     * Parse a date from the given {@code date_str}, which can be {@code null}
     * @return the parsed data, or {@code null}, if {@code date_str} was {@code null} or empty.
     */
    private static Date parseDate(@Nullable String date_str) throws ParseException {
        if (date_str == null || date_str.isEmpty()){
            return null;
        } else {
            return DATE_FORMAT.parse(date_str);
        }
    }

    @Override
    public Series getSeries(String name){
        return getSeriesForId(findSeries(name), true); // Also load images!
    }

    /**
     * Loads a Series </b>without</b> loading any images for it.
     */
    private static Series getSeriesForId(int tmdb_id) {
        return getSeriesForId(tmdb_id, false);
    }

    private static Series getSeriesForId(int tmdb_id, boolean load_images){
        try {
            URL url = new URL(String.format(
                    BASE_URL+API_VERSION+"/tv/%s?api_key="+API_KEY, tmdb_id
            ));
            Object json = jsonFromUrl(url);
            if (json instanceof JSONArray){
                throw new RuntimeException("Expected a JSON Object, got an array...");
            }

            JSONObject series = (JSONObject) json;

            Date first_air_date = DATE_FORMAT.parse(series.getString("first_air_date"));
            Date last_air_date = parseDate(series.optString("last_air_date", null));
            Series s = new Series(series.getString("name"), series.getInt("number_of_seasons"), first_air_date);
            s.setEndDate(last_air_date);
            s.setDescription(series.optString("overview", null));
            s.putIdentifier(IDENTIFIER_KEY, String.valueOf(tmdb_id));

            if (load_images){
                String backdrop_link = series.optString("backdrop_path", null);
                if (backdrop_link != null) {
                    s.setBackdrop(storeImage(s, backdrop_link, CloudStorage.ImageType.BACKDROP));
                }
                String poster_link = series.optString("poster_path", null);
                if (poster_link != null) {
                    s.setPoster(storeImage(s, poster_link, CloudStorage.ImageType.POSTER));
                }
            }
            return s;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException("Could not parse the first_air_date to a Date...", e);
        }
    }

    /**
     * Returns a list of TMDB id's for the given name, in the order they we're returned by the API.
     */
    private static int findSeries(String name) {
        try {
            URL url = new URL(String.format(
                    BASE_URL+API_VERSION+"/search/tv?query=%s&api_key="+API_KEY, URLEncoder.encode(name, "UTF-8")
            ));

            Object json = jsonFromUrl(url);
            if (json instanceof JSONArray){
                throw new RuntimeException("Expected a JSON Object, got an array...");
            }

            JSONObject result_object = (JSONObject) json;
            JSONArray results = result_object.getJSONArray("results");

            /*List<Integer> ids = new ArrayList<>(results.length());
            for (int i = 0; i < results.length(); i++){
                ids.add( results.getJSONObject(i).getInt("id") );
            }
            return ids;*/

            int BEST_MATCH_FOUND = 0; // TODO How do we decide here?
            return results.getJSONObject(BEST_MATCH_FOUND).getInt("id");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Season> getSeasons(Series series) {
        List<Season> all_seasons = new ArrayList<>();
        int tmdb_id = getTmdbId(series);
        try {
            for (int season_nr = 0; season_nr <= series.getSeasonCount(); season_nr++){
                URL url = new URL(String.format(
                        BASE_URL+API_VERSION+"/tv/%s/season/%s?api_key="+API_KEY, tmdb_id, season_nr
                ));
                Object json = jsonFromUrl(url);
                if (json instanceof JSONArray){
                    throw new RuntimeException("Expected a JSON Object, got an array...");
                }

                JSONObject season = (JSONObject) json;

                if (season.optInt("status_code", STATUS_CODE_OK) != STATUS_CODE_OK){
                    logger.info(String.format("A season %s does not exist for %s", season_nr, series.getName()));
                    continue;
                }

                Date season_air_date = parseDate(season.optString("air_date", null));
                Season current_season = new Season(series, season.getString("name"), season.getInt("season_number"), season_air_date);
                String poster_link = season.optString("poster_path", null);
                if (poster_link != null) {
                    current_season.setPoster(storeImage(current_season, poster_link, CloudStorage.ImageType.POSTER));
                }
                all_seasons.add(current_season);

                if (season.has("episodes")){
                    JSONArray episodes = season.getJSONArray("episodes");
                    for (int i = 0; i < episodes.length(); i++){
                        JSONObject episode = episodes.getJSONObject(i);

                        Date air_date = parseDate(episode.optString("air_date", null));

                        current_season.putEpisode(new Episode(
                                current_season, episode.getString("name"),
                                episode.getInt("episode_number"), episode.getInt("season_number"),
                                air_date, episode.optString("overview", null)
                        ));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            throw new RuntimeException("Could not parse season or episode air_date", e);
        }
        return all_seasons;
    }

    @Override
    public boolean update(Series series) {
        int series_tmdb_id = getTmdbId(series);
        boolean load_images = (series.getPoster() == null || series.getBackdrop() == null);
        Series newest = getSeriesForId(series_tmdb_id, load_images);
        boolean was_modified = series.merge(newest);

        Map<Integer, Season> changedSeasons = getChangedSeasons(series);
        if (!changedSeasons.isEmpty()){
            for (Map.Entry<Integer, Season> season : changedSeasons.entrySet()) {
                // Now, check the episodes:
                List<Episode> changed_episodes = getChangedEpisodes(season.getValue(), season.getKey(), series_tmdb_id);
                for (Episode episode : changed_episodes) {
                    season.getValue().putEpisode(episode);
                }
                // Save the changed season.
                series.putSeason(season.getValue());
            }
            was_modified = true;
        }
        return was_modified;
    }

    /**
     * Get information on any season-level changes for the Series TMDB-ID.
     */
    private static Map<Integer, Season> getChangedSeasons(Series series){
        Map<Integer, Season> changed_seasons = new HashMap<>();
        try {
            URL url = new URL(String.format(BASE_URL + API_VERSION + "/tv/%s/changes?api_key=" + API_KEY, getTmdbId(series)));
            Object resp = jsonFromUrl(url);
            if (resp instanceof JSONArray) {
                throw new RuntimeException("Expected a JSON Object, got an array...");
            }
            JSONObject json = (JSONObject) resp;

            JSONArray changes = json.getJSONArray("changes");
            for (int i = 0; i < changes.length(); i++) {
                JSONObject change = changes.getJSONObject(i);

                if (change.getString("key").equals("season")) {
                    // Something changed in a particular season
                    JSONArray items = change.getJSONArray("items");

                    for (int j = 0; j < items.length(); j++) {
                        JSONObject season_change = items.getJSONObject(j).getJSONObject("value");

                        int season_nr = season_change.getInt("season_number");
                        boolean fetch_poster = true;
                        Season old_season = series.getSeason(season_nr);
                        if (old_season != null){
                            // If this season is already in the Database and has no Poster, fetch it!
                            fetch_poster = old_season.getPoster() == null;
                        }
                        changed_seasons.put(
                                season_change.getInt("season_id"),
                                getSeason(series, season_nr, fetch_poster)
                        );
                    }

                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return changed_seasons;
    }

    private static List<Episode> getChangedEpisodes(Season season, int season_tmdb_id, int series_tmdb_id){
        List<Episode> changed_episodes = new ArrayList<>();
        try {
            URL url = new URL(String.format(
                    String.format(BASE_URL + API_VERSION + "/tv/season/%s/changes?api_key=" + API_KEY, season_tmdb_id)
            ));
            Object json = jsonFromUrl(url);
            if (!(json instanceof JSONObject)){
                throw new RuntimeException("Expected a JSON Object...");
            }
            JSONArray changes = ((JSONObject) json).getJSONArray("changes");
            JSONObject change;
            for (int i = 0; i < changes.length(); i++){
                change = changes.getJSONObject(i);
                if (change.getString("key").equals("episode")){
                    // Episodes changed:
                    JSONArray items = change.getJSONArray("items");
                    for (int j = 0; j < items.length(); j++){
                        JSONObject episode_value = items.getJSONObject(j).getJSONObject("value");
                        Episode episode = getEpisode(
                                series_tmdb_id, season, episode_value.getInt("episode_number")
                        );
                        changed_episodes.add(episode);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return changed_episodes;
    }

    private static Season getSeason(Series series, int season_nr, boolean fetch_poster){
        int series_tmdb_id = getTmdbId(series);
        try {
            URL url = new URL(String.format(
                    String.format(BASE_URL + API_VERSION + "/tv/%s/season/%s?api_key=" + API_KEY, series_tmdb_id, season_nr)
            ));
            Object json = jsonFromUrl(url);
            if (!(json instanceof JSONObject)){
                throw new RuntimeException("Expected a JSON Object...");
            }
            JSONObject season = (JSONObject) json;

            Date season_air_date = parseDate(season.optString("air_date", null));
            Season s = new Season(series, season.getString("name"), season.getInt("season_number"), season_air_date);

            if (fetch_poster){
                String poster_link = season.optString("poster_path", null);
                if (poster_link != null) {
                    s.setPoster(storeImage(s, poster_link, CloudStorage.ImageType.POSTER));
                }
            }
            return s;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException("Couldn't parse Season air_date", e);
        }
    }

    private static Episode getEpisode(int series_tmdb_id, Season season, int episode_nr){
        try {
            URL url = new URL(String.format(
                    String.format(BASE_URL + API_VERSION + "/tv/%s/season/%s/episode/%s?api_key=" + API_KEY,
                            series_tmdb_id, season.getSeasonNr(), episode_nr
                    )
            ));
            Object json = jsonFromUrl(url);
            if (!(json instanceof JSONObject)){
                throw new RuntimeException("Expected a JSON Object...");
            }
            JSONObject episode = (JSONObject) json;

            Date air_date = parseDate(episode.optString("air_date", null));
            return new Episode(
                    season, episode.getString("name"), episode_nr, season.getSeasonNr(),
                    air_date, episode.getString("overview")
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException("Couldn't parse Season air_date", e);
        }
    }
}