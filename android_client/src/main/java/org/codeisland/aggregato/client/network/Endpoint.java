package org.codeisland.aggregato.client.network;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.gson.GsonFactory;
import org.codeisland.aggregato.client.Login;
import org.codeisland.aggregato.tvseries.tvseries.Tvseries;
import org.codeisland.aggregato.tvseries.tvseries.model.Season;
import org.codeisland.aggregato.tvseries.tvseries.model.Series;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * <p>Holds the shared {@link org.codeisland.aggregato.tvseries.tvseries.Tvseries}-instance for
 *  <b>the entire Application</b>.</p>
 * <p>Also provides static helper methods to deal with endpoint results.</p>
 * @author Lukas Knuth
 * @version 1.0
 */
public class Endpoint {

    private static final String SEASON_KEY = "__season_bundled";
    private static final String SERIES_KEY = "__series_bundled";
    private static final String PREF_ACCOUNT_FILE = "AggregatoGoogleAccountPref";
    private static final String PREF_ACCOUNT_NAME = "AggregatoGoogleAccountName";
    public static final int ACCOUNT_SELECT_REQUEST = 141;

    private static final Tvseries tv_api;
    static {
        Tvseries.Builder builder = new Tvseries.Builder(
                new NetHttpTransport(), new GsonFactory(), null
        );
        tv_api = builder.build();
    }

    private Endpoint(){}

    /**
     * This is the preferred way of getting a {@link org.codeisland.aggregato.tvseries.tvseries.Tvseries.SeriesAPI}-
     *  instance, since this enables connection sharing, caching and more.
     */
    public static Tvseries.SeriesAPI getTvAPI(){
        return tv_api.seriesAPI();
    }

    /**
     * <p>Get a {@link org.codeisland.aggregato.tvseries.tvseries.Tvseries.SeriesAPI} to make authenticated requests
     *  to the Endpoint API.</p>
     * <p>If the user is currently <b>not</b> logged in, this method will start the {@link org.codeisland.aggregato.client.Login}
     *  -Activity to prompt the user. Your job is to override the
     *  {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}-method and check the {@code resultCode}.</p>
     * <p>If the {@code resultCode} is {@link android.app.Activity#RESULT_OK}, call this method again to obtain the authenticated
     *  API. If the {@code resultCode} is {@link android.app.Activity#RESULT_CANCELED}, the user canceled the login and no
     *  authenticated actions may be performed.</p>
     * @return a usable authenticated API if the user is already logged in, or {@code null} if not.
     */
    public static Tvseries.SeriesAPI getAuthenticatedTvAPI(Activity context){
        GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(
                context, "server:client_id:203492774524-i3okn41qjlugu6kt72ui2pdftveb25sm.apps.googleusercontent.com"
        );
        SharedPreferences pref = context.getSharedPreferences(PREF_ACCOUNT_FILE, Context.MODE_PRIVATE);
        String account_name = pref.getString(PREF_ACCOUNT_NAME, null);
        if (account_name != null){
            // Logged in, everything is good:
            credential.setSelectedAccountName(account_name);
            Tvseries.Builder builder = new Tvseries.Builder(
                    new NetHttpTransport(), new GsonFactory(), credential
            );
            return builder.build().seriesAPI();
        } else {
            // Not logged in, show Activity!
            Intent login_activity = new Intent(context, Login.class);
            login_activity.putExtra(Login.ACCOUNT_PICKER_KEY, credential.newChooseAccountIntent());
            context.startActivityForResult(login_activity, ACCOUNT_SELECT_REQUEST);
            return null;
        }
    }

    /**
     * <b>Normally, there is no reason to call this method yourself!.</b>
     */
    public static void setSelectedAccountName(String accountName, Login context){
        SharedPreferences preferences = context.getSharedPreferences(PREF_ACCOUNT_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_ACCOUNT_NAME, accountName);
        editor.apply();
    }

    // ---------------- HELPER ------------------------

    /**
     * Create a {@link android.os.Bundle} containing a given Season.
     * @see #extractSeason(android.os.Bundle)
     * @return the bundle holding the season, or {@code null} if there was a problem.
     */
    public static Bundle bundleSeason(Season season){
        try {
            Bundle bundle = new Bundle(1);
            bundle(SEASON_KEY, season, bundle);
            return bundle;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extract a {@link org.codeisland.aggregato.tvseries.tvseries.model.Season} (which was previously
     *  put there) from a bundle.
     * @return the extracted season, or {@code null}, if there was a problem.
     * @see #bundleSeason(org.codeisland.aggregato.tvseries.tvseries.model.Season)
     */
    public static Season extractSeason(Bundle bundle){
        try {
            return extract(SEASON_KEY, Season.class, bundle);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create a {@link android.os.Bundle} containing a given Series.
     * @see #extractSeason(android.os.Bundle)
     * @return the bundle holding the series, or {@code null} if there was a problem.
     */
    public static Bundle bundleSeries(Series series){
        try {
            Bundle bundle = new Bundle(1);
            bundle(SERIES_KEY, series, bundle);
            return bundle;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extract a {@link org.codeisland.aggregato.tvseries.tvseries.model.Series} (which was previously
     *  put there) from a bundle.
     * @return the extracted series, or {@code null}, if there was a problem.
     * @see #bundleSeries(org.codeisland.aggregato.tvseries.tvseries.model.Series)
     */
    public static Series extractSeries(Bundle bundle){
        try {
            return extract(SERIES_KEY, Series.class, bundle);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void bundle(String key, GenericJson obj, Bundle bundle) throws IOException {
        String json = tv_api.getJsonFactory().toString(obj);
        bundle.putString(key, json);
    }
    private static <T> T extract(String key, Class<T> clazz, Bundle bundle) throws IOException, NoSuchElementException {
        if (bundle.containsKey(key)){
            return tv_api.getJsonFactory().fromString(bundle.getString(key), clazz);
        } else {
            throw new NoSuchElementException("No element with key '"+key+"' found in the given Bundle!");
        }
    }

}
