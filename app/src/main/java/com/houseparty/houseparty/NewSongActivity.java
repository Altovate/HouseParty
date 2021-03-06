package com.houseparty.houseparty;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.houseparty.houseparty.napsterSampleLibrary.Search;
import com.napster.cedar.Napster;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class NewSongActivity extends AppCompatActivity implements
    SpotifyPlayer.NotificationCallback, ConnectionStateCallback {
    private boolean pause = true;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private ArrayList<Song> songs;
    private Map<String, String> songIDs;
    private String songName;

    private static final String CLIENT_ID = "4c6b32bf19e4481abdcfbe77ab6e46c0";
    private static final String REDIRECT_URI = "houseparty-android://callback";
    private static final int REQUEST_CODE = 777;

    private String accessToken;
    public static SpotifyPlayer spotifyPlayer;
    private SpotifyService spotify;

    private String currentImage;

    private String host;
    private FirebaseUser currentUser;  /* Variable to keep track of who I is */

    private FirebaseDatabase sFirebaseDatabase;
    private DatabaseReference songDatabaseReference;
    private ChildEventListener sChildEventListener;

    protected Napster napster;
    public static com.napster.cedar.player.Player napsterPlayer;
    protected com.houseparty.houseparty.napsterSampleLibrary.Metadata metadata;

    @Override
    public void onLoggedIn() {

    }

    @Override
    public void onLoggedOut() {

    }

    @Override
    public void onLoginFailed(Error error) {

    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String s) {

    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {

    }

    @Override
    public void onPlaybackError(Error error) {

    }

    private interface AsyncCallback {
        void onSuccess(String uri);
    }

    private interface NapsterCallback {
        void onSuccess(com.napster.cedar.player.data.Track track);
    }

    private final SpotifyPlayer.OperationCallback mOperationCallback = new SpotifyPlayer.OperationCallback() {
        @Override
        public void onSuccess() {
            Log.d("OperationCallback", "OK!");
        }

        @Override
        public void onError(Error error) {
            Log.d("OperationCallback", "Error: " + error);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_activity_song);

        currentImage = "";

        setUpButton();
        setUpAdapter();

        sFirebaseDatabase = FirebaseDatabase.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        host = getIntent().getExtras().getString("HOST");
        Map<String, String> t = PlaylistActivity.getIdTable();
        String id = t.get(PlaylistActivity.selection());
        songDatabaseReference = sFirebaseDatabase.getReference().child("playlists").child(id).child("songs");
        songIDs = new HashMap<>();

        authenticateUser();

        napster = Napster.register(this, "OTY2ZDkxYTctZDZlYy00MDBkLWE2ZWQtMGQ5YzhhOGIyZjMw", "NTI2NWVjOGYtODY3ZS00YTg5LWIyNWYtZDkyNzY5ODM0Nzcw");
        napsterPlayer = napster.getPlayer();
        metadata = new com.houseparty.houseparty.napsterSampleLibrary.Metadata("OTY2ZDkxYTctZDZlYy00MDBkLWE2ZWQtMGQ5YzhhOGIyZjMw");


        sChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                HashMap<String, String> dataTable = (HashMap) dataSnapshot.getValue();

                songIDs.put(dataTable.get(getString(R.string.title)), dataSnapshot.getKey());

                songs.add(new SpotifySong(
                    dataTable.get(getString(R.string.title)),
                    dataTable.get("uri"),
                    dataTable.get("artist"),
                    accessToken,
                    dataTable.get("coverArtUrl")
                ));

                Collections.sort(songs);

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Collections.sort(songs);

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                HashMap<String, String> dataTable = (HashMap) dataSnapshot.getValue();

                songs.remove(new SpotifySong(
                    dataTable.get("title"),
                    dataTable.get("uri"),
                    dataTable.get("artist")
                ));

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                throw new UnsupportedOperationException();
            }
        };
        songDatabaseReference.addChildEventListener(sChildEventListener);

        ImageView view = findViewById(R.id.imageView2);
        view.setImageResource(R.drawable.ic_launcher_background);
    }

    public void setUpButton() {
        pause = false;
        final Button myButton = findViewById(R.id.button3);
        myButton.setText(R.string.play);
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!pause) {
                    myButton.setText(R.string.play);
                    pause = true;
                    spotifyPlayer.pause(mOperationCallback);
                } else {
                    myButton.setText(R.string.pause);
                    pause = false;
                    spotifyPlayer.resume(mOperationCallback);
                }
            }
        });
    }

    public void dialogueBoxSong(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Song Name:");

        LinearLayout layout = new LinearLayout(this);
        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.song_name_hint);
        layout.addView(input);
        final Spinner selectAPI = new Spinner(this);
        final ArrayList<String> apiList = new ArrayList<String>();
        apiList.add("Spotify");
        apiList.add("Apple Music");
        apiList.add("Napster");
        apiList.add("Soundcloud");
        apiList.add("Youtube");
        apiList.add("Google Play");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, apiList);
        selectAPI.setAdapter(arrayAdapter);
        layout.addView(selectAPI);
        layout.setOrientation(LinearLayout.VERTICAL);
        //layout.setGravity(Gravity.CENTER);

        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                songName = input.getText().toString();
                Log.d("SongActivity: ", songName);
                if (songName.isEmpty()) {
                    dialog.cancel();
                } else {
                    if (selectAPI.getSelectedItem() == "Spotify") {
                        spotifySearchForTrack(songName, new NewSongActivity.AsyncCallback() {
                            @Override
                            public void onSuccess(String uri) {
                                Log.i("SongActivity", "this is the uri: " + uri);
                                Log.i("PushToFirebaseImageURL", currentImage);

                                Song song = new SpotifySong(songName, uri, null, accessToken, currentImage);
                                //Song song = songFactory.createSong(songName, spotify, "spotify");
                                songDatabaseReference.push().setValue(song);
                                Log.i("SongActivity", "This is the song name: " + song.title);

                            }
                        });
                    } else if (selectAPI.getSelectedItem() == "Napster") {
                        napsterSearchForTrack(songName);

                    }
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    boolean napsterSearchForTrack(String query) {
        Log.d("NapsterSearchForTrack", "search for track is called");

        metadata.queryTrack(query, new Callback<Search>() {

            @Override
            public void success(Search search, Response response) {
                try {
                    Log.d("NapsterSearchForTrack", "successful search");
                    Log.d("NapsterSearchForTrack", "tracks: " + search.toString());
                    com.napster.cedar.player.data.Track firstTrack = search.search.get(0);
                    songs.add(new NapsterSong(
                            firstTrack.name,
                            firstTrack.id,
                            firstTrack.artistName,
                            firstTrack,
                            ""
                    ));
                    adapter.notifyDataSetChanged();
                    napsterPlayer.play(search.search.get(0));
                } catch (Exception e) {
                    Log.d("NapsterSearchForTrack", e.getMessage());

                    Log.d("NapsterSearchForTrack", "Failed to play napster song");


                }
            }

            @Override
            public void failure(RetrofitError error) {
            }
        });

        return true;
    }

    boolean spotifySearchForTrack(String query, final NewSongActivity.AsyncCallback callback) {

        query = query.replaceAll(" ", "+");

        Map<String, Object> options = new HashMap<>();
        options.put("market", "US");
        options.put("limit", 20);
        try {
            spotify.searchTracks(query, options, new Callback<TracksPager>() {
                @Override
                public void success(TracksPager tracksPager, Response response) {
                    String songUri = "";
                    String imageURL = "";
                    try {
                        List<Track> searchResults = tracksPager.tracks.items;
                        Log.d("SpotifySearchForTrack", "beginning of try block");
                        songUri = searchResults.get(0).uri;
                        try {
                            String webpageURL = searchResults.get(0).external_urls.get("spotify");
                            currentImage = new RetrieveImage().execute(webpageURL).get();
                            Log.d("AsyncRetrieveImage", currentImage);
                        } catch (Exception e) {
                            Log.d("SpotifySearchForTrack", "JSoup failure");
                            Log.d("SpotifySearchForTrack", e.toString());
                        }

                    } catch (Exception e) {
                        Log.d("SearchTracksInner: ", "No results");
                    }


                    Log.d("FetchSongTask", "1st song uri = " + songUri);
                    //imageURL = searchResults.get(0).external_urls.get("images");
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

    // used in spotifySearchForTrack to scrape web for cover art url
    class RetrieveImage extends AsyncTask<String, Void, String> {

        private Exception exception;
        private String imageURL;

        protected String doInBackground(String... args) {
            //String imageURL = "";
            try {
                Log.d("doInBackgroundArg", args[0]);
                Document doc = Jsoup.connect(args[0]).timeout(10000).get();
                imageURL = doc.select("img").get(1).absUrl("src");
                Log.d("ImageForLoop", imageURL);

            } catch (Exception e) {
                Log.d("Retrieve Image", e.toString());

            }
            return imageURL;
        }
    }

    boolean authenticateUser() {
        // ---------USER AUTHENTICATION----------

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
            AuthenticationResponse.Type.TOKEN,
            REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming", "playlist-modify-public",
            "playlist-modify-private", "playlist-read-collaborative", "user-library-read",
            "user-library-modify", "user-read-playback-state", "user-modify-playback-state",
            "user-read-currently-playing"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(NewSongActivity.this, REQUEST_CODE, request);

        return true;

    }

    public static Drawable LoadImage(String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "src name");
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    public void printSongs() {
        for (Song song : songs) {
            Log.d(getClass().getSimpleName(), song.getTitle());
        }
    }

    public void setUpAdapter() {
        recyclerView = findViewById(R.id.recyclerView);
        songs = new ArrayList<>();

        adapter = new SongAdapter(songs);
        adapter.setOnItemClickListener(new SongAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Song song = songs.get(position);
                authenticateUser();
                songs.set(position, new SpotifySong(song.title, song.uri, song.artist, accessToken, song.coverArtUrl));
                song = songs.get(position);
                song.playSong();
                try {
                    Log.d("CurrentSongCoverURL", song.coverArtUrl);
                    ImageView currentView = findViewById(R.id.imageView2);
                    Picasso.get().load(song.coverArtUrl).into(currentView);
                } catch (Exception e) {
                    Log.d("ImageOnPlay", "Failed to get image");

                }
                Snackbar.make(view, "Now playing: " + song.getTitle(), Snackbar.LENGTH_LONG).show();
                Button myButton = findViewById(R.id.button3);
                myButton.setText(R.string.pause);
                pause = false;
            }
        });

        adapter.setOnThumbClickListener(new SongAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Song song = songs.get(position);
                ImageButton thumbImageButton = view.findViewById(R.id.thumb);
                thumbImageButton.setSelected(!thumbImageButton.isSelected());

                if (thumbImageButton.isSelected()) {
                    song.addThumbsUp(currentUser.getUid());
                } else {
                    song.removeThumbsUp(currentUser.getUid());
                }

                Collections.sort(songs);
                adapter.notifyDataSetChanged();
            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getParent(), 1));

        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Add snap scrolling
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            Drawable background = new ColorDrawable(Color.RED);

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // Don't create dialogbox if the user doesn't own the playlist
                if (!currentUser.getUid().equals(host)) {
                    return;
                }


                final int position = viewHolder.getAdapterPosition();

                final Song song = songs.remove(position);
                adapter.notifyItemRemoved(position);

                Snackbar.make(findViewById(R.id.recyclerView), "1 item removed", Snackbar.LENGTH_LONG)
                    .setAction("undo", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            songs.add(position, song);
                            adapter.notifyItemInserted(position);
                        }
                    })
                    .setActionTextColor(Color.MAGENTA)
                    .addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            if (event == DISMISS_EVENT_TIMEOUT) {
                                songDatabaseReference.child(songIDs.get(song.getTitle())).removeValue();
                            }
                        }
                    })
                    .show();
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState,
                                    boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                // not sure why, but this method get's called for viewholder that are already swiped away
                if (viewHolder.getAdapterPosition() == -1) {
                    // not interested in those
                    return;
                }

                // Don't swipe if the user doesn't own the playlist
                if (!currentUser.getUid().equals(host)) {
                    return;
                }

                // draw red background
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                accessToken = response.getAccessToken();
                Config playerConfig = new Config(NewSongActivity.this, accessToken, CLIENT_ID);
                Spotify.getPlayer(playerConfig, NewSongActivity.this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        NewSongActivity.this.spotifyPlayer = spotifyPlayer;
                        Log.d("SongActivity", "spotifyPlayer initialized.");
                        NewSongActivity.this.spotifyPlayer.addConnectionStateCallback(NewSongActivity.this);
                        NewSongActivity.this.spotifyPlayer.addNotificationCallback(NewSongActivity.this);
                        Log.d("SongActivity", "spotifyPlayer: callbacks added.");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("SongActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
        }
    }