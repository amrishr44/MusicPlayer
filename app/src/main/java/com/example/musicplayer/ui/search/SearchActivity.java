package com.example.musicplayer.ui.search;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageUtil;
import com.example.musicplayer.adapters.PlaylistItemClicked;
import com.example.musicplayer.adapters.SearchAdapter;
import com.example.musicplayer.adapters.SearchAlbumClicked;
import com.example.musicplayer.adapters.SearchArtistClicked;
import com.example.musicplayer.adapters.SearchSongClicked;
import com.example.musicplayer.database.Albums;
import com.example.musicplayer.database.Artists;
import com.example.musicplayer.database.DataLoader;
import com.example.musicplayer.database.Songs;
import com.example.musicplayer.nowplaying.NowPlaying;
import com.example.musicplayer.ui.albums.AlbumsFragment;
import com.example.musicplayer.ui.artists.ArtistsFragment;
import com.example.musicplayer.ui.songs.SongsFragment;

import java.util.ArrayList;

import static com.example.musicplayer.MainActivity.UNIQUE_REQUEST_CODE;

public class SearchActivity extends AppCompatActivity implements SearchSongClicked, SearchAlbumClicked, SearchArtistClicked, PlaylistItemClicked {

    private RecyclerView recyclerView;
    private SearchAdapter myAdapter;
    private SearchView searchView;
    private GridLayoutManager layoutManager;
    private StorageUtil storage;

    private ArrayList<Songs> songs;
    private ArrayList<Albums> albums;
    private ArrayList<Artists> artists;

    private ActionMode actionMode;
    private ActionModeCallback actionModeCallback = new ActionModeCallback();

    public static boolean openAlbumSongs = false;
    public static boolean openArtistAlbums = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = findViewById(R.id.search_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.back_green);
        getSupportActionBar().setTitle("");

        storage = new StorageUtil(this);

