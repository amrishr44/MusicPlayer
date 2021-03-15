package com.example.musicplayer.adapters;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageUtil;
import com.example.musicplayer.database.Albums;
import com.example.musicplayer.database.Artists;
import com.example.musicplayer.database.DataLoader;
import com.example.musicplayer.database.Songs;
import com.example.musicplayer.nowplaying.NowPlaying;
import com.example.musicplayer.ui.EditTags;

import java.io.File;
import java.util.ArrayList;

public class SearchAdapter extends SelectableAdapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SONG = 1;
    private static final int TYPE_ALBUM = 2;
    private static final int TYPE_ARTIST = 3;
    private static final int TYPE_HEADER = 4;


    private final ArrayList<Songs> songs;
    private ArrayList<Songs> tempSongs;
    private final ArrayList<Albums> albums;
    private final ArrayList<Artists> artists;
    private final SearchSongClicked activitySong;
    private final SearchAlbumClicked activityAlbum;
    private final SearchArtistClicked activityArtist;
    private final StorageUtil storage;
    private final SharedPreferences sortAlbumSongs, sortArtistAlbums;
    private final Context context;
    private final String query;
    private Toast toast;
    private int songSize = 0;
    private int albumSize = 0;
    private final int totalSize;

    public SearchAdapter(Context context, ArrayList<Songs> songs, ArrayList<Albums> albums, ArrayList<Artists> artists, String query) {
        this.songs = songs;
        this.albums = albums;
        this.artists = artists;
        this.activitySong = (SearchSongClicked) context;
        this.activityAlbum = (SearchAlbumClicked) context;
        this.activityArtist = (SearchArtistClicked) context;
        this.context = context;
        this.query = query;
        if (songs.size() > 0)songSize = songs.size()+1;
        if (albums.size() > 0) albumSize = albums.size()+1;
        if (artists.size() > 0) totalSize = songSize + albumSize + artists.size()+1;
        else totalSize = songSize + albumSize;
        storage = new StorageUtil(context);
        sortAlbumSongs = context.getSharedPreferences("AlbumSongSort", Context.MODE_PRIVATE);
        sortArtistAlbums = context.getSharedPreferences("ArtistAlbumSort", Context.MODE_PRIVATE);
    }


    public class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final TextView tv_song_name, tv_artist_name, tv_song_duration;
        private final ImageView iv_album_art, iv_popup_menu;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);


            tv_song_name = itemView.findViewById(R.id.tv_song_name);
            tv_artist_name = itemView.findViewById(R.id.tv_artist_name);
            iv_album_art = itemView.findViewById(R.id.iv_album_art);
            tv_song_duration = itemView.findViewById(R.id.tv_song_duration);
            iv_popup_menu = itemView.findViewById(R.id.iv_popup_menu);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

        }


        @Override
        public void onClick(View v) {

            activitySong.onSearchSongClicked(getAdapterPosition(), songs.get(getAdapterPosition()-1));
        }

        @Override
        public boolean onLongClick(View v) {
            activitySong.onSearchSongLongClicked(getAdapterPosition());
            notifyDataSetChanged();
            return true;
        }
    }


    public class AlbumViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{

        private final TextView albums_album_title;
        private final ImageView albums_album_art, albums_popup_menu;

        AlbumViewHolder(@NonNull View itemView) {
            super(itemView);

            albums_album_art = itemView.findViewById(R.id.albums_album_art);
            albums_album_title = itemView.findViewById(R.id.albums_album_title);
            albums_popup_menu = itemView.findViewById(R.id.albums_popup_menu);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

        }

        @Override
        public void onClick(View v) {

            activityAlbum.onSearchAlbumClicked(getAdapterPosition(), albums.get(getAdapterPosition()-songSize-1));
        }

        @Override
        public boolean onLongClick(View v) {
            activityAlbum.onSearchAlbumLongClicked(getAdapterPosition());
            notifyDataSetChanged();
            return true;
        }
    }

    public class ArtistViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final TextView artists_artist_name;
        private final ImageView artists_popup_menu;

        ArtistViewHolder(@NonNull View itemView) {
            super(itemView);

            artists_artist_name = itemView.findViewById(R.id.artists_artist_name);
            artists_popup_menu = itemView.findViewById(R.id.artists_popup_menu);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

        }

        @Override
        public void onClick(View v) {

            activityArtist.onSearchArtistClicked(getAdapterPosition(), artists.get(getAdapterPosition()-songSize-albumSize-1));
        }

        @Override
        public boolean onLongClick(View v) {
            activityArtist.onSearchArtistLongClicked(getAdapterPosition());
            notifyDataSetChanged();
            return true;
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder{

        private final TextView headerText;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            headerText = itemView.findViewById(R.id.search_header);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        switch (viewType){

            case TYPE_SONG:
                View root0 = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_songs, parent, false);
                return new SearchAdapter.SongViewHolder(root0);

            case TYPE_ALBUM:
                View root1 = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_albums, parent, false);
                return new SearchAdapter.AlbumViewHolder(root1);

            case TYPE_ARTIST:
                View root2 = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_artists, parent, false);
                return new SearchAdapter.ArtistViewHolder(root2);

            case TYPE_HEADER:
                View root3 = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_header_text, parent, false);
                return new HeaderViewHolder(root3);

        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {


        switch (holder.getItemViewType()){

            case TYPE_SONG:

                SongViewHolder holder0 = (SongViewHolder) holder;

                int x = songs.get(position-1).getTitle().toLowerCase().indexOf(query);

                if (x!=-1) {

                    Spannable text = new SpannableString(songs.get(position-1).getTitle());
                    text.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.programmer_green)), x, x + query.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    holder0.tv_song_name.setText(text);
                }
                else holder0.tv_song_name.setText(songs.get(position-1).getTitle());
                holder0.tv_artist_name.setText(songs.get(position-1).getArtist());
                holder0.tv_song_duration.setText(NowPlaying.getTimeInMins(songs.get(position-1).getDuration()/1000));
                holder0.tv_song_name.setSelected(true);

                Glide.with(holder0.iv_album_art).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), songs.get(position-1).getAlbumid()))
                        .error(R.mipmap.cassette_image_foreground)
                        .placeholder(R.mipmap.cassette_image_foreground)
                        .centerCrop()
                        .fallback(R.mipmap.cassette_image_foreground)
                        .into(holder0.iv_album_art);


                if (isSelected(position)){
                    holder0.itemView.setBackground(context.getResources().getDrawable(R.drawable.selected));
                    holder0.iv_popup_menu.setClickable(false);
                }
                else {

                    holder0.itemView.setBackground(context.getResources().getDrawable(R.drawable.not_selected));
                    holder0.iv_popup_menu.setClickable(true);

                    holder0.iv_popup_menu.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            tempSongs = new ArrayList<>();
                            tempSongs.add(songs.get(position));

                            PopupMenu menu = new PopupMenu(context, v);

                            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {

                                    switch (item.getTitle().toString()) {

                                        case "Play":
                                            DataLoader.playAudio(0, tempSongs, storage, context);
                                            break;

                                        case "Enqueue":
                                            MediaPlayerService.audioList.add(songs.get(position));
                                            storage.storeAudio(MediaPlayerService.audioList);
                                            break;

                                        case "Play next":
                                            MediaPlayerService.audioList.add(MediaPlayerService.audioIndex + 1, songs.get(position));
                                            storage.storeAudio(MediaPlayerService.audioList);
                                            break;

                                        case "Shuffle":
                                            DataLoader.playAudio(position, tempSongs, storage, context);
                                            MediaControllerCompat.getMediaController((AppCompatActivity) context).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                                            break;

                                        case "Add to playlist":
                                            DataLoader.addToPlaylist(tempSongs, context, null);
                                            break;

                                        case "Lyrics":
                                            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                                            intent.putExtra("query", songs.get(position).getTitle() + " " + songs.get(position).getArtist() + " lyrics");
                                            context.startActivity(intent);
                                            break;

                                        case "Edit tags":
                                            EditTags.song = songs.get(position);
                                            context.startActivity(new Intent(context, EditTags.class));
                                            break;

                                        case "Use as ringtone":

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                if (!Settings.System.canWrite(context)) {

                                                    Intent ringtoneIntent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                                    ((AppCompatActivity)context).startActivityForResult(ringtoneIntent, 69);

                                                } else {
                                                    RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songs.get(position).getId()));
                                                    if (toast != null) toast = null;
                                                    toast = Toast.makeText(context, songs.get(position).getTitle() + " set as Ringtone!", Toast.LENGTH_LONG);
                                                    toast.show();
                                                }
                                            } else {
                                                RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songs.get(position).getId()));
                                                if (toast != null) toast = null;
                                                toast = Toast.makeText(context, songs.get(position).getTitle() + " set as Ringtone!", Toast.LENGTH_LONG);
                                                toast.show();
                                            }
                                            break;

                                        case "Delete":
                                            deleteSongs();
                                            break;

                                    }

                                    return true;
                                }
                            });

                            menu.inflate(R.menu.song_popup_menu);
                            menu.show();

                        }
                    });

                }

                break;


            case TYPE_ALBUM:

                AlbumViewHolder holder1 = (AlbumViewHolder) holder;

                int y = albums.get(position-songSize-1).getAlbum().toLowerCase().indexOf(query);
                if (y!=-1) {

                    Spannable text = new SpannableString(albums.get(position-songSize-1).getAlbum());
                    text.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.programmer_green)), y, y + query.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    holder1.albums_album_title.setText(text);
                }
                else holder1.albums_album_title.setText(albums.get(position-songSize-1).getAlbum());

                Glide.with(holder1.albums_album_art).load(albums.get(position-songSize-1).getAlbumArt())
                        .centerCrop().error(R.drawable.cassettes).into(holder1.albums_album_art);

                if (isSelected(position)) {
                    holder1.itemView.setBackgroundColor(context.getResources().getColor(R.color.programmer_green));
                    holder1.albums_popup_menu.setClickable(false);
                }
                else {
                    holder1.itemView.setBackgroundColor(context.getResources().getColor(R.color.colorDark));
                    holder1.albums_popup_menu.setClickable(true);

                    holder1.albums_popup_menu.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(context, v);

                            menu.setOnMenuItemClickListener(new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {

                                    tempSongs = DataLoader.loadAudio(albums.get(position).getId(), 1, context, sortAlbumSongs);

                                    switch (item.getTitle().toString()) {

                                        case "Play":
                                            DataLoader.playAudio(0, tempSongs, storage, context);
                                            break;

                                        case "Enqueue":
                                            MediaPlayerService.audioList.addAll(tempSongs);
                                            storage.storeAudio(MediaPlayerService.audioList);
                                            break;

                                        case "Play next":
                                            MediaPlayerService.audioList.addAll(MediaPlayerService.audioIndex + 1, tempSongs);
                                            storage.storeAudio(MediaPlayerService.audioList);
                                            break;

                                        case "Shuffle":
                                            DataLoader.playAudio(position, tempSongs, storage, context);
                                            MediaControllerCompat.getMediaController((AppCompatActivity) context).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                                            break;

                                        case "Add to playlist":
                                            DataLoader.addToPlaylist(tempSongs, context, null);
                                            break;

                                        case "Delete":
                                            deleteSongs();
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

                break;

            case TYPE_ARTIST:

                ArtistViewHolder holder2 = (ArtistViewHolder) holder;

                int z = artists.get(position-songSize-albumSize-1).getArtist().toLowerCase().indexOf(query);
                if (z!=-1) {

                    Spannable text = new SpannableString(artists.get(position-songSize-albumSize-1).getArtist());
                    text.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.programmer_green)), z, z + query.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    holder2.artists_artist_name.setText(text);
                }
                else holder2.artists_artist_name.setText(artists.get(position-songSize-albumSize-1).getArtist());

                if (isSelected(position)) {
                    holder2.itemView.setBackgroundColor(context.getResources().getColor(R.color.programmer_green));
                    holder2.artists_popup_menu.setClickable(false);
                }
                else {
                    holder2.itemView.setBackgroundColor(context.getResources().getColor(R.color.colorDark));
                    holder2.artists_popup_menu.setClickable(true);

                    holder2.artists_popup_menu.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(context, v);

                            menu.setOnMenuItemClickListener(new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {

                                    tempSongs = loadArtistAudio(artists.get(position).getId());

                                    switch (item.getTitle().toString()) {

                                        case "Play":
                                            DataLoader.playAudio(0, tempSongs, storage, context);
                                            break;

                                        case "Enqueue":
                                            MediaPlayerService.audioList.addAll(tempSongs);
                                            storage.storeAudio(MediaPlayerService.audioList);
                                            break;

                                        case "Play next":
                                            MediaPlayerService.audioList.addAll(MediaPlayerService.audioIndex + 1, tempSongs);
                                            storage.storeAudio(MediaPlayerService.audioList);
                                            break;

                                        case "Shuffle":
                                            DataLoader.playAudio(position, tempSongs, storage, context);
                                            MediaControllerCompat.getMediaController((AppCompatActivity) context).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                                            break;

                                        case "Add to playlist":
                                            DataLoader.addToPlaylist(tempSongs, context, null);
                                            break;

                                        case "Delete":
                                            deleteSongs();
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

                break;

            case TYPE_HEADER:
                HeaderViewHolder holder3 = (HeaderViewHolder) holder;
                if (position==0 && songSize != 0){
                    holder3.headerText.setText("Songs");
                }
                else if (position==songSize+albumSize){
                    holder3.headerText.setText("Artists");
                }
                else {
                    holder3.headerText.setText("Albums");
                }
                break;

        }


    }

    @Override
    public int getItemCount() {
        return totalSize;
    }

    @Override
    public int getItemViewType(int position) {

        if (position==0 || position==songSize || position == songSize+albumSize) {
            return TYPE_HEADER;
        }
        else if (position<songSize){
            return TYPE_SONG;
        }
        else if (position<songSize+albumSize){
            return TYPE_ALBUM;
        }
        else return TYPE_ARTIST;

    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {

        if (holder.getItemViewType() == TYPE_SONG){

            SongViewHolder holder1 = (SongViewHolder) holder;
            Glide.with(holder1.iv_album_art).clear(holder1.iv_album_art);
            holder1.iv_album_art.setImageDrawable(null);
            holder1.iv_popup_menu.setOnClickListener(null);
        }
        else if (holder.getItemViewType() == TYPE_ALBUM){

            AlbumViewHolder holder1 = (AlbumViewHolder) holder;
            Glide.with(holder1.albums_album_art).clear(holder1.albums_album_art);
            holder1.albums_album_art.setImageDrawable(null);
            holder1.albums_popup_menu.setOnClickListener(null);
        }
        else if (holder.getItemViewType() == TYPE_ARTIST){

            ArtistViewHolder holder1 = (ArtistViewHolder) holder;
            holder1.artists_popup_menu.setOnClickListener(null);
        }

        super.onViewRecycled(holder);
    }

    public ArrayList<Songs> loadArtistAudio(long artist_id)
    {
        ArrayList<Songs> arrayList = new ArrayList<>();

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = android.provider.MediaStore.Audio.Media.IS_MUSIC + "!= 0 AND " + MediaStore.Audio.Media.ARTIST_ID + "= " + artist_id;
        String sortOrder;
        if (sortArtistAlbums != null) {
            if (sortArtistAlbums.getBoolean("reverse", false)) sortOrder = sortArtistAlbums.getString("sortOrder", MediaStore.Audio.Media.DEFAULT_SORT_ORDER) + " DESC";
            else sortOrder = sortArtistAlbums.getString("sortOrder", MediaStore.Audio.Media.TRACK);
        }
        else sortOrder = MediaStore.Audio.Media.TRACK;
        Cursor cursor = contentResolver.query(uri, new String[]{"_id","_data", "title", "artist", "album", "duration", "track", "artist_id", "album_id", "year"}, selection, null, sortOrder);


        if (cursor != null && cursor.getCount()>0) {

            cursor.moveToFirst();

            do {
                long id = cursor.getLong(0);
                String data = cursor.getString(1).trim();
                String title = cursor.getString(2).trim();
                String artist = cursor.getString(3).trim();
                String album = cursor.getString(4).trim();
                int duration = cursor.getInt(5);
                int trackNumber = cursor.getInt(6);
                long artistId = cursor.getInt(7);
                long albumId = cursor.getLong(8);
                long year = cursor.getLong(9);

                arrayList.add(new Songs(id, data, title, artist, album, artistId, albumId, trackNumber, duration, year));
            }
            while (cursor.moveToNext());

        }
        if (cursor != null) {

            cursor.close();
        }

        return arrayList;
    }



    private void deleteSongs(){

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

                        for (int i = 0; i < tempSongs.size(); i++) {

                            dialog.dismiss();
                            context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "_id =? ", new String[]{tempSongs.get(i).getId() + ""});
                            new File(tempSongs.get(i).getData()).delete();
                            tempSongs.remove(i);
                        }
                        notifyDataSetChanged();
                    }
                }.run();


            }
        });



    }


}

