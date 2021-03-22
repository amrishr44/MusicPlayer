package com.example.musicplayer.ui.albums;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.example.musicplayer.ui.equalizer.EqualizerActivity;
import com.example.musicplayer.ui.folders.FoldersFragment;
import com.example.musicplayer.ui.search.SearchActivity;
import com.example.musicplayer.ui.songs.SongsFragment;
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller;

import java.util.ArrayList;
import java.util.Random;

import static com.example.musicplayer.MainActivity.UNIQUE_REQUEST_CODE;

public class AlbumsFragment extends Fragment implements ItemClicked, PlaylistItemClicked, SongChanged {

    public static Albums album;
    private ArrayList<Albums> myAlbums;

    private boolean shouldRefresh = false;
    private Parcelable recyclerviewParcelable;

    private Menu menu;
    private SharedPreferences sort;
    private StorageUtil storage;
    private Context context;

    private AlbumsViewModel albumsViewModel;
    private RecyclerView recyclerView;
    private View root;
    private AlbumsAdapter myAdapter;
    RecyclerViewFastScroller fastScroller;

    private ActionMode actionMode;
    private final AlbumsFragment.ActionModeCallback actionModeCallback = new AlbumsFragment.ActionModeCallback();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        context = getContext();
        sort = getActivity().getSharedPreferences("Sort", Context.MODE_PRIVATE);
        storage = new StorageUtil(context);

        root = inflater.inflate(R.layout.fragment_albums, container, false);

