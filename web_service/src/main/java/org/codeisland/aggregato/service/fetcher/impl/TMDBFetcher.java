package org.codeisland.aggregato.service.fetcher.impl;

import com.google.appengine.repackaged.com.google.common.primitives.Ints;
import org.codeisland.aggregato.service.fetcher.SeriesFetcher;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Season;
import org.codeisland.aggregato.service.storage.Series;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class TMDBFetcher implements SeriesFetcher {

    // TODO Add a ConnectionPool or keep connections alive, so we don't have to open one for every request.

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final String BASE_URL = "http://api.themoviedb.org/";
    private static final String API_KEY = "f3737c9013174480c625c67f4d84d741";
    private static final String API_VERSION = "3";

    private static final String IDENTIFIER_KEY = "TMDB";

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
        Integer tmdb_id = Ints.tryParse(series.optIdentifier(IDENTIFIER_KEY, ""));
        if (tmdb_id == null){
            int id = findSeries(series.getName());
            series.putIdentifier(IDENTIFIER_KEY, String.valueOf(id));
            return id;
        }
        return tmdb_id;
    }

    @Override
    public Series getSeries(String name){
        return getSeriesForId(findSeries(name));
    }

    private static Series getSeriesForId(int tmdb_id){
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
            Series s = new Series(series.getString("name"), series.getInt("number_of_seasons"), first_air_date);
            s.putIdentifier(IDENTIFIER_KEY, String.valueOf(tmdb_id));
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
            // Start with season 1, since 0 is usually the specials...
            for (int season_nr = 1; season_nr <= series.getSeasonCount(); season_nr++){
                URL url = new URL(String.format(
                        BASE_URL+API_VERSION+"/tv/%s/season/%s?api_key="+API_KEY, tmdb_id, season_nr
                ));
                Object json = jsonFromUrl(url);
                if (json instanceof JSONArray){
                    throw new RuntimeException("Expected a JSON Object, got an array...");
                }

                JSONObject season = (JSONObject) json;

                Date season_air_date = DATE_FORMAT.parse(season.getString("air_date"));
                Season current_season = new Season(series, season.getString("name"), season.getInt("season_number"), season_air_date);
                all_seasons.add(current_season);

                if (season.has("episodes")){
                    JSONArray episodes = season.getJSONArray("episodes");
                    for (int i = 0; i < episodes.length(); i++){
                        JSONObject episode = episodes.getJSONObject(i);

                        Date air_date = DATE_FORMAT.parse(episode.getString("air_date"));

                        current_season.putEpisode(new Episode(
                                current_season, episode.getString("name"),
                                episode.getInt("episode_number"), episode.getInt("season_number"),
                                air_date, episode.getString("overview")
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
}
