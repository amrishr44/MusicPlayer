package com.example.musicplayer.ui.artists;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageUtil;
import com.example.musicplayer.adapters.AlbumsAdapter;
import com.example.musicplayer.adapters.ItemClicked;
import com.example.musicplayer.adapters.PlaylistItemClicked;
import com.example.musicplayer.adapters.SongChanged;
import com.example.musicplayer.database.Albums;
import com.example.musicplayer.database.DataLoader;
import com.example.musicplayer.database.Songs;
import com.example.musicplayer.nowplaying.NowPlaying;
import com.example.musicplayer.ui.SettingsActivity;
import com.example.musicplayer.ui.albums.AlbumsFragment;
import com.example.musicplayer.ui.equalizer.EqualizerActivity;
import com.example.musicplayer.ui.folders.FoldersFragment;
import com.example.musicplayer.ui.search.SearchActivity;
import com.example.musicplayer.ui.songs.SongsFragment;

import java.util.ArrayList;
import java.util.Random;


public class ArtistAlbumsFragment extends Fragment implements ItemClicked, PlaylistItemClicked, SongChanged {

    private Parcelable recyclerviewParcelable;

    private RecyclerView recyclerView;
    private View root;
    private ArrayList<Albums> artistAlbums;
    private AlbumsAdapter myAdapter;

    private Menu menu;
    private SharedPreferences sort;
    private StorageUtil storage;
    private Context context;

    private ActionMode actionMode;
    private final ArtistAlbumsFragment.ActionModeCallback actionModeCallback = new ArtistAlbumsFragment.ActionModeCallback();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        context = getContext();
        sort = getActivity().getSharedPreferences("Sort", Context.MODE_PRIVATE);
        storage = new StorageUtil(context);

        setHasOptionsMenu(true);

        root = inflater.inflate(R.layout.fragment_artist_albums, container, false);

