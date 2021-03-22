package com.example.musicplayer.ui.albums;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
import com.example.musicplayer.ui.songs.SongsFragment;

import java.util.ArrayList;
import java.util.Random;

import static android.app.Activity.RESULT_OK;
import static com.example.musicplayer.MainActivity.index;


public class AlbumSongsFragment extends Fragment implements ItemClicked, PlaylistItemClicked, SongChanged {

    Toast toast;

    private long album_id;
    private ArrayList<Songs> myAlbumSongs;
    private Parcelable recyclerviewParcelable;

    private SongAdapter myAdapter;
    private StorageUtil storage;
    private Context context;
    private SharedPreferences sort;
    private Menu menu;

    private View root;
    private RecyclerView recyclerView;

    private ActionMode actionMode;
    private final AlbumSongsFragment.ActionModeCallback actionModeCallback = new AlbumSongsFragment.ActionModeCallback();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        context = getContext();
        sort = getActivity().getSharedPreferences("Sort", Context.MODE_PRIVATE);
        storage = new StorageUtil(context);

        setHasOptionsMenu(true);

        root = inflater.inflate(R.layout.fragment_album_songs, container, false);

        recyclerView = root.findViewById(R.id.album_songs_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        album_id = AlbumsFragment.album.getId();
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar toolbar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        toolbar.setHomeAsUpIndicator(R.drawable.back_green);
        toolbar.setTitle(AlbumsFragment.album.getAlbum());

        TextView album_songs_album = root.findViewById(R.id.album_songs_album_name);
        TextView album_songs_artist = root.findViewById(R.id.album_songs_album_artist);
        TextView album_songs_num_songs = root.findViewById(R.id.album_song_num_songs);

        String string = AlbumsFragment.album.getNumSongs() + "";
        album_songs_num_songs.setText(string);
        album_songs_album.setText(AlbumsFragment.album.getAlbum());
        album_songs_artist.setText(AlbumsFragment.album.getArtist());


        ImageView app_bar_image = root.findViewById(R.id.app_bar_image);
        ImageView small_album_art = root.findViewById(R.id.small_album_art);


        Glide.with(this).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), album_id))
                .centerCrop().error(R.drawable.cassettes).into(app_bar_image);

        Glide.with(this).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), album_id))
                .centerCrop().error(R.drawable.cassettes).into(small_album_art);


        Button play_all = root.findViewById(R.id.play_all);
        Button shuffle_all = root.findViewById(R.id.shuffle_all);
        Button queue_all = root.findViewById(R.id.queue_all);

        play_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DataLoader.playAudio(0, myAlbumSongs, storage, context);
            }
        });

        shuffle_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DataLoader.playAudio(new Random().nextInt(myAlbumSongs.size()), myAlbumSongs, storage, context);
                MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
            }
        });

        queue_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (toast != null) toast.cancel();

                MediaPlayerService.audioList.addAll(myAlbumSongs);
                storage.storeAudio(MediaPlayerService.audioList);

                toast = Toast.makeText(context, "QUEUED", Toast.LENGTH_SHORT);
                toast.show();
            }
        });


    }


    private final Runnable albumSongsAdapterRunnable = new Runnable() {
        @Override
        public void run() {

            myAlbumSongs = DataLoader.loadAudio(album_id, 1, context, sort);
            myAdapter = new SongAdapter(context, FragmentManager.findFragment(root),myAlbumSongs);
            recyclerView.setAdapter(myAdapter);
            if (recyclerviewParcelable != null){
                recyclerView.getLayoutManager().onRestoreInstanceState(recyclerviewParcelable);
            }
        }
    };

    @Override
    public void onItemClicked(int index) {

        if (actionMode == null) {
            DataLoader.playAudio(index, myAlbumSongs, storage, context);
        }
        else toggleSelection(index);

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

        contentValues.put("audio_id", myAlbumSongs.get(index).getId());

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
        Toast.makeText(context, "Song has been added to the playlist!", Toast.LENGTH_SHORT).show();

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
                    DataLoader.playAudio(0, myAlbumSongs, storage, context);
                    break;

                case "Shuffle All":
                    DataLoader.playAudio(new Random().nextInt(myAlbumSongs.size()), myAlbumSongs, storage, context);
                    NowPlaying.shuffle = true;
                    break;

                case "Save Now Playing":
                    DataLoader.addToPlaylist(MediaPlayerService.audioList, context, AlbumSongsFragment.this);
                    break;

                case "Settings":
                    startActivity(new Intent(context, SettingsActivity.class));
                    break;

                case "Track":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("AlbumSongIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("AlbumSongIndex",0).putString("AlbumSongSortOrder", "track").apply();
                        albumSongsAdapterRunnable.run();
                    }
                    break;

                case "Title":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("AlbumSongIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("AlbumSongIndex",1).putString("AlbumSongSortOrder", MediaStore.Audio.Media.DEFAULT_SORT_ORDER).apply();
                        albumSongsAdapterRunnable.run();
                    }
                    break;


                case "Duration":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("AlbumSongIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("AlbumSongIndex",2).putString("AlbumSongSortOrder", "duration").apply();
                        albumSongsAdapterRunnable.run();
                    }
                    break;


                case "Date added":
                    menu.getItem(3).getSubMenu().getItem(sort.getInt("AlbumSongIndex", 0)).setChecked(false);
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putInt("AlbumSongIndex",3).putString("AlbumSongSortOrder", "date_added").apply();
                        albumSongsAdapterRunnable.run();
                    }
                    break;

                case "Reverse order":
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        sort.edit().putBoolean("AlbumSongReverse", true).apply();
                    }
                    else {
                        item.setChecked(false);
                        sort.edit().putBoolean("AlbumSongReverse", false).apply();
                    }
                    albumSongsAdapterRunnable.run();
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
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_album);
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_artist);
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_year);
        menu.getItem(3).getSubMenu().removeItem(R.id.sort_num_album);
        menu.getItem(3).getSubMenu().getItem(sort.getInt("AlbumSongIndex",0)).setChecked(true);
        if (sort.getBoolean("AlbumSongReverse", false)) menu.getItem(3).getSubMenu().getItem(4).setChecked(true);

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
                songs.add(myAlbumSongs.get(indices.get(i)));
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
                    DataLoader.addToPlaylist(songs, context, AlbumSongsFragment.this);
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 421) {

            if (resultCode == RESULT_OK) {
                ArrayList<String> newData = data.getStringArrayListExtra("data");
                if (newData != null) {

                    int x = -1;

                    for(int i = 0; i<MediaPlayerService.audioList.size(); i++){
                        Songs song = MediaPlayerService.audioList.get(i);
                        if (song.getId() == (myAlbumSongs.get(index).getId())){
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

                    myAlbumSongs.get(index).setTitle(newData.get(0));
                    myAlbumSongs.get(index).setAlbum(newData.get(1));
                    myAlbumSongs.get(index).setArtist(newData.get(2));
                    myAdapter.notifyItemChanged(index);

                }
            }
        }
        else super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void onPause() {
        super.onPause();

        recyclerviewParcelable = recyclerView.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onResume() {
        super.onResume();

        albumSongsAdapterRunnable.run();
    }

    @Override
    public void onDestroyView() {
        toast = null;
        myAlbumSongs = null;
        myAdapter = null;
        storage = null;
        context = null;
        sort = null;
        menu = null;
        root = null;
        recyclerView = null;
        actionMode = null;

        super.onDestroyView();
    }
}
