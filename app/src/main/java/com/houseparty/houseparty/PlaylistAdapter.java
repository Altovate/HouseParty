package com.houseparty.houseparty;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * @author Eitan created on 5/17/2018.
 */
public class PlaylistAdapter extends
    RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    private List<Playlist> mPlaylists;

    private FirebaseUser currentUser;

    // Define listener member variable
    private OnItemClickListener listener;

    public PlaylistAdapter(List<Playlist> playlists) {
        mPlaylists = playlists;
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    // Define the method that allows the parent activity or fragment to define the listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View playlistView = inflater.inflate(R.layout.playlist_item, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(playlistView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // This could be unneccesary and not actually fixing our bug
        holder.setIsRecyclable(false);

        // Get the data model based on position
        Playlist playlist = mPlaylists.get(position);

        // Set item views based on your views and data model
        TextView textView = holder.playlistName;
        textView.setText(playlist.getName());

        TextView hostTextView = holder.hostNotifier;

        if (playlist.isHost(currentUser.getUid())) {
            hostTextView.setVisibility(View.VISIBLE);
        }

        holder.setIsRecyclable(true);
    }

    @Override
    public int getItemCount() {
        return mPlaylists.size();
    }

    // Define the listener interface
    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView playlistName;
        public TextView hostNotifier;

        public ViewHolder(final View itemView) {
            super(itemView);

            playlistName = itemView.findViewById(R.id.playlist_name);
            hostNotifier = itemView.findViewById(R.id.host_notifier);

            // Setup the click listener
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Triggers click upwards to the adapter on click
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(itemView, position);
                        }
                    }
                }
            });
        }
    }
}
