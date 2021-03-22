package com.example.musicplayer.ui.songs;

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
import android.widget.TextView;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageUtil;
import com.example.musicplayer.adapters.ItemClicked;
import com.example.musicplayer.adapters.PlaylistItemClicked;
import com.example.musicplayer.adapters.SongAdapter;
import com.example.musicplayer.adapters.SongChanged;
import com.example.musicplayer.database.DataLoader;
import com.example.musicplayer.database.Songs;
import com.example.musicplayer.nowplaying.NowPlaying;
import com.example.musicplayer.ui.SettingsActivity;
import com.example.musicplayer.ui.equalizer.EqualizerActivity;
import com.example.musicplayer.ui.folders.FoldersFragment;
import com.example.musicplayer.ui.search.SearchActivity;
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import static android.app.Activity.RESULT_OK;
import static com.example.musicplayer.MainActivity.UNIQUE_REQUEST_CODE;
import static com.example.musicplayer.MainActivity.index;

public class SongsFragment extends Fragment implements ItemClicked, PlaylistItemClicked, SongChanged {

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.musicplayer.PlayNewAudio";
    private boolean shouldRefresh = false;

    private ArrayList<Songs> mySongs;

    private Parcelable recyclerviewParcelable;

    private SongsViewModel songsViewModel;
    private RecyclerView recyclerView;
    private SongAdapter myAdapter;
    private View root;
    private RecyclerViewFastScroller fastScroller;

    private SharedPreferences sort;
    private StorageUtil storage;
    private Menu menu;
    private Context context;

