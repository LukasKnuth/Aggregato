package org.codeisland.aggregato.service.fetcher.impl;

import org.codeisland.aggregato.service.fetcher.SeriesFetcher;
import org.codeisland.aggregato.service.storage.Episode;
import org.codeisland.aggregato.service.storage.Series;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
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

    @Override
    public Series getSeries(String name){
        try {
            URL url = new URL(String.format(
                    BASE_URL+API_VERSION+"/search/tv?query=%s&api_key="+API_KEY, URLEncoder.encode(name, "UTF-8")
            ));
            Object json = new JSONTokener(url.openStream()).nextValue();
            if (json instanceof JSONArray){
                throw new RuntimeException("Expected a JSON Object, got an array...");
            }

            JSONObject results = (JSONObject) json;
            if (results.getInt("total_results") > 0){
                JSONArray series = results.getJSONArray("results");
                // TODO How do we decide here? Also, store the ID somewhere??
                JSONObject first = series.getJSONObject(0);
                return new Series(first.getString("name"), 0, String.valueOf(first.getInt("id")) );
            } else {
                return null;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Episode> getEpisodes(Series series) {
        List<Episode> all_episodes = new ArrayList<>();
        try {
            URL url = new URL(String.format(
                    BASE_URL+API_VERSION+"/tv/%s/season/%s?api_key="+API_KEY, series.getTmdbId(), "1"
            ));
            Object json = new JSONTokener(url.openStream()).nextValue();
            if (json instanceof JSONArray){
                throw new RuntimeException("Expected a JSON Object, got an array...");
            }

            JSONObject seasons = (JSONObject) json;
            JSONArray episodes = seasons.getJSONArray("episodes");
            for (int i = 0; i < episodes.length(); i++){
                JSONObject episode = episodes.getJSONObject(i);

                Date air_date = null;
                try {
                    air_date = DATE_FORMAT.parse(episode.getString("air_date"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                all_episodes.add(new Episode(
                        series, air_date, episode.getString("name")
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return all_episodes;
    }
}
