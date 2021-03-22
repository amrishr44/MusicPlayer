package com.example.musicplayer.ui.playlists;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageUtil;
import com.example.musicplayer.adapters.ItemClicked;
import com.example.musicplayer.adapters.PlaylistItemClicked;
import com.example.musicplayer.adapters.PlaylistsAdapter;
import com.example.musicplayer.adapters.SongChanged;
import com.example.musicplayer.database.DataLoader;
import com.example.musicplayer.database.Playlists;
import com.example.musicplayer.database.Songs;
import com.example.musicplayer.nowplaying.NowPlaying;
import com.example.musicplayer.ui.SettingsActivity;
import com.example.musicplayer.ui.equalizer.EqualizerActivity;
import com.example.musicplayer.ui.folders.FoldersFragment;
import com.example.musicplayer.ui.search.SearchActivity;
import com.example.musicplayer.ui.songs.SongsFragment;

import java.util.ArrayList;
import java.util.Random;

import static com.example.musicplayer.MainActivity.UNIQUE_REQUEST_CODE;

public class PlaylistsFragment extends Fragment implements ItemClicked, PlaylistItemClicked, SongChanged {

    private boolean shouldRefresh = false;
    public static long playlistId;
    public static String playlistName;
    
    private Parcelable recyclerviewParcelable;

    private View root;
    private RecyclerView recyclerView;
    private ArrayList<Playlists> myPlaylists;
    private PlaylistsViewModel playlistsViewModel;

    private StorageUtil storage;
    private PlaylistsAdapter myAdapter;
    private Context context;

    private ActionMode actionMode;
    private final PlaylistsFragment.ActionModeCallback actionModeCallback = new PlaylistsFragment.ActionModeCallback();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        myPlaylists = new ArrayList<>();
        context = getContext();
        storage = new StorageUtil(context);

        root = inflater.inflate(R.layout.fragment_playlists, container, false);
        
        recyclerView = root.findViewById(R.id.playlists_recyler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(true);
        
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Toolbar toolbar = ((AppCompatActivity)getActivity()).findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.nav_green);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED) {

           requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, UNIQUE_REQUEST_CODE);

        }
        else{

            playlistsViewModel = new ViewModelProvider(this).get(PlaylistsViewModel.class);

            playlistsViewModel.getPlaylists().observe(getViewLifecycleOwner(), new Observer<ArrayList<Playlists>>() {
                @Override
                public void onChanged(ArrayList<Playlists> playlists) {

                    myPlaylists = playlists;
                    playlistloader.run();
                }
            });

        }
    }

    private final Runnable playlistloader = new Runnable() {
        @Override
        public void run() {
            myAdapter = new PlaylistsAdapter(context,PlaylistsFragment.this, myPlaylists);
            recyclerView.setAdapter(myAdapter);
            if (recyclerviewParcelable != null){
                recyclerView.getLayoutManager().onRestoreInstanceState(recyclerviewParcelable);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE )== PackageManager.PERMISSION_GRANTED) {
            if (shouldRefresh) playlistsViewModel.refresh();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE )== PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context,Manifest.permission.MEDIA_CONTENT_CONTROL) == PackageManager.PERMISSION_GRANTED) {
            shouldRefresh = true;
        }
        
        recyclerviewParcelable = recyclerView.getLayoutManager().onSaveInstanceState();
    }



    @Override
    public void onItemClicked(int index) {

        if (actionMode == null) {
            playlistId = myPlaylists.get(index).getId();
            playlistName = myPlaylists.get(index).getName();
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_playlists_to_playlistSongsFragment);
        }
        else toggleSelection(index);
    }

    @Override
    public void onItemLongClicked(int index) {
        if (actionMode == null) actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(actionModeCallback);
        toggleSelection(index);
    }


    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.removeItem(R.id.action_sort);
    }

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
                songs.addAll(DataLoader.loadAudio(myPlaylists.get(indices.get(i)).getId(), 3, context, null));
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
                    DataLoader.addToPlaylist(songs, context, PlaylistsFragment.this);
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
                    for (int i = 0; i<myPlaylists.size(); i++){
                        songs.addAll(DataLoader.loadAudio(myPlaylists.get(i).getId(), 3, context, null));

                    }
                    DataLoader.playAudio(0, songs, storage, context);
                    break;

                case "Shuffle All":
                    ArrayList<Songs> shuffleSongs = new ArrayList<>();
                    for (int i = 0; i<myPlaylists.size(); i++){
                        shuffleSongs.addAll(DataLoader.loadAudio(myPlaylists.get(i).getId(), 3, context, null));

                    }
                    DataLoader.playAudio(new Random().nextInt(shuffleSongs.size()), shuffleSongs, storage, context);
                    NowPlaying.shuffle = true;
                    break;

                case "Save Now Playing":
                    DataLoader.addToPlaylist(MediaPlayerService.audioList, context, PlaylistsFragment.this);
                    break;

                case "Settings":
                    startActivity(new Intent(context, SettingsActivity.class));
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
    public void onDestroyView() {
        root = null;
        recyclerView = null;
        playlistsViewModel = null;
        storage = null;
        myAdapter = null;
        context = null;
        actionMode = null;

        super.onDestroyView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == UNIQUE_REQUEST_CODE){

            try {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    playlistsViewModel =
                            new ViewModelProvider(this).get(PlaylistsViewModel.class);

                    playlistsViewModel.getPlaylists().observe(getViewLifecycleOwner(), new Observer<ArrayList<Playlists>>() {
                        @Override
                        public void onChanged(ArrayList<Playlists> playlists) {

                            myPlaylists = playlists;
                            playlistloader.run();
                        }
                    });

                }
                else{

                    Toast.makeText(context, "What the hell bro!", Toast.LENGTH_SHORT).show();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }


}
