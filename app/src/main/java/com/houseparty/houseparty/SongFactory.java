package com.houseparty.houseparty;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * @author Nathan Boyd april 12 2018
 */


public class SongFactory {

    private static SongFactory instance;

    private Song song;

    private interface AsyncCallback {
        void onSuccess(String uri);
    }

    private SongFactory(){}

    public static SongFactory getInstance()
    {
        if (instance == null)
        {
            instance = new SongFactory();
        }
        return instance;

    }

    public Song createSong(final String title, SpotifyService spotify, String api) {
        String songUri = "";

        if ("spotify".equals(api)) {

            spotifySearchForTrack(title, new AsyncCallback() {
                @Override
                public void onSuccess(String uri) {
                    Log.i("SongActivity", "this is the uri: " + uri);
                    song = new SpotifySong(title, uri);
                }
            }, spotify);
        }
        if ("soundcloud".equals(api)) {
            String uri = "";
            song = new SoundCloudSong(title, uri);
        }
        if ("tidal".equals(api)) {
            String uri = "";
            song = new TidalSong(title, uri);
        }
        if ("googleplay".equals(api)) {
            String uri = "";
            song = new GooglePlaySong(title, uri);
        }

        return song;
    }


    boolean spotifySearchForTrack(String query, final AsyncCallback callback, SpotifyService spotify) {

        query = query.replaceAll(" ", "+");


        Map<String, Object> options = new HashMap<String, Object>();
        //options.put("Authorization", accessToken);
        options.put("market", "US");
        options.put("limit", 20);
        try {
            spotify.searchTracks(query, options, new Callback<TracksPager>() {
                @Override
                public void success(TracksPager tracksPager, Response response) {
                    String songUri = "";
                    try {


                        List<Track> searchResults = tracksPager.tracks.items;
                        songUri = searchResults.get(0).uri;
                    } catch (Exception e) {
                        Log.d("SearchTracksInner: ", "No results");
                    }
                    Log.d("FetchSongTask", "1st song uri = " + songUri);
                    callback.onSuccess(songUri);
                }

                @Override
                public void failure(RetrofitError error) {
                    error.printStackTrace();
                }
            });

        } catch (IndexOutOfBoundsException e) {
            Log.d("SEARCHTRACKS", "No results");
        }

        return true;

    }
}