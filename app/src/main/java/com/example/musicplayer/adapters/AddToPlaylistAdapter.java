package com.example.musicplayer.adapters;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.R;
import com.example.musicplayer.database.Playlists;
import com.example.musicplayer.database.Songs;

import java.util.ArrayList;
import java.util.List;

public class AddToPlaylistAdapter extends RecyclerView.Adapter<AddToPlaylistAdapter.ViewHolder> {


    private final List<Playlists> playlists;
    private final PlaylistItemClicked activity;
    private final Context context;
    private final Dialog dialog;
    private int index;
    private ArrayList<Songs> songs;


    public AddToPlaylistAdapter(Fragment fragment, Dialog dialog, List<Playlists> playlistsArrayList, ArrayList<Songs> songs){

        playlists = playlistsArrayList;
        this.context = fragment.getContext();
        activity = (PlaylistItemClicked) fragment;
        this.dialog = dialog;
        this.songs = songs;

    }

    public AddToPlaylistAdapter(Context context, Dialog dialog, List<Playlists> playlists, int index) {
        this.playlists = playlists;
        this.context = context;
        this.dialog = dialog;
        this.index = index;
        activity = (PlaylistItemClicked) context;
    }

    public AddToPlaylistAdapter(Context context, Dialog dialog, ArrayList<Playlists> playlistsArrayList, ArrayList<Songs> songs) {

        playlists = playlistsArrayList;
        this.context = context;
        activity = (PlaylistItemClicked) context;
        this.dialog = dialog;
        this.songs = songs;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{


        private final TextView tv_playlist_name;
        private final ImageView iv_playlist_art, iv_playlist_popup_menu;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);


            tv_playlist_name = itemView.findViewById(R.id. tv_playlist_name);
            iv_playlist_art = itemView.findViewById(R.id.iv_playlist_art);
            iv_playlist_popup_menu = itemView.findViewById(R.id.iv_playlist_popup_menu);

            itemView.setOnClickListener(this);

        }


        @Override
        public void onClick(View v) {

            if (songs == null) activity.onPlaylistItemClicked(index, playlists.get(getAdapterPosition()).getId(), dialog);
            else activity.onPlaylistItemClicked(playlists.get(getAdapterPosition()).getId(), dialog, songs);

        }
    }


    @NonNull
    @Override
    public AddToPlaylistAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_playlists,parent,false);
        return new AddToPlaylistAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final AddToPlaylistAdapter.ViewHolder holder, int position) {

        holder.iv_playlist_popup_menu.setVisibility(View.GONE);
        holder.tv_playlist_name.setText(playlists.get(position).getName());
        holder.tv_playlist_name.setSelected(true);

        Glide.with(context).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), playlists.get(position).getId()))
                .error(R.mipmap.cassette_image_foreground)
                .placeholder(R.mipmap.cassette_image_foreground)
                .centerCrop()
                .fallback(R.mipmap.cassette_image_foreground)
                .into(holder.iv_playlist_art);

    }




    @Override
    public int getItemCount() {
        return playlists.size();
    }


    @Override
    public void onViewRecycled(@NonNull AddToPlaylistAdapter.ViewHolder holder) {

        holder.iv_playlist_art.setImageDrawable(null);
        holder.tv_playlist_name.setText(null);

    }



}
