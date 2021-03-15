package com.example.musicplayer.database;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageUtil;
import com.example.musicplayer.adapters.AddToPlaylistAdapter;
import com.example.musicplayer.nowplaying.NowPlaying;
import com.example.musicplayer.ui.playlists.PlaylistsViewModel;
import com.example.musicplayer.ui.songs.SongsFragment;

import java.util.ArrayList;

public class DataLoader {

//    Common functions such as loading audio, playing audio etc which are used multiple times are placed in this class.

    /**
     * Used to play a specific audio file from a list of songs.
     *
     * @param audioIndex - the index of the song in the list
     * @param songs - the list of songs
     * @param storage - the storageutil file to store the audio index
     * @param context - the context of the activity/fragment where it is called
     * **/
    public static void playAudio(int audioIndex, ArrayList<Songs> songs, StorageUtil storage, final Context context) {
        //Check is service is active

        if (!MainActivity.serviceBound) {
            //Store Serializable audioList to SharedPreferences
            storage.storeAudio(songs);
            storage.storeAudioIndexAndPostion(audioIndex, 0);


            context.startService(new Intent(context.getApplicationContext(), MediaPlayerService.class));
            MainActivity.serviceBound = true;

        }


        else {
            //Store the new audioIndex to SharedPreferences
            storage.storeAudio(songs);
            storage.storeAudioIndexAndPostion(audioIndex, 0);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(SongsFragment.Broadcast_PLAY_NEW_AUDIO);
            context.sendBroadcast(broadcastIntent);
        }

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {

                context.startActivity(new Intent(context, NowPlaying.class));
//            }
//        }, 1000);

    }

    /**
     *Used to load audio files into a list of songs from an album/artist/playlist
     *
     * @param id_ - The id of the album/artist/playlist to be loaded
     * @param type - 1 for album, 2 for artist, 3 for playlist
     * @param context - the context of the activity/fragment where it is called
     * @param sort  - the sharedpreferences file containing the sort order
     * @return - an Arraylist of songs from the album/artist/playlist
     * */

    public static ArrayList<Songs> loadAudio(long id_, int type, Context context, SharedPreferences sort)
    {
        ArrayList<Songs> arrayList = new ArrayList<>();

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String sortOrder;
        String selection;

        if (type == 1) {
            selection = android.provider.MediaStore.Audio.Media.IS_MUSIC + "!= 0 AND " + MediaStore.Audio.Media.ALBUM_ID + "= " + id_;
        }
        else if (type == 2){
            selection = android.provider.MediaStore.Audio.Media.IS_MUSIC + "!= 0 AND " + MediaStore.Audio.Media.ARTIST_ID + "= " + id_;
        }
        else{

           uri = MediaStore.Audio.Playlists.Members.getContentUri("external", id_);
           selection = android.provider.MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        }

        if (type != 3) {
            if (sort != null) {
                if (sort.getBoolean("AlbumSongReverse", false))
                    sortOrder = sort.getString("AlbumSongSortOrder", MediaStore.Audio.Media.DEFAULT_SORT_ORDER) + " DESC";
                else sortOrder = sort.getString("AlbumSongSortOrder", MediaStore.Audio.Media.TRACK);
            } else sortOrder = MediaStore.Audio.Media.TRACK;
        }
        else sortOrder = "play_order";

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


    /**
     *
     * Used to add a list of songs to a playlist
     *
     * @param songs - the list of songs to be added to the playlist
     * @param context - the context of the activity/fragment where it is called
     * @param fragment - the fragment where it is called
     * */

    public static void addToPlaylist(final ArrayList<Songs> songs, final Context context, final Fragment fragment){

        final Dialog dialog  = new Dialog(context);

        dialog.setContentView(R.layout.add_to_playlist); //Dialog Box for choosing the playlist

        dialog.findViewById(R.id.create_new_playlist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();

                final Dialog newPlaylist = new Dialog(context); //Dialog Box for creating a new playlist
                newPlaylist.setContentView(R.layout.new_playlist);
                final EditText name = newPlaylist.findViewById(R.id.new_playlist_name);


                Button cancel = newPlaylist.findViewById(R.id.bt_cancel);
                final Button create = newPlaylist.findViewById(R.id.bt_create);

                final ContentResolver contentResolver = context.getContentResolver();
                final Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
                final ArrayList<String> playlistNames = new ArrayList<>();

                final Handler handler = new Handler();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                        String x = name.getText().toString().trim();

                        if (playlistNames.contains(x)) create.setText("Overwrite");
                        else create.setText("Create");

                        handler.postDelayed(this, 500);

                    }
                };

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        newPlaylist.dismiss();
                        handler.removeCallbacks(runnable);

                    }
                });

                new Runnable() {
                    @Override
                    public void run() {

                        Cursor cursor = contentResolver.query(uri, new String[]{"name"}, null, null, null);

                        if (cursor!=null && cursor.getCount()>0){

                            cursor.moveToFirst();

                            do {
                                playlistNames.add(cursor.getString(0));
                            }while(cursor.moveToNext());

                            cursor.close();

                        }

                    }
                }.run();



                handler.postDelayed(runnable, 500);

                create.setOnClickListener(new View.OnClickListener() {

                    // Creates a new playlist and then adds the songs to it

                    @Override
                    public void onClick(View v) {

                        ContentValues contentValue = new ContentValues();
                        contentValue.put("name", name.getText().toString().trim());
                        contentValue.put("date_modified", System.currentTimeMillis());

                        if (!create.getText().toString().equals("Overwrite")) {
                            contentResolver.insert(uri, contentValue);
                        }
                        else{
                            contentResolver.update(uri, contentValue, "name =? ", new String[]{name.getText().toString().trim()});
                        }

                        long playlist_id = 0;

                        Cursor cursor = contentResolver.query(uri, new String[]{"_id"}, "name =? ", new String[]{name.getText().toString().trim()}, null);

                        if (cursor!=null && cursor.getCount()>0){

                            cursor.moveToFirst();

                            do {
                                playlist_id = cursor.getLong(0);
                            }while(cursor.moveToNext());

                            cursor.close();

                        }



                        Uri songUri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist_id);

                        int playOrder = 1;

                        cursor = contentResolver.query(songUri, new String[]{"max(play_order)"}, null, null,null);

                        if (cursor != null && cursor.getCount()>0){

                            cursor.moveToFirst();

                            do {
                                playOrder = cursor.getInt(0) + 1;
                            }while (cursor.moveToNext());

                            cursor.close();
                        }


                        for (int i = 0; i<songs.size(); i++) {

                            contentValue.clear();
                            contentValue.put("audio_id", songs.get(i).getId());
                            contentValue.put("play_order", playOrder);

                            contentResolver.insert(songUri, contentValue);
                            playOrder++;
                        }
                        handler.removeCallbacks(runnable);
                        newPlaylist.dismiss();

                    }
                });

                newPlaylist.show();
            }
        });


        final RecyclerView recyclerView = dialog.findViewById(R.id.add_to_playlist_recycler_view);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        new Runnable() {
            @Override
            public void run() {

                if (fragment != null) recyclerView.setAdapter(new AddToPlaylistAdapter(fragment, dialog, PlaylistsViewModel.loadPlaylists(context),songs));
                else recyclerView.setAdapter(new AddToPlaylistAdapter(context, dialog, PlaylistsViewModel.loadPlaylists(context),songs));

            }
        }.run();

        dialog.show();

    }



}