    private ActionMode actionMode;
    private final ActionModeCallback actionModeCallback = new ActionModeCallback();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_songs, container, false);

        context = getContext();
        sort = getActivity().getSharedPreferences("Sort", Context.MODE_PRIVATE);
        storage = new StorageUtil(context);

        recyclerView = root.findViewById(R.id.songs_rv);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(30);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        setHasOptionsMenu(true);

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Toolbar toolbar = ((AppCompatActivity)getActivity()).findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.nav_green);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        mySongs = new ArrayList<>();
        
        fastScroller = root.findViewById(R.id.fast_scroller);

        if (!sort.getString("SongSortOrder", MediaStore.Audio.Media.DEFAULT_SORT_ORDER).equals(MediaStore.Audio.Media.DEFAULT_SORT_ORDER)){
            fastScroller.setPopupDrawable(null);
        }


        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, UNIQUE_REQUEST_CODE);
        }
        else{

            songsViewModel = new ViewModelProvider(this).get(SongsViewModel.class);

            songsViewModel.getSongs().observe(getViewLifecycleOwner(), new Observer<ArrayList<Songs>>() {
                @Override
                public void onChanged(ArrayList<Songs> songs) {


                    mySongs = songs;
                    songAdapterRunnable.run();
                }
            });

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

        if (mySongs.size()<25){
            fastScroller.setTrackDrawable(null);
        }

    }


    private final Runnable songAdapterRunnable =  new Runnable() {
        @Override
        public void run() {
            if (getActivity() != null) {

                myAdapter = new SongAdapter(context, FragmentManager.findFragment(root), mySongs);
                recyclerView.setAdapter(myAdapter);
                if (recyclerviewParcelable != null){
                    recyclerView.getLayoutManager().onRestoreInstanceState(recyclerviewParcelable);
                }
            }
        }
    };


    @Override
    public void onItemClicked(int index) {

        if (actionMode != null) {
            toggleSelection(index);
        }
        else{
            DataLoader.playAudio(index, mySongs, storage, context);
            myAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemLongClicked(int index) {
        if (actionMode == null) actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(actionModeCallback);
        toggleSelection(index);
    }


    @Override
    public void onPlaylistItemClicked(int index, long id, Dialog dialog) {

        ContentResolver contentResolver = context.getContentResolver();

        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", id);

        ContentValues contentValues = new ContentValues();

        contentValues.put("audio_id", mySongs.get(index).getId());

        int playOrder = 1;

        Cursor cursor = contentResolver.query(uri, new String[]{"max(play_order)"}, null, null,null);

        if (cursor != null && cursor.getCount()>0){

            cursor.moveToFirst();

            do {
                playOrder = cursor.getInt(0) + 1;
            }while (cursor.moveToNext());

            cursor.close();
        }

        contentValues.put("play_order", playOrder);
        contentResolver.insert(uri,contentValues);
        dialog.dismiss();

        Toast.makeText(context, "Song has been added to the playlist!", Toast.LENGTH_SHORT).show();

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
                    DataLoader.playAudio(0,mySongs, storage, context);
                    break;

                case "Shuffle All":
                    DataLoader.playAudio(new Random().nextInt(mySongs.size()), mySongs, storage, context);
                    NowPlaying.shuffle = true;
                    break;

                case "Save Now Playing":
                    DataLoader.addToPlaylist(MediaPlayerService.audioList, context, SongsFragment.this);
                    break;

                case "Settings":
                    startActivity(new Intent(context, SettingsActivity.class));
                    break;

                case "Title":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("SongIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("SongIndex",0).putString("SongSortOrder", MediaStore.Audio.Media.DEFAULT_SORT_ORDER).apply();
                        songsViewModel.refresh();
                        fastScroller.setPopupDrawable(getResources().getDrawable(R.drawable.fsp));
                    }
                    break;

                case "Album":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("SongIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("SongIndex",1).putString("SongSortOrder", "album").apply();
                        songsViewModel.refresh();
                        fastScroller.setPopupDrawable(null);
                    }
                    break;

                case "Artist":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("SongIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("SongIndex",2).putString("SongSortOrder", "artist").apply();
                        songsViewModel.refresh();
                        fastScroller.setPopupDrawable(null);
                    }
                    break;

                case "Duration":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("SongIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("SongIndex",3).putString("SongSortOrder", "duration").apply();
                        songsViewModel.refresh();
                        fastScroller.setPopupDrawable(null);
                    }
                    break;

                case "Year":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("SongIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("SongIndex",4).putString("SongSortOrder", "year").apply();
                        songsViewModel.refresh();
                        fastScroller.setPopupDrawable(null);
                    }
                    break;

                case "Date added":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("SongIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("SongIndex",5).putString("SongSortOrder", "date_added").apply();
                        songsViewModel.refresh();
                        fastScroller.setPopupDrawable(null);
                    }
                    break;

                case "Reverse order":
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putBoolean("SongReverse", true).apply();
                    }
                    else {
                        item.setChecked(false);
                        sort.edit().putBoolean("SongReverse", false).apply();
                    }
                    songsViewModel.refresh();
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
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_num_songs);
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_track);
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_num_album);
        menu.getItem(3).getSubMenu().getItem(sort.getInt("SongIndex",0)).setChecked(true);
        if (sort.getBoolean("SongReverse", false)) menu.getItem(3).getSubMenu().getItem(6).setChecked(true);

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
                songs.add(mySongs.get(indices.get(i)));
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
                    DataLoader.addToPlaylist(songs, context, SongsFragment.this);
                    mode.finish();
                    return true;

                case "Delete":
                    deleteSongs(songs, context);
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


    public static void deleteSongs(final ArrayList<Songs> songs, final Context context){

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
                    }
                }.run();

            }
        });



    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 421) {

            if (resultCode == RESULT_OK) {
                ArrayList<String> newData = data.getStringArrayListExtra("data");
                if (newData != null) {

                    int x = -1;

                    for(int i = 0; i<MediaPlayerService.audioList.size(); i++){
                        Songs song = MediaPlayerService.audioList.get(i);
                        if (song.getId() == (mySongs.get(index).getId())){
                            x = i;
                            break;
                        }
                    }

                    if (x != -1){
                        MediaPlayerService.audioList.get(x).setTitle(newData.get(0));
                        MediaPlayerService.audioList.get(x).setAlbum(newData.get(1));
                        MediaPlayerService.audioList.get(x).setArtist(newData.get(2));
                        storage.storeAudio(MediaPlayerService.audioList);
                    }

                    mySongs.get(index).setTitle(newData.get(0));
                    mySongs.get(index).setAlbum(newData.get(1));
                    mySongs.get(index).setArtist(newData.get(2));
                    myAdapter.notifyItemChanged(index);

                }
            }
        }
        else super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == UNIQUE_REQUEST_CODE){

            try {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    songsViewModel =
                            new ViewModelProvider(this).get(SongsViewModel.class);

                    songsViewModel.getSongs().observe(getViewLifecycleOwner(), new Observer<ArrayList<Songs>>() {
                        @Override
                        public void onChanged(ArrayList<Songs> songs) {

                            mySongs = songs;
                            songAdapterRunnable.run();
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
    public void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE )== PackageManager.PERMISSION_GRANTED) {
            if (shouldRefresh) songsViewModel.refresh();
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
    public void onDestroyView() {

        Glide.get(context).clearMemory();

        songsViewModel = null;
        recyclerView = null;
        myAdapter = null;
        root = null;
        fastScroller = null;
        sort = null;
        storage = null;
        menu = null;
        context = null;
        actionMode = null;

        super.onDestroyView();
    }


}


