package com.houseparty.houseparty;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.houseparty.houseparty.HousePartyPreferences.PREF_PASSWORD;
import static com.houseparty.houseparty.HousePartyPreferences.PREF_USERNAME;

public class PlaylistActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PlaylistAdapter adapter;
    private ActionBar actionBar;
    /* What is this variable for? */
    private static HashMap<String, String> idTable;
    private String code = "";
    private View currentView;
    private FirebaseUser currentFirebaseUser;
    public List<Playlist> playlists;

    private String title = "HouseParty";
    private static String selectedList;
    private HashMap<String, String> dataTable;
    private String playlistName = "";
    private static String passcode;
    private FirebaseDatabase pFirebaseDatabase;
    private DatabaseReference pPlaylistDatabaseReference;
    private ChildEventListener pChildEventListener;

    // Required constants for Spotify API connection.
    static final String CLIENT_ID = "4c6b32bf19e4481abdcfbe77ab6e46c0";
    static final String REDIRECT_URI = "houseparty-android://callback";

    // Used to verify if Spotify results come from correct activity.
    static final int REQUEST_CODE = 777;

    public static String selection() {
        return selectedList;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_logout:
                onClickLogout();
                return true;
            case R.id.action_settings:
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public static Map<String, String> getIdTable() {
        return idTable;
    }

    public void onClickLogout() {
        FirebaseAuth.getInstance().signOut();
        PreferenceManager.getDefaultSharedPreferences(PlaylistActivity.this)
            .edit()
            .putString(PREF_USERNAME, null)
            .putString(PREF_PASSWORD, null)
            .apply();

        finish();
        Intent intent = new Intent(PlaylistActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    public void dialogueBoxInvalidPasscode(View v) {
        Snackbar.make(findViewById(R.id.recyclerView), "Invalid passcode", Snackbar.LENGTH_SHORT).show();
    }

    public void dialogueBoxPasscode(View v, final Playlist playlist) {
        final String thePassword = playlist.getPasscode();
        Log.d("Passcode of selected: ", thePassword);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter passcode:");
        final EditText inputPasscode = new EditText(this);
        inputPasscode.setHint("XXXX");
        layout.addView(inputPasscode);
        inputPasscode.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setView(layout);
        final View thisView = v;

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String enteredCode = inputPasscode.getText().toString();
                if(!selectPlaylist(playlist, enteredCode) ) {
                if (!enteredCode.equals(thePassword)) {
                    dialogueBoxInvalidPasscode(thisView);
                    dialog.cancel();
                }
                else {
                    Intent intent = new Intent(PlaylistActivity.this, NewSongActivity.class);
                    intent.putExtra("CLIENT_ID", CLIENT_ID);
                    intent.putExtra("REDIRECT_URI", REDIRECT_URI);
                    intent.putExtra("REQUEST_CODE", REQUEST_CODE);
                    intent.putExtra("HOST", playlist.getHost());
                    startActivity(intent);
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

    public boolean selectPlaylist(Playlist playlist, String enteredCode) {
        if (!enteredCode.equals(passcode)) {
            return false;

        } else {
            return true;
        }
    }

    public void dialogueBoxPlaylist(View v) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Playlist Name:");

        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("Title");
        layout.addView(inputTitle);
        final EditText inputPasscode = new EditText(this);
        inputPasscode.setHint("Passcode (XXXX)");
        layout.addView(inputPasscode);

        inputPasscode.setInputType(InputType.TYPE_CLASS_NUMBER);

        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                playlistName = inputTitle.getText().toString();
                code = inputPasscode.getText().toString();
                Log.d("NONNUMBERCHECK", Boolean.toString(code.matches("\\d+")));
                selectedList = playlistName;
                passcode = code;
                if( !createPlaylist( selectedList, passcode )) {
                    dialog.cancel();

                }
                else {
                if (playlistName.isEmpty() ||
                    !code.matches("\\d+")
                    || code.length() != 4) {
                    dialog.cancel();
                } else {
                    selectedList = playlistName;
                    passcode = code;
                    Playlist plist = new Playlist(selectedList, passcode, currentFirebaseUser.getUid());
                    pPlaylistDatabaseReference.push().setValue(plist);
                    Intent intent = new Intent(getBaseContext(), NewSongActivity.class);
                    intent.putExtra("CLIENT_ID", CLIENT_ID);
                    intent.putExtra("REDIRECT_URI", REDIRECT_URI);
                    intent.putExtra("REQUEST_CODE", REQUEST_CODE);
                    intent.putExtra("HOST", currentFirebaseUser.getUid());
                    startActivity(intent);
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

    public boolean createPlaylist(String selectedList, String passcode) {
        if (playlistName.isEmpty() ||
                !code.matches("\\d+")
                || code.length() != 4) {
            return false;
        } else {
            Playlist plist = new Playlist(selectedList, passcode, currentFirebaseUser.getUid());
            pPlaylistDatabaseReference.push().setValue(plist);
            return true;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pFirebaseDatabase = FirebaseDatabase.getInstance();
        pPlaylistDatabaseReference = pFirebaseDatabase.getReference().child("playlists");
        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentFirebaseUser == null) {
            Log.d("PlaylistActivity", "User not authenticated");
        }

        idTable = new HashMap<>();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();

        actionBar.setTitle(title);

        recyclerView = findViewById(R.id.recyclerView);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            Drawable background = new ColorDrawable(Color.RED);
            Drawable delete = getResources().getDrawable(R.drawable.delete);

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // Don't create dialogbox if the user doesn't own the playlist
                if (!playlists.get(viewHolder.getAdapterPosition()).isHost(currentFirebaseUser.getUid())) {
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistActivity.this);
                builder.setTitle("Are you sure you want to delete this playlist?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int position = viewHolder.getAdapterPosition();
                        pPlaylistDatabaseReference.child(idTable.get(playlists.get(position).getName())).removeValue();
                        playlists.remove(position);
                        adapter.notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        adapter.notifyDataSetChanged();
                    }
                });
                builder.show();
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
                if (!playlists.get(viewHolder.getAdapterPosition()).isHost(currentFirebaseUser.getUid())) {
                    return;
                }

                // draw red background
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);

                delete.setBounds(itemView.getRight() - 140, itemView.getTop() + 25, itemView.getRight() - 20, itemView.getBottom() - 25);
                delete.draw(c);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);

        playlists = new ArrayList<>();

        adapter = new PlaylistAdapter(playlists);
        adapter.setOnItemClickListener(new PlaylistAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Playlist playlist = playlists.get(position);
                selectedList = playlists.get(position).getName();
                String id = idTable.get(selectedList);
                dialogueBoxPasscode(view, playlist);
            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getParent(), 1));

        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Add snap scrolling
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        actionBar.show();

        pChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                dataTable = (HashMap) dataSnapshot.getValue();
                idTable.put(dataTable.get("name"), dataSnapshot.getKey());

                playlists.add(new Playlist(
                    dataTable.get("name"),
                    dataTable.get("passcode"),
                    dataTable.get("host")
                ));

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.i("PlaylistActivity", "Child Changed!");

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                dataTable = (HashMap) dataSnapshot.getValue();

                playlists.remove(new Playlist(
                    dataTable.get("name"),
                    dataTable.get("passcode"),
                    dataTable.get("host")
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
        pPlaylistDatabaseReference.addChildEventListener(pChildEventListener);
    }
}
