package com.example.musicplayer.adapters;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageUtil;
import com.example.musicplayer.database.Artists;
import com.example.musicplayer.database.DataLoader;
import com.example.musicplayer.database.Songs;
import com.example.musicplayer.nowplaying.NowPlaying;
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

public class ArtistsAdapter extends SelectableAdapter<ArtistsAdapter.ViewHolder> implements RecyclerViewFastScroller.OnPopupTextUpdate {

    private final ArrayList<Artists> artists;
    private final ItemClicked activity;
    private final Context context;
    private final StorageUtil storage;
    private ArrayList<Songs> songs;
    private final Fragment fragment;
    private final SharedPreferences sort;

    public ArtistsAdapter(ArrayList<Artists> artists, Fragment fragment, Context context) {
        this.artists = artists;
        this.activity = (ItemClicked) fragment;
        this.context = context;
        storage = new StorageUtil(context);
        this.fragment = fragment;
        sort = context.getSharedPreferences("Sort", Context.MODE_PRIVATE);
    }

    @NotNull
    @Override
    public CharSequence onChange(int i) {


        if (sort.getString("ArtistSortOrder", MediaStore.Audio.Artists.DEFAULT_SORT_ORDER).equals(MediaStore.Audio.Artists.DEFAULT_SORT_ORDER)) {
            String s;
            int x = (int) artists.get(i).getArtist().toUpperCase().charAt(0);

            if (artists.get(i).getArtist().toUpperCase().indexOf("THE ") == 0) {
                s = artists.get(i).getArtist().substring(4, 5).toUpperCase();
            } else if (artists.get(i).getArtist().toUpperCase().indexOf("A ") == 0) {
                s = artists.get(i).getArtist().substring(2, 3).toUpperCase();
            } else if (x >= 65 && x <= 90) s = artists.get(i).getArtist().substring(0, 1).toUpperCase();
            else {
                s = "#";
            }
            return s;
        }
        return "";
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{

        private final ImageView artists_popup_menu;
        private final TextView artists_artist_name;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            artists_artist_name = itemView.findViewById(R.id.artists_artist_name);
            artists_popup_menu = itemView.findViewById(R.id.artists_popup_menu);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {

            activity.onItemClicked(getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {

            activity.onItemLongClicked(getAdapterPosition());
            notifyDataSetChanged();
            return true;
        }
    }

    @NonNull
    @Override
    public ArtistsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View root = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_artists, parent, false);

        return new ArtistsAdapter.ViewHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistsAdapter.ViewHolder holder, final int position) {

        holder.artists_artist_name.setText(artists.get(position).getArtist());

        if (isSelected(position)) {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.programmer_green));
            holder.artists_popup_menu.setClickable(false);
        }
        else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.colorDark));
            holder.artists_popup_menu.setClickable(true);

            holder.artists_popup_menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    PopupMenu menu = new PopupMenu(context, v);

                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            songs = DataLoader.loadAudio(artists.get(position).getId(), 2, context, sort);

                            switch (item.getTitle().toString()) {

                                case "Play":
                                    DataLoader.playAudio(0, songs, storage, context);
                                    break;

                                case "Enqueue":
                                    MediaPlayerService.audioList.addAll(songs);
                                    storage.storeAudio(MediaPlayerService.audioList);
                                    if (songs.size()>1) Toast.makeText(context, songs.size() + " songs have been added to the queue!", Toast.LENGTH_SHORT).show();
                                    else Toast.makeText(context, "1 song has been added to the queue!", Toast.LENGTH_SHORT).show();
                                    break;

                                case "Play next":
                                    MediaPlayerService.audioList.addAll(MediaPlayerService.audioIndex + 1, songs);
                                    storage.storeAudio(MediaPlayerService.audioList);
                                    if (songs.size()>1) Toast.makeText(context, songs.size() + " songs have been added to the queue!", Toast.LENGTH_SHORT).show();
                                    else Toast.makeText(context, "1 song has been added to the queue!", Toast.LENGTH_SHORT).show();
                                    break;

                                case "Shuffle":
                                    DataLoader.playAudio(position, songs, storage, context);
                                    NowPlaying.shuffle = true;
                                    break;

                                case "Add to playlist":
                                    DataLoader.addToPlaylist(songs, context, fragment);
                                    break;

                                case "Delete":
                                    deleteSongs(position);
                                    break;

                            }

                            return true;
                        }
                    });

                    menu.inflate(R.menu.album_popup_menu);
                    menu.show();


                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return artists.size();
    }


    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.artists_popup_menu.setOnClickListener(null);
        holder.artists_artist_name.setText(null);

        super.onViewRecycled(holder);
    }

    private void deleteSongs(final int position){

        final Dialog dialog = new Dialog(context);

        dialog.setContentView(R.layout.delete_song_layout);

        TextView delete = dialog.findViewById(R.id.tv_delete);

        delete.setText("Are you sure you want to delete multiple songs?");

        dialog.show();

        dialog.findViewById(R.id.delete_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();

            }
        });

        dialog.findViewById(R.id.delete_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Runnable() {
                    @Override
                    public void run() {

                        for (int i = 0; i < songs.size(); i++) {

                            dialog.dismiss();
                            context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "_id =? ", new String[]{songs.get(i).getId() + ""});
                            new File(songs.get(i).getData()).delete();
                            songs.remove(i);
                        }
                        artists.remove(position);
                        notifyDataSetChanged();
                    }
                }.run();

            }
        });



    }


}
