package com.example.musicplayer.ui.artists;

import android.Manifest;
import android.app.Activity;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageUtil;
import com.example.musicplayer.adapters.ArtistsAdapter;
import com.example.musicplayer.adapters.ItemClicked;
import com.example.musicplayer.adapters.PlaylistItemClicked;
import com.example.musicplayer.adapters.SongChanged;
import com.example.musicplayer.database.Artists;
import com.example.musicplayer.database.DataLoader;
import com.example.musicplayer.database.Songs;
import com.example.musicplayer.ui.SettingsActivity;
import com.example.musicplayer.ui.equalizer.EqualizerActivity;
import com.example.musicplayer.ui.folders.FoldersFragment;
import com.example.musicplayer.ui.search.SearchActivity;
import com.example.musicplayer.ui.songs.SongsFragment;
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller;

import java.util.ArrayList;
import java.util.Random;

import static com.example.musicplayer.MainActivity.UNIQUE_REQUEST_CODE;

public class ArtistsFragment extends Fragment implements ItemClicked, PlaylistItemClicked, SongChanged {

    public static Artists artist;
    private boolean shouldRefresh = false;

    private Menu menu;
    private SharedPreferences sort;
    private StorageUtil storage;
    private Context context;

    private ArtistsViewModel artistsViewModel;
    private RecyclerView recyclerView;
    private ArrayList<Artists> myArtists;
    private View root;
    private ArtistsAdapter myAdapter;
    RecyclerViewFastScroller fastScroller;

    private ActionMode actionMode;
    private final ArtistsFragment.ActionModeCallback actionModeCallback = new ArtistsFragment.ActionModeCallback();


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        context = getContext();
        sort = getActivity().getSharedPreferences("Sort", Context.MODE_PRIVATE);
        storage = new StorageUtil(context);

        setHasOptionsMenu(true);