        Toolbar toolbar = ((AppCompatActivity)getActivity()).findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.nav_green);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        recyclerView = root.findViewById(R.id.albums_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, UNIQUE_REQUEST_CODE);

        }
        else{

            albumsViewModel = new ViewModelProvider(this).get(AlbumsViewModel.class);


            albumsViewModel.getAlbums().observe(getViewLifecycleOwner(), new Observer<ArrayList<Albums>>() {
                @Override
                public void onChanged(ArrayList<Albums> albums) {

                    if (!albums.equals(myAlbums)) {
                        myAlbums = albums;
                        albumAdapterRunnable.run();
                    }
                }
            });

        }

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        fastScroller = root.findViewById(R.id.fast_scroller_album);

        if (!sort.getString("AlbumSortOrder", MediaStore.Audio.Albums.DEFAULT_SORT_ORDER).equals(MediaStore.Audio.Albums.DEFAULT_SORT_ORDER)){
            fastScroller.setPopupDrawable(null);
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState != RecyclerView.SCROLL_STATE_DRAGGING  && newState != RecyclerView.SCROLL_STATE_SETTLING){

                    fastScroller.setTrackDrawable(null);
                    fastScroller.setHandleDrawable(getResources().getDrawable(R.drawable.fastscrollbar_hidden));
                }
                else{
                    fastScroller.setTrackDrawable(getResources().getDrawable(R.drawable.fst));
                    fastScroller.setHandleDrawable(getResources().getDrawable(R.drawable.fastscrollbar));
                }
            }
        });

        fastScroller.setHandleStateListener(new RecyclerViewFastScroller.HandleStateListener() {
            @Override
            public void onEngaged() {
                fastScroller.setTrackDrawable(getResources().getDrawable(R.drawable.fst));
                fastScroller.setHandleDrawable(getResources().getDrawable(R.drawable.fastscrollbar));
            }

            @Override
            public void onDragged(float v, int i) {
                fastScroller.setTrackDrawable(getResources().getDrawable(R.drawable.fst));
                fastScroller.setHandleDrawable(getResources().getDrawable(R.drawable.fastscrollbar));
            }

            @Override
            public void onReleased() {

                fastScroller.setTrackDrawable(null);
                fastScroller.setHandleDrawable(getResources().getDrawable(R.drawable.fastscrollbar_hidden));
            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE )== PackageManager.PERMISSION_GRANTED) {

            if (shouldRefresh) albumsViewModel.refresh();
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

            album = myAlbums.get(index);
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_albums_to_albumSongsFragment2);
        }
        else toggleSelection(index);
    }

    @Override
    public void onItemLongClicked(int index) {
        if (actionMode == null) actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(actionModeCallback);
        toggleSelection(index);
    }


    private final Runnable albumAdapterRunnable = new Runnable() {
        @Override
        public void run() {

            myAdapter = new AlbumsAdapter(myAlbums, FragmentManager.findFragment(root), context);
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
                    startActivity(new Intent(getContext(), SearchActivity.class));
                    break;

                case "Equalizer":
                    startActivity(new Intent(getContext(), EqualizerActivity.class));
                    break;

                case "Play All":
                    ArrayList<Songs> songs = new ArrayList<>();
                    for (int i = 0; i<myAlbums.size(); i++){
                        songs.addAll(DataLoader.loadAudio(myAlbums.get(i).getId(), 1, context, sort));

                    }
                    DataLoader.playAudio(0, songs, storage, context);
                    break;

                case "Shuffle All":
                    ArrayList<Songs> shuffleSongs = new ArrayList<>();
                    for (int i = 0; i<myAlbums.size(); i++){
                        shuffleSongs.addAll(DataLoader.loadAudio(myAlbums.get(i).getId(), 1, context, sort));

                    }
                    DataLoader.playAudio(new Random().nextInt(shuffleSongs.size()), shuffleSongs, storage, context);
                    NowPlaying.shuffle = true;
                    break;

                case "Save Now Playing":
                    DataLoader.addToPlaylist(MediaPlayerService.audioList, getContext(), AlbumsFragment.this);
                    break;

                case "Settings":
                    startActivity(new Intent(getContext(), SettingsActivity.class));
                    break;

                case "Title":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("AlbumIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("AlbumIndex",0).putString("AlbumSortOrder", MediaStore.Audio.Albums.DEFAULT_SORT_ORDER).apply();
                        albumsViewModel.refresh();
                        fastScroller.setPopupDrawable(getResources().getDrawable(R.drawable.fsp));
                    }
                    break;

                case "Artist":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("AlbumIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("AlbumIndex",1).putString("AlbumSortOrder", "artist").apply();
                        albumsViewModel.refresh();
                        fastScroller.setPopupDrawable(null);
                    }
                    break;


                case "Year":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("AlbumIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("AlbumIndex",2).putString("AlbumSortOrder", "minyear").apply();
                        albumsViewModel.refresh();
                        fastScroller.setPopupDrawable(null);
                    }
                    break;

                case "Number of songs":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("AlbumIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("AlbumIndex",3).putString("AlbumSortOrder", "numsongs").apply();
                        albumsViewModel.refresh();
                        fastScroller.setPopupDrawable(null);
                    }
                    break;

                case "Reverse order":
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putBoolean("AlbumReverse", true).apply();
                    }
                    else {
                        item.setChecked(false);
                        sort.edit().putBoolean("AlbumReverse", false).apply();
                    }
                    albumsViewModel.refresh();
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
        menu.getItem(3).getSubMenu().getItem(sort.getInt("AlbumIndex",0)).setChecked(true);
        if (sort.getBoolean("AlbumReverse", false)) menu.getItem(3).getSubMenu().getItem(4).setChecked(true);

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
                songs.addAll(DataLoader.loadAudio(myAlbums.get(indices.get(i)).getId(), 1, context, sort));

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
                    DataLoader.addToPlaylist(songs, context, AlbumsFragment.this);
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
    public void onDestroyView() {
        menu = null;
        sort = null;
        storage = null;
        context = null;
        albumsViewModel = null;
        recyclerView = null;
        root = null;
        myAdapter = null;
        fastScroller = null;
        actionMode = null;

        super.onDestroyView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == UNIQUE_REQUEST_CODE){

            try {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    albumsViewModel = new ViewModelProvider(this).get(AlbumsViewModel.class);


                    albumsViewModel.getAlbums().observe(getViewLifecycleOwner(), new Observer<ArrayList<Albums>>() {
                        @Override
                        public void onChanged(ArrayList<Albums> albums) {

                            myAlbums = albums;
                            albumAdapterRunnable.run();
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