        recyclerView = root.findViewById(R.id.artist_albums_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));

        return root;
    }



    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        ActionBar toolbar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        toolbar.setHomeAsUpIndicator(R.drawable.back_green);
        toolbar.setTitle(ArtistsFragment.artist.getArtist());

        artistAlbumsAdapterRunnable.run();
    }

    private ArrayList<Albums> loadArtistAlbums() {

        ArrayList<Albums> arrayList = new ArrayList<>();

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Artists.Albums.getContentUri("external", ArtistsFragment.artist.getId());

        String sortOrder;
        if (sort != null) {
            if (sort.getBoolean("ArtistAlbumReverse", false)) sortOrder = sort.getString("ArtistAlbumSortOrder", MediaStore.Audio.Albums.DEFAULT_SORT_ORDER) + " DESC";
            else sortOrder = sort.getString("ArtistAlbumSortOrder", MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
        }
        else sortOrder = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER;

        Cursor cursor = contentResolver.query(uri, new String[]{"_id","album","album_art", "numsongs", "artist"}, null, null, sortOrder);

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

    @Override
    public void onItemClicked(int index) {

        if (actionMode == null) {
            AlbumsFragment.album = artistAlbums.get(index);
            NavHostFragment.findNavController(this).navigate(R.id.action_artist_albums_frag_to_albumSongsFragment2);
        }
        else toggleSelection(index);
    }

    @Override
    public void onItemLongClicked(int index) {

        if (actionMode == null) actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(actionModeCallback);
        toggleSelection(index);

    }

    private final Runnable artistAlbumsAdapterRunnable = new Runnable() {
        @Override
        public void run() {

            artistAlbums = loadArtistAlbums();
            myAdapter = new AlbumsAdapter(artistAlbums, FragmentManager.findFragment(root), context);
            recyclerView.setAdapter(myAdapter);
            if (recyclerviewParcelable != null){
                recyclerView.getLayoutManager().onRestoreInstanceState(recyclerviewParcelable);
            }
        }
    };


    @Override
    public void onPlaylistItemClicked(int index, long id, Dialog dialog) {

    }

    @Override
    public void onPlaylistItemClicked(long id, Dialog dialog, final ArrayList<Songs> mySongs) {

        final ContentResolver contentResolver = context.getContentResolver();

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
                if (mySongs.size()>1) Toast.makeText(context, mySongs.size() + " songs have been added to the playlist!", Toast.LENGTH_SHORT).show();
                else Toast.makeText(context, "1 song has been added to the playlist!", Toast.LENGTH_SHORT).show();

            }
        }.run();

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {

            switch (item.getTitle().toString()) {

                case "Search":
                    FoldersFragment.currentPath = "/";
                    FoldersFragment.slashCount = 0;
                    startActivity(new Intent(context, SearchActivity.class));
                    break;

                case "Equalizer":
                    startActivity(new Intent(context, EqualizerActivity.class));
                    break;

                case "Play All":
                    ArrayList<Songs> songs = new ArrayList<>();
                    for (int i = 0; i<artistAlbums.size(); i++){
                        songs.addAll(DataLoader.loadAudio(artistAlbums.get(i).getId(), 1, context, sort));
                    }
                    DataLoader.playAudio(0, songs, storage, context);
                    break;

                case "Shuffle All":
                    ArrayList<Songs> shuffleSongs = new ArrayList<>();
                    for (int i = 0; i<artistAlbums.size(); i++){
                        shuffleSongs.addAll(DataLoader.loadAudio(artistAlbums.get(i).getId(), 1, context, sort));
                    }
                    DataLoader.playAudio(new Random().nextInt(shuffleSongs.size()), shuffleSongs, storage, context);
                    NowPlaying.shuffle = true;
                    break;

                case "Save Now Playing":
                    DataLoader.addToPlaylist(MediaPlayerService.audioList, context, ArtistAlbumsFragment.this);
                    break;

                case "Settings":
                    startActivity(new Intent(context, SettingsActivity.class));
                    break;


                case "Title":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("ArtistAlbumIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("ArtistAlbumIndex",0).putString("ArtistAlbumSortOrder", MediaStore.Audio.Albums.DEFAULT_SORT_ORDER).apply();
                        artistAlbumsAdapterRunnable.run();
                    }
                    break;


                case "Year":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("ArtistAlbumIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("ArtistAlbumIndex",1).putString("ArtistAlbumSortOrder", "minyear").apply();
                        artistAlbumsAdapterRunnable.run();
                    }
                    break;

                case "Number of songs":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("ArtistAlbumIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("ArtistAlbumIndex",2).putString("ArtistAlbumSortOrder", "numsongs").apply();
                        artistAlbumsAdapterRunnable.run();
                    }
                    break;

                case "Reverse order":
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putBoolean("ArtistAlbumReverse", true).apply();
                    }
                    else {
                        item.setChecked(false);
                        sort.edit().putBoolean("ArtistAlbumReverse", false).apply();
                    }
                    artistAlbumsAdapterRunnable.run();
                    break;

                default:
                    return super.onOptionsItemSelected(item);
            }
        } catch (Exception e) {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }


    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {

        this.menu = menu;
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_album);
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_duration);
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_date_added);
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_track);
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_num_album);
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_artist);
        menu.getItem(3).getSubMenu().getItem(sort.getInt("ArtistAlbumIndex",0)).setChecked(true);
        if (sort.getBoolean("ArtistAlbumReverse", false)) menu.getItem(3).getSubMenu().getItem(3).setChecked(true);

    }

    @Override
    public void onSongChanged() {
        myAdapter.notifyDataSetChanged();
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
            ArrayList<Songs> songs = new ArrayList<>();

            for (int i=0; i<indices.size(); i++){
                songs.addAll(DataLoader.loadAudio(artistAlbums.get(indices.get(i)).getId(), 1, context, sort));
            }

            switch (item.getTitle().toString()) {

                case "Play":
                    DataLoader.playAudio(0, songs, storage, context);
                    mode.finish();
                    return true;

                case "Enqueue":
                    if (MediaPlayerService.audioList != null) MediaPlayerService.audioList.addAll(songs);
                    else MediaPlayerService.audioList = songs;
                    storage.storeAudio(MediaPlayerService.audioList);
                    if (songs.size()>1) Toast.makeText(context, songs.size() + " songs have been added to the queue!", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(context, "1 song has been added to the queue!", Toast.LENGTH_SHORT).show();
                    mode.finish();
                    return true;

                case "Play next":
                    if (MediaPlayerService.audioList != null) MediaPlayerService.audioList.addAll(MediaPlayerService.audioIndex+1, songs);
                    else MediaPlayerService.audioList = songs;
                    storage.storeAudio(MediaPlayerService.audioList);
                    if (songs.size()>1) Toast.makeText(context, songs.size() + " songs have been added to the queue!", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(context, "1 song has been added to the queue!", Toast.LENGTH_SHORT).show();
                    mode.finish();
                    return true;

                case "Shuffle":
                    DataLoader.playAudio(0, songs, storage, context);
                    NowPlaying.shuffle = true;
                    mode.finish();
                    return true;

                case "Add to playlist":
                    DataLoader.addToPlaylist(songs, context, ArtistAlbumsFragment.this);
                    mode.finish();
                    return true;

                case "Delete":
                    SongsFragment.deleteSongs(songs, context);
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
    public void onPause() {
        super.onPause();

        recyclerviewParcelable = recyclerView.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onDestroyView() {
        recyclerView = null;
        root = null;
        artistAlbums = null;
        myAdapter = null;
        menu = null;
        sort = null;
        storage = null;
        context = null;
        actionMode = null;

        super.onDestroyView();
    }
}