        searchView =findViewById(R.id.search_view);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, UNIQUE_REQUEST_CODE);
        }
        else{

            recyclerView = findViewById(R.id.search_recycler_view);
            recyclerView.setHasFixedSize(true);
            layoutManager = new GridLayoutManager(this, 2);
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {

                    if (position <= songs.size() + 1 || position == songs.size() + albums.size() + 2) {
                        return 2;
                    } else return 1;
                }
            });
            recyclerView.setLayoutManager(layoutManager);


            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(final String query) {

                    if (!query.isEmpty() && !query.trim().equals("")) {


                        new Runnable() {
                            @Override
                            public void run() {

                                new Runnable() {
                                    @Override
                                    public void run() {
                                        songs = loadAudio(query.trim());
                                    }
                                }.run();

                                new Runnable() {
                                    @Override
                                    public void run() {
                                        albums = loadAlbums(query.trim());
                                    }
                                }.run();

                                new Runnable() {
                                    @Override
                                    public void run() {
                                        artists = loadArtists(query.trim());
                                    }
                                }.run();

                                myAdapter = new SearchAdapter(SearchActivity.this, songs, albums, artists, query.trim().toLowerCase());
                                recyclerView.setAdapter(myAdapter);

                            }
                        }.run();

                    } else {

                        myAdapter = null;
                        recyclerView.setAdapter(null);

                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(final String newText) {

                    if (!newText.isEmpty() && !newText.trim().equals("")) {


                        new Runnable() {
                            @Override
                            public void run() {

                                new Runnable() {
                                    @Override
                                    public void run() {
                                        songs = loadAudio(newText.trim());
                                    }
                                }.run();

                                new Runnable() {
                                    @Override
                                    public void run() {
                                        albums = loadAlbums(newText.trim());
                                    }
                                }.run();

                                new Runnable() {
                                    @Override
                                    public void run() {
                                        artists = loadArtists(newText.trim());
                                    }
                                }.run();

                                myAdapter = new SearchAdapter(SearchActivity.this, songs, albums, artists, newText.trim().toLowerCase());
                                recyclerView.setAdapter(myAdapter);

                            }
                        }.run();
                    } else {
                        myAdapter = null;
                        recyclerView.setAdapter(null);
                    }

                    return true;
                }
            });

        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == UNIQUE_REQUEST_CODE){

            try {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    recyclerView = findViewById(R.id.search_recycler_view);
                    recyclerView.setHasFixedSize(true);
                    layoutManager = new GridLayoutManager(this, 2);
                    layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                        @Override
                        public int getSpanSize(int position) {

                            if (position <= songs.size() + 1 || position == songs.size() + albums.size() + 2) {
                                return 2;
                            } else return 1;
                        }
                    });
                    recyclerView.setLayoutManager(layoutManager);


                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(final String query) {

                            if (!query.isEmpty() && !query.trim().equals("")) {


                                new Runnable() {
                                    @Override
                                    public void run() {

                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                songs = loadAudio(query.trim());
                                            }
                                        }.run();

                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                albums = loadAlbums(query.trim());
                                            }
                                        }.run();

                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                artists = loadArtists(query.trim());
                                            }
                                        }.run();

                                        myAdapter = new SearchAdapter(SearchActivity.this, songs, albums, artists, query.trim().toLowerCase());
                                        recyclerView.setAdapter(myAdapter);

                                    }
                                }.run();

                            } else {

                                myAdapter = null;
                                recyclerView.setAdapter(null);

                            }
                            return true;
                        }

                        @Override
                        public boolean onQueryTextChange(final String newText) {

                            if (!newText.isEmpty() && !newText.trim().equals("")) {


                                new Runnable() {
                                    @Override
                                    public void run() {

                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                songs = loadAudio(newText.trim());
                                            }
                                        }.run();

                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                albums = loadAlbums(newText.trim());
                                            }
                                        }.run();

                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                artists = loadArtists(newText.trim());
                                            }
                                        }.run();

                                        myAdapter = new SearchAdapter(SearchActivity.this, songs, albums, artists, newText.trim().toLowerCase());
                                        recyclerView.setAdapter(myAdapter);

                                    }
                                }.run();
                            } else {
                                myAdapter = null;
                                recyclerView.setAdapter(null);
                            }

                            return true;
                        }
                    });

                }
                else{

                    Toast.makeText(this, "What the hell bro!", Toast.LENGTH_SHORT).show();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }



    private ArrayList<Songs> loadAudio(String search)
    {
        ArrayList<Songs> arrayList = new ArrayList<>();

        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = android.provider.MediaStore.Audio.Media.IS_MUSIC + "!= 0 AND " + MediaStore.Audio.Media.TITLE + " Like ? " ;
        String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
        Cursor cursor = contentResolver.query(uri, new String[]{"_id","_data", "title", "artist", "album", "duration", "track", "artist_id", "album_id", "year"}, selection, new String[]{"%" + search + "%"}, sortOrder);


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
                long year= cursor.getLong(9);

                arrayList.add(new Songs(id, data, title, artist, album, artistId, albumId, trackNumber, duration, year));
            }
            while (cursor.moveToNext());

        }
        if (cursor != null) {

            cursor.close();
        }

        return arrayList;
    }

    private ArrayList<Albums> loadAlbums(String query){

        ArrayList<Albums> arrayList = new ArrayList<>();

        ContentResolver contentResolver = getApplication().getContentResolver();
        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER;
        String selection = MediaStore.Audio.Albums.ALBUM + " LIKE ? ";


        Cursor cursor = contentResolver.query(uri, new String[]{"_id","album","album_art", "numsongs", "artist"}, selection, new String[]{"%"+query+"%"}, sortOrder);

        if (cursor != null && cursor.getCount()>0) {

            cursor.moveToFirst();

            do {

                long id = cursor.getLong(0);
                String album = cursor.getString(1).trim();
                String albumArt = cursor.getString(2);
                int numSongs = cursor.getInt(3);
                String artist = cursor.getString(4).trim();


                arrayList.add(new Albums(id, album, albumArt, numSongs, artist));
            }
            while (cursor.moveToNext());

        }
        if (cursor != null) {

            cursor.close();
        }


        return arrayList;

    }

    private ArrayList<Artists> loadArtists(String query){

        ArrayList<Artists> arrayList = new ArrayList<>();

        ContentResolver contentResolver = getApplication().getContentResolver();
        Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER;
        String selection = MediaStore.Audio.Artists.ARTIST + " LIKE ? ";

        Cursor cursor = contentResolver.query(uri, new String[]{"_id", "artist"}, selection, new String[]{"%"+query+"%"}, sortOrder);

        if (cursor != null && cursor.getCount()>0) {

            cursor.moveToFirst();

            do {

                long id = cursor.getLong(0);
                String artist = cursor.getString(1).trim();

                arrayList.add(new Artists(id, artist));
            }
            while (cursor.moveToNext());

        }
        if (cursor != null) {

            cursor.close();
        }


        return arrayList;

    }

    @Override
    public void onSearchSongClicked(int index, Songs song) {

        if (actionMode != null) {
            toggleSelection(index);
        }
        else{
            playAudio(song);
            myAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onSearchSongLongClicked(int index) {
        if (actionMode == null) actionMode = startSupportActionMode(actionModeCallback);
        toggleSelection(index);
    }


    @Override
    public void onSearchAlbumClicked(int index, Albums album) {

        if (actionMode != null) {
            toggleSelection(index);
        }
        else {
            myAdapter.notifyDataSetChanged();
            AlbumsFragment.album = album;
            openAlbumSongs = true;
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    @Override
    public void onSearchAlbumLongClicked(int index) {
        if (actionMode == null) actionMode = startSupportActionMode(actionModeCallback);
        toggleSelection(index);
    }

    @Override
    public void onSearchArtistClicked(int index, Artists artist) {
        if (actionMode != null) {
            toggleSelection(index);
        }
        else {
            myAdapter.notifyDataSetChanged();
            ArtistsFragment.artist = artist;
            openArtistAlbums = true;
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    @Override
    public void onSearchArtistLongClicked(int index) {
        if (actionMode == null) actionMode = startSupportActionMode(actionModeCallback);
        toggleSelection(index);
    }

    private void playAudio(Songs song) {
        //Check is service is active
        if (!MainActivity.serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(this);
            ArrayList<Songs> songs = new ArrayList<>();
            songs.add(song);
            storage.storeAudio(songs);
            storage.storeAudioIndexAndPostion(0, 0);


            startService(new Intent(getApplicationContext(), MediaPlayerService.class));
            MainActivity.serviceBound = true;

        }


        else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(this);
            ArrayList<Songs> songs = new ArrayList<>();
            songs.add(song);
            storage.storeAudio(songs);
            storage.storeAudioIndexAndPostion(0, 0);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(SongsFragment.Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent nowPlaying = new Intent(SearchActivity.this, NowPlaying.class);
                nowPlaying.putExtra("SEARCH", true);
                startActivity(nowPlaying);
            }
        }, 1000);

    }

    @Override
    public void onPlaylistItemClicked(int index, long id, Dialog dialog) {

    }

    @Override
    public void onPlaylistItemClicked(long id, Dialog dialog, final ArrayList<Songs> mySongs) {
        final ContentResolver contentResolver = getContentResolver();

        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", id);

        final ContentValues contentValues = new ContentValues();

        dialog.dismiss();

        new Runnable() {
            @Override
            public void run() {

                int playOrder = 1;

                Cursor cursor = contentResolver.query(uri, new String[]{"max(play_order)"}, null, null, null);

                if (cursor != null && cursor.getCount() > 0) {

                    cursor.moveToFirst();

                    do {
                        playOrder = cursor.getInt(0) + 1;
                    } while (cursor.moveToNext());

                    cursor.close();
                }


                for (int i =0; i<mySongs.size(); i++) {

                    contentValues.clear();
                    contentValues.put("audio_id", mySongs.get(i).getId());
                    contentValues.put("play_order", playOrder);
                    contentResolver.insert(uri, contentValues);
                    playOrder++;
                }
                if (mySongs.size()>1) Toast.makeText(SearchActivity.this, mySongs.size() + "songs have been added to the playlist!", Toast.LENGTH_SHORT).show();
                else Toast.makeText(SearchActivity.this, "1 song has been added to the playlist!", Toast.LENGTH_SHORT).show();


            }
        }.run();

    }

    private class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate (R.menu.selection_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            ArrayList<Integer> indices = (ArrayList<Integer>) myAdapter.getSelectedItems();
            ArrayList<Songs> selectedSongs = new ArrayList<>();

            int songSize = 0;
            int albumSize = 0;

            if (songs.size() > 0)songSize = songs.size()+1;
            if (albums.size() > 0) albumSize = albums.size()+1;

            for (int i=0; i<indices.size(); i++){
                if (indices.get(i) < songSize && songSize != 0) selectedSongs.add(songs.get(indices.get(i)-1));
                else if (indices.get(i) > songSize && indices.get(i) < songSize + albumSize && albumSize != 0) selectedSongs.addAll(DataLoader.loadAudio(albums.get(indices.get(i)-songSize-1).getId(), 1, SearchActivity.this, getSharedPreferences("AlbumSongSort", Context.MODE_PRIVATE)));
                else selectedSongs.addAll(myAdapter.loadArtistAudio(artists.get(indices.get(i)-songSize-albumSize-1).getId()));
            }

            switch (item.getTitle().toString()) {

                case "Play":
                    DataLoader.playAudio(0, selectedSongs, storage, SearchActivity.this);
                    mode.finish();
                    return true;

                case "Enqueue":
                    if (MediaPlayerService.audioList != null) MediaPlayerService.audioList.addAll(selectedSongs);
                    else MediaPlayerService.audioList = selectedSongs;
                    storage.storeAudio(MediaPlayerService.audioList);
                    if (selectedSongs.size()>1) Toast.makeText(SearchActivity.this, selectedSongs.size() + "songs have been added to the queue!", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(SearchActivity.this, "1 song has been added to the queue!", Toast.LENGTH_SHORT).show();
                    mode.finish();
                    return true;

                case "Play next":
                    if (MediaPlayerService.audioList != null) MediaPlayerService.audioList.addAll(MediaPlayerService.audioIndex+1, selectedSongs);
                    else MediaPlayerService.audioList = selectedSongs;
                    storage.storeAudio(MediaPlayerService.audioList);
                    if (selectedSongs.size()>1) Toast.makeText(SearchActivity.this, selectedSongs.size() + "songs have been added to the queue!", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(SearchActivity.this, "1 song has been added to the queue!", Toast.LENGTH_SHORT).show();
                    mode.finish();
                    return true;

                case "Shuffle":
                    playAudio(selectedSongs);
                    mode.finish();
                    return true;

                case "Add to playlist":
                    DataLoader.addToPlaylist(selectedSongs, SearchActivity.this, null);
                    mode.finish();
                    return true;

                case "Delete":
                    SongsFragment.deleteSongs(selectedSongs, SearchActivity.this);
                    myAdapter.notifyDataSetChanged();
                    mode.finish();
                    return true;


                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            myAdapter.clearSelection();
            actionMode = null;
        }
    }

    private void playAudio(ArrayList<Songs> songs) {

        if (!MainActivity.serviceBound) {
            //Store Serializable audioList to SharedPreferences
            storage.storeAudio(songs);
            storage.storeAudioIndexAndPostion(0, 0);

            Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
            intent.setAction("SHUFFLE");
            startService(intent);
            MainActivity.serviceBound = true;

        }


        else {
            //Store the new audioIndex to SharedPreferences
            storage.storeAudio(songs);
            storage.storeAudioIndexAndPostion(0, 0);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(SongsFragment.Broadcast_PLAY_NEW_AUDIO);
            broadcastIntent.setAction("SHUFFLE");
            sendBroadcast(broadcastIntent);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                startActivity(new Intent(SearchActivity.this, NowPlaying.class));
            }
        }, 1000);

    }


    private void toggleSelection(int position) {
        myAdapter.toggleSelection(position);
        int count = myAdapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 69){
            if (resultCode == RESULT_OK){

                try {
                    RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaPlayerService.activeAudio.getId()));
                    Toast.makeText(this, MediaPlayerService.activeAudio.getTitle() + " set as Ringtone!", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Permission was not Granted!", Toast.LENGTH_SHORT).show();
                }

            }
            else {

                Toast.makeText(this, "Permission was not Granted!", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    protected void onDestroy() {
        recyclerView = null;
        myAdapter = null;
        searchView = null;
        layoutManager = null;
        storage = null;
        songs = null;
        albums = null;
        artists = null;
        actionMode = null;
        actionModeCallback = null;

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(isTaskRoot()){
            startActivity(new Intent(this, MainActivity.class));
        }
        super.onBackPressed();
    }
}

