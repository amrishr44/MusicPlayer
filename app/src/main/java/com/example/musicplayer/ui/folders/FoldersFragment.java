package com.example.musicplayer.ui.folders;

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
import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageUtil;
import com.example.musicplayer.adapters.FolderItemClicked;
import com.example.musicplayer.adapters.FoldersAdapter;
import com.example.musicplayer.adapters.ItemClicked;
import com.example.musicplayer.adapters.PlaylistItemClicked;
import com.example.musicplayer.adapters.SongChanged;
import com.example.musicplayer.database.DataLoader;
import com.example.musicplayer.database.Songs;
import com.example.musicplayer.ui.SettingsActivity;
import com.example.musicplayer.ui.equalizer.EqualizerActivity;
import com.example.musicplayer.ui.search.SearchActivity;
import com.example.musicplayer.ui.songs.SongsFragment;
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static android.app.Activity.RESULT_OK;
import static com.example.musicplayer.MainActivity.UNIQUE_REQUEST_CODE;
import static com.example.musicplayer.MainActivity.index;


public class FoldersFragment extends Fragment implements ItemClicked, FolderItemClicked, PlaylistItemClicked, SongChanged {

    public static int slashCount = 0;
    public static String currentPath = "/";
    public static boolean isFolders;

    private ArrayList<Songs> mySongs;
    private List<String> list;
    private ArrayMap<String, Integer> folders2;

    private View root;
    private Menu menu;
    private SharedPreferences sort;
    private StorageUtil storage;
    private Context context;

    private RecyclerView foldersRecyclerView;
    private FoldersAdapter myAdapter;
    RecyclerViewFastScroller fastScroller;

