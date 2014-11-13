package org.codeisland.aggregato.service.fetcher.impl;

import org.codeisland.aggregato.service.fetcher.SeriesFetcher;
import org.codeisland.aggregato.service.storage.Episode;
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

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final String BASE_URL = "http://api.themoviedb.org/";
    private static final String API_KEY = "f3737c9013174480c625c67f4d84d741";
    private static final String API_VERSION = "3";

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

    @Override
    public Series getSeries(String name){
        List<Integer> ids = findSeries(name);
        String series_id = String.valueOf(ids.get(0)); // TODO How do we decide here?

        try {
            URL url = new URL(String.format(
                    BASE_URL+API_VERSION+"/tv/%s?api_key="+API_KEY, series_id
            ));
            Object json = jsonFromUrl(url);
            if (json instanceof JSONArray){
                throw new RuntimeException("Expected a JSON Object, got an array...");
            }

            JSONObject series = (JSONObject) json;

            return new Series(
                    series.getString("name"), series.getInt("number_of_seasons"), series_id
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a list of TMDB id's for the given name, in the order they we're returned by the API.
     */
    private List<Integer> findSeries(String name) {
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
            List<Integer> ids = new ArrayList<>(results.length());

            for (int i = 0; i < results.length(); i++){
                ids.add( results.getJSONObject(i).getInt("id") );
            }
            return ids;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Episode> getEpisodes(Series series) {
        List<Episode> all_episodes = new ArrayList<>();
        try {
            // Start with season 1, since 0 is usually the specials...
            for (int season_nr = 1; season_nr <= series.getSeasons(); season_nr++){
                URL url = new URL(String.format(
                        BASE_URL+API_VERSION+"/tv/%s/season/%s?api_key="+API_KEY, series.getTmdbId(), season_nr
                ));
                Object json = jsonFromUrl(url);
                if (json instanceof JSONArray){
                    throw new RuntimeException("Expected a JSON Object, got an array...");
                }

                JSONObject season = (JSONObject) json;
                if (season.has("episodes")){
                    JSONArray episodes = season.getJSONArray("episodes");
                    for (int i = 0; i < episodes.length(); i++){
                        JSONObject episode = episodes.getJSONObject(i);

                        Date air_date = null;
                        try {
                            air_date = DATE_FORMAT.parse(episode.getString("air_date"));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        all_episodes.add(new Episode(
                                series, episode.getString("name"),
                                episode.getInt("episode_number"), episode.getInt("season_number"),
                                air_date, episode.getString("overview")
                        ));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return all_episodes;
    }
}