        root = inflater.inflate(R.layout.artists_fragment, container, false);

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Toolbar toolbar = ((AppCompatActivity)getActivity()).findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.nav_green);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        recyclerView = root.findViewById(R.id.artists_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));
        recyclerView.setHasFixedSize(true);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, UNIQUE_REQUEST_CODE);
        }
        else {

            artistsViewModel = ViewModelProviders.of(this).get(ArtistsViewModel.class);

            artistsViewModel.getArtists().observe(getViewLifecycleOwner(), new Observer<ArrayList<Artists>>() {
                @Override
                public void onChanged(ArrayList<Artists> artists) {

                    myArtists = artists;
                    artistsAdapterRunnable.run();
                }
            });

        }


        fastScroller = root.findViewById(R.id.fast_scroller_artist);

        if (!sort.getString("ArtistSortOrder", MediaStore.Audio.Artists.DEFAULT_SORT_ORDER).equals(MediaStore.Audio.Artists.DEFAULT_SORT_ORDER)){
            fastScroller.setPopupDrawable(null);
        }

        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
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

    private final Runnable artistsAdapterRunnable = new Runnable() {
        @Override
        public void run() {

            myAdapter = new ArtistsAdapter(myArtists, FragmentManager.findFragment(root), context);
            recyclerView.setAdapter(myAdapter);
            recyclerView.scrollToPosition(MainActivity.index);

        }
    };


    @Override
    public void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE )== PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context,Manifest.permission.MEDIA_CONTENT_CONTROL) == PackageManager.PERMISSION_GRANTED) {

            if (shouldRefresh) artistsViewModel.refresh();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == UNIQUE_REQUEST_CODE){

            try {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    artistsViewModel = ViewModelProviders.of(this).get(ArtistsViewModel.class);

                    artistsViewModel.getArtists().observe(getViewLifecycleOwner(), new Observer<ArrayList<Artists>>() {
                        @Override
                        public void onChanged(ArrayList<Artists> artists) {

                            myArtists = artists;
                            artistsAdapterRunnable.run();
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

    @Override
    public void onPause() {
        super.onPause();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE )== PackageManager.PERMISSION_GRANTED) {

            shouldRefresh = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        shouldRefresh = false;
    }


    @Override
    public void onItemClicked(int index) {

        if (actionMode == null) {
            MainActivity.index = index;
            artist = myArtists.get(index);
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_artists_to_artist_albums_frag);
        }
        else toggleSelection(index);
    }

    @Override
    public void onItemLongClicked(int index) {

        if (actionMode == null) {

            actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(actionModeCallback);
        }

        toggleSelection(index);
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

                    contentValues.put("audio_id", mySongs.get(i).getId());

                    contentValues.put("play_order", playOrder);

                    contentResolver.insert(uri, contentValues);
                }

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
                    for (int i = 0; i<myArtists.size(); i++){
                        songs.addAll(DataLoader.loadAudio(myArtists.get(i).getId(), 2, context, sort));
                    }
                    DataLoader.playAudio(0, songs, storage, context);
                    break;

                case "Shuffle All":
                    ArrayList<Songs> shuffleSongs = new ArrayList<>();
                    for (int i = 0; i<myArtists.size(); i++){
                        shuffleSongs.addAll(DataLoader.loadAudio(myArtists.get(i).getId(), 2, context, sort));
                    }
                    DataLoader.playAudio(new Random().nextInt(shuffleSongs.size()), shuffleSongs, storage, context);
                    MediaControllerCompat.getMediaController((Activity) context).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                    break;

                case "Save Now Playing":
                    DataLoader.addToPlaylist(MediaPlayerService.audioList, context, ArtistsFragment.this);
                    break;

                case "Settings":
                    startActivity(new Intent(getContext(), SettingsActivity.class));
                    break;


                case "Title":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("ArtistIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("ArtistIndex",0).putString("ArtistSortOrder", MediaStore.Audio.Artists.DEFAULT_SORT_ORDER).apply();
                        artistsViewModel.refresh();
                        fastScroller.setPopupDrawable(getResources().getDrawable(R.drawable.fsp));
                    }
                    break;

                case "Number of albums":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("ArtistIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("ArtistIndex",1).putString("ArtistSortOrder", "number_of_albums").apply();
                        artistsViewModel.refresh();
                        fastScroller.setPopupDrawable(null);
                    }
                    break;

                case "Number of songs":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("ArtistIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("ArtistIndex",2).putString("ArtistSortOrder", "number_of_tracks").apply();
                        artistsViewModel.refresh();
                        fastScroller.setPopupDrawable(null);
                    }
                    break;


                case "Reverse order":
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putBoolean("ArtistReverse", true).apply();
                        artistsViewModel.refresh();
                    }
                    else {
                        item.setChecked(false);
                        sort.edit().putBoolean("ArtistReverse", false).apply();
                        artistsViewModel.refresh();
                    }
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
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_year);
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_artist);
        menu.getItem(3).getSubMenu().getItem(sort.getInt("ArtistIndex",0)).setChecked(true);
        if (sort.getBoolean("ArtistReverse", false)) menu.getItem(3).getSubMenu().getItem(3).setChecked(true);

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
                songs.addAll(DataLoader.loadAudio(myArtists.get(indices.get(i)).getId(), 2, context, sort));

            }

            switch (item.getItemId()) {

                case R.id.selected_play:
                    DataLoader.playAudio(0, songs, storage, context);
                    mode.finish();
                    return true;

                case R.id.selected_enqueue:
                    if (MediaPlayerService.audioList != null) MediaPlayerService.audioList.addAll(songs);
                    else MediaPlayerService.audioList = songs;
                    storage.storeAudio(MediaPlayerService.audioList);
                    mode.finish();
                    return true;

                case R.id.selected_play_next:
                    if (MediaPlayerService.audioList != null) MediaPlayerService.audioList.addAll(MediaPlayerService.audioIndex+1, songs);
                    else MediaPlayerService.audioList = songs;
                    storage.storeAudio(MediaPlayerService.audioList);
                    mode.finish();
                    return true;

                case R.id.selected_shuffle:
                    DataLoader.playAudio(0, songs, storage, context);
                    MediaControllerCompat.getMediaController(getActivity()).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                    mode.finish();
                    return true;

                case R.id.selected_add_to_playlist:
                    DataLoader.addToPlaylist(songs, context, ArtistsFragment.this);
                    mode.finish();
                    return true;

                case R.id.selected_delete:
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
        artistsViewModel = null;
        recyclerView = null;
        myArtists = null;
        root = null;
        myAdapter = null;
        fastScroller = null;
        actionMode = null;

        super.onDestroyView();
    }
}