    private ActionMode actionMode;
    private final FoldersFragment.ActionModeCallback actionModeCallback = new FoldersFragment.ActionModeCallback();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_folders, container, false);

        Toolbar toolbar = ((AppCompatActivity) getActivity()).findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.nav_green);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        setHasOptionsMenu(true);

        context = getContext();
        sort = getActivity().getSharedPreferences("Sort", Context.MODE_PRIVATE);
        storage = new StorageUtil(context);

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        foldersRecyclerView = root.findViewById(R.id.folders_recycler_view);
        foldersRecyclerView.setHasFixedSize(true);
        foldersRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, UNIQUE_REQUEST_CODE);

        }
        else{
            loading();
        }

        fastScroller = root.findViewById(R.id.fast_scroller_album);
        fastScroller.setPopupDrawable(null);

        foldersRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == UNIQUE_REQUEST_CODE){

            try {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    loading();
                }
                else{

                    Toast.makeText(getContext(), "What the hell bro!", Toast.LENGTH_SHORT).show();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        isFolders = true;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        isFolders = false;
    }



    private ArrayList<Songs> loadAudio()
    {
        ArrayList<Songs> arrayList = new ArrayList<>();

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0 AND " + MediaStore.Audio.Media.DATA + " LIKE ? ";
        String sortOrder;
        if (sort.getBoolean("FolderReverse", false)) sortOrder = sort.getString("FolderSortOrder", MediaStore.Audio.Media.DEFAULT_SORT_ORDER) + " DESC";
        else sortOrder = sort.getString("FolderSortOrder", MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        Cursor cursor = contentResolver.query(uri, new String[]{"_id","_data", "title", "artist", "album", "duration", "track", "artist_id", "album_id", "year"}, selection, new String[]{currentPath + "%"}, sortOrder);

        folders2 = new ArrayMap<>();


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

                String pathSubstring = data.substring(1);
                int i=0;
                int slash;


                while(i<slashCount){

                    pathSubstring = pathSubstring.substring(pathSubstring.indexOf("/")+1);
                    i++;
                }

                slash = pathSubstring.indexOf("/");

                if (slash != -1) {

                    pathSubstring = pathSubstring.substring(0, slash);

                    if (folders2.containsKey(pathSubstring)) {

                        folders2.put(pathSubstring, folders2.get(pathSubstring)+1);
                    }
                    else {

                        folders2.put(pathSubstring, 1);
                    }

                }

                arrayList.add(new Songs(id, data, title, album, artist, artistId, albumId, trackNumber, duration, year));
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
        if (actionMode != null) {
            toggleSelection(index);
        }
        else{
            MainActivity.index = index;
            DataLoader.playAudio(index-list.size()-1, mySongs, storage, context);
            myAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemLongClicked(int index) {
        if (actionMode == null) actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(actionModeCallback);
        toggleSelection(index);
    }

    private ArrayList<Songs> loadSongs(String path){

        ArrayList<Songs> folderSongs = new ArrayList<>();

        if (path.lastIndexOf("/") != -1) {
            String z = path.substring(0, path.lastIndexOf("/"));

            for (int i = 0; i < mySongs.size(); i++) {

                String y = mySongs.get(i).getData();
                int x = y.lastIndexOf("/");
                y = y.substring(0, x);

                if (y.equals(z)) folderSongs.add(mySongs.get(i));
            }
            return folderSongs;
        }
        return null;
    }


    @Override
    public void onFolderItemClicked(int index) {

        if (actionMode == null) {
            MainActivity.index = 0;

            if (!FoldersFragment.currentPath.equals("/")) {

                if (index == 0) getActivity().onBackPressed();
                else {
                    currentPath = currentPath + list.get(index - 1) + "/";
                    slashCount++;

                    NavHostFragment.findNavController(this).navigate(R.id.action_nav_folders_self);
                }
            } else {
                currentPath = currentPath + list.get(index) + "/";
                slashCount++;

                NavHostFragment.findNavController(this).navigate(R.id.action_nav_folders_self);
            }
        }
        else {
            if (!FoldersFragment.currentPath.equals("/") && index == 0)  actionMode.finish();
            else toggleSelection(index);
        }
    }

    @Override
    public void onFolderItemLongClicked(int index) {

        if (index != 0 && !currentPath.equals("/")) {
            if (actionMode == null)
                actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
            toggleSelection(index);
        }
        else if (currentPath.equals("/")){
            if (actionMode == null)
                actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
            toggleSelection(index);
        }

    }


    private void loading(){

       String a;

       mySongs = new ArrayList<>();

       foldersRecyclerView.setAdapter(null);

       if (!currentPath.equals("")  && !currentPath.equals("/")){
           a = currentPath.substring(0,currentPath.lastIndexOf("/"));
           if (a.lastIndexOf("/") != -1) a= a.substring(a.lastIndexOf("/"));
           ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(a);
       }
       else ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("/");

       songLoader.run();
       songsInCurrentPath.run();
       folderAdapter.run();
   }

   private final Runnable songLoader = new Runnable() {
       @Override
       public void run() {
           mySongs = loadAudio();
       }
   };

    private final Runnable songsInCurrentPath = new Runnable() {
        @Override
        public void run() {
            mySongs = loadSongs(currentPath);
        }
    };

    private final Runnable folderAdapter = new Runnable() {
        @Override
        public void run() {

            list = new ArrayList<>(folders2.keySet());
            Collections.sort(list);
            myAdapter = new FoldersAdapter(getContext(), FragmentManager.findFragment(root), folders2, mySongs, list);
            foldersRecyclerView.setAdapter(myAdapter);
            foldersRecyclerView.scrollToPosition(MainActivity.index);
        }
    };


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
                    startActivity(new Intent(context, SearchActivity.class));
                    break;

                case "Equalizer":
                    startActivity(new Intent(context, EqualizerActivity.class));
                    break;

                case "Play All":
                    ArrayList<Songs> songs = myAdapter.loadFolderAudio(currentPath);
                    DataLoader.playAudio(0, songs, storage, context);
                    break;

                case "Shuffle All":
                    ArrayList<Songs> shuffleSongs = myAdapter.loadFolderAudio(currentPath);
                    DataLoader.playAudio(new Random().nextInt(shuffleSongs.size()), shuffleSongs, storage, context);
                    MediaControllerCompat.getMediaController((Activity) context).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                    break;

                case "Save Now Playing":
                    DataLoader.addToPlaylist(MediaPlayerService.audioList, context, FoldersFragment.this);
                    break;

                case "Settings":
                    startActivity(new Intent(context, SettingsActivity.class));
                    break;

                case "Title":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("FolderIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("FolderIndex",0).putString("FolderSortOrder", MediaStore.Audio.Media.DEFAULT_SORT_ORDER).apply();
                        loading();
                    }
                    break;

                case "Album":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("FolderIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("FolderIndex",1).putString("FolderSortOrder", "album").apply();
                        loading();
                    }
                    break;

                case "Artist":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("FolderIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("FolderIndex",2).putString("FolderSortOrder", "artist").apply();
                        loading();
                    }
                    break;

                case "Duration":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("FolderIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("FolderIndex",3).putString("FolderSortOrder", "duration").apply();
                        loading();
                    }
                    break;

                case "Year":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("FolderIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("FolderIndex",4).putString("FolderSortOrder", "year").apply();
                        loading();
                    }
                    break;

                case "Date added":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("FolderIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("FolderIndex",5).putString("FolderSortOrder", "date_added").apply();
                        loading();
                    }
                    break;

                case "Reverse order":
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putBoolean("FolderReverse", true).apply();
                    }
                    else {
                        item.setChecked(false);
                        sort.edit().putBoolean("FolderReverse", false).apply();
                    }
                    loading();
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
        menu.getItem(3).getSubMenu().getItem(sort.getInt("FolderIndex",0)).setChecked(true);
        if (sort.getBoolean("FolderReverse", false)) menu.getItem(3).getSubMenu().getItem(6).setChecked(true);

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

            if (!FoldersFragment.currentPath.equals("/")) {
                for (int i = 0; i < indices.size(); i++) {

                    if (indices.get(i) <= list.size()) {
                        songs.addAll(myAdapter.loadFolderAudio(FoldersFragment.currentPath + list.get(indices.get(i)-1) + "/"));
                    } else {
                        songs.add(mySongs.get(indices.get(i) - list.size() - 1));
                    }

                }
            }
            else songs.addAll(myAdapter.loadFolderAudio("/"));

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
                    DataLoader.addToPlaylist(songs, context, FoldersFragment.this);
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 422) {

            if (resultCode == RESULT_OK) {
                ArrayList<String> newData = data.getStringArrayListExtra("data");
                if (newData != null) {

                    int x = -1;

                    for(int i = 0; i<MediaPlayerService.audioList.size(); i++){
                        Songs song = MediaPlayerService.audioList.get(i);
                        if (song.getId() == (mySongs.get(index-list.size()-1).getId())){
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

                    mySongs.get(index-list.size()-1).setTitle(newData.get(0));
                    mySongs.get(index-list.size()-1).setAlbum(newData.get(1));
                    mySongs.get(index-list.size()-1).setArtist(newData.get(2));
                    myAdapter.notifyItemChanged(index);

                }
            }
        }
        else super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void onDestroyView() {
        mySongs = null;
        list = null;
        folders2 = null;
        root = null;
        menu = null;
        sort = null;
        storage = null;
        context = null;
        foldersRecyclerView = null;
        myAdapter = null;
        fastScroller = null;
        actionMode = null;

        super.onDestroyView();
    }
}
