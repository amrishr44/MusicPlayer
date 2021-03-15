package com.example.musicplayer.ui.playlists;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageUtil;
import com.example.musicplayer.adapters.ItemClicked;
import com.example.musicplayer.adapters.PlaylistItemClicked;
import com.example.musicplayer.adapters.QueueAdapter;
import com.example.musicplayer.adapters.SongChanged;
import com.example.musicplayer.database.DataLoader;
import com.example.musicplayer.database.Songs;
import com.example.musicplayer.ui.SettingsActivity;
import com.example.musicplayer.ui.equalizer.EqualizerActivity;
import com.example.musicplayer.ui.folders.FoldersFragment;
import com.example.musicplayer.ui.search.SearchActivity;
import com.example.musicplayer.ui.songs.SongsFragment;
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static android.app.Activity.RESULT_OK;
import static com.example.musicplayer.MainActivity.secondary_index;


public class PlaylistSongsFragment extends Fragment  implements ItemClicked, PlaylistItemClicked, SongChanged {

   Toast toast;

    private ArrayList<Songs> playlistSongs;

    private View root;
    private RecyclerView playlistRecyclerView;
    private QueueAdapter queueAdapter;
    private RecyclerViewFastScroller fastScroller;
    private ItemTouchHelper itemTouchHelper;

    private StorageUtil storage;
    private Context context;

    private TextView playlist_num_songs;

    private ActionMode actionMode;
    private final ActionModeCallback actionModeCallback = new ActionModeCallback();


    private final ItemTouchHelper.Callback itemCallback = new ItemTouchHelper.Callback() {

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.DOWN | ItemTouchHelper.UP,ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                float width = (float) viewHolder.itemView.getWidth();
                float alpha = 1.0f - Math.abs(dX) / width;
                viewHolder.itemView.setAlpha(alpha);
                viewHolder.itemView.setTranslationX(dX);
            }
            else {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY,
                        actionState, isCurrentlyActive);
            }

        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);

            if (viewHolder != null && actionState != ItemTouchHelper.ACTION_STATE_SWIPE)
                viewHolder.itemView.setBackgroundColor(getResources().getColor(R.color.dark_transparent_black));
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

            viewHolder.itemView.setBackgroundColor(getResources().getColor(R.color.colorDark));

        }



        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {

            final int fromPosition = viewHolder.getAdapterPosition();
            final int toPosition = target.getAdapterPosition();

            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(playlistSongs, i, i + 1);

                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(playlistSongs, i, i - 1);
                }
            }



            new Runnable() {
                @Override
                public void run() {

                    MediaStore.Audio.Playlists.Members.moveItem(context.getContentResolver(), PlaylistsFragment.playlistId, fromPosition, toPosition);
                    queueAdapter.notifyItemMoved(fromPosition, toPosition);

                }
            }.run();


            return true;
        }



        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            int position = viewHolder.getAdapterPosition();

            deletePlaylistTracks(PlaylistsFragment.playlistId, playlistSongs.get(position).getId());

            playlistSongs.remove(position);
            queueAdapter.notifyItemRemoved(position);
            playlist_num_songs.setText(queueAdapter.getItemCount() + "");
        }
    };



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        context = getContext();

        root = inflater.inflate(R.layout.fragment_playlist_songs, container, false);

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar toolbar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        toolbar.setHomeAsUpIndicator(R.drawable.back_green);
        toolbar.setTitle(PlaylistsFragment.playlistName);

        itemTouchHelper = new ItemTouchHelper(itemCallback);
        storage = new StorageUtil(context);

        playlistRecyclerView = root.findViewById(R.id.playlist_songs_recycler_view);
        playlistRecyclerView.setHasFixedSize(true);
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        playlistSongsRunnable.run();

        itemTouchHelper.attachToRecyclerView(playlistRecyclerView);

        TextView playlist_name = root.findViewById(R.id.playlist_name);
        playlist_num_songs = root.findViewById(R.id.playlist_song_num_songs);

        String string = playlistSongs.size() + "";
        playlist_num_songs.setText(string);
        playlist_name.setText(PlaylistsFragment.playlistName);

        ImageView app_bar_image = root.findViewById(R.id.app_bar_image_playlist);
        ImageView small_playlist_art = root.findViewById(R.id.small_playlist_art);


        Glide.with(this).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), PlaylistsFragment.playlistId))
                .centerCrop().error(R.drawable.cassettes).into(app_bar_image);

        Glide.with(this).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), PlaylistsFragment.playlistId))
                .centerCrop().error(R.drawable.cassettes).into(small_playlist_art);

        Button play_all = root.findViewById(R.id.playlist_play_all);
        Button shuffle_all = root.findViewById(R.id.playlist_shuffle_all);
        Button queue_all = root.findViewById(R.id.playlist_queue_all);

        play_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DataLoader.playAudio(0,playlistSongs, storage, context);
            }
        });

        shuffle_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DataLoader.playAudio(new Random().nextInt(playlistSongs.size()), playlistSongs, storage, context);
                MediaControllerCompat.getMediaController(((AppCompatActivity)getActivity())).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
            }
        });

        queue_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (toast != null) toast.cancel();


                MediaPlayerService.audioList.addAll(playlistSongs);
                storage.storeAudio(MediaPlayerService.audioList);

                toast = Toast.makeText(context, "QUEUED", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        fastScroller = root.findViewById(R.id.fast_scroller_album);

        playlistRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
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


    private final Runnable playlistSongsRunnable = new Runnable() {
        @Override
        public void run() {

            playlistSongs = DataLoader.loadAudio(PlaylistsFragment.playlistId, 3, context, null);

            queueAdapter = new QueueAdapter(context, PlaylistSongsFragment.this, playlistSongs, itemTouchHelper);
            playlistRecyclerView.setAdapter(queueAdapter);
            playlistRecyclerView.scrollToPosition(MainActivity.secondary_index);
        }
    };



    @Override
    public void onDestroy() {
        super.onDestroy();

        itemTouchHelper = null;
    }

    @Override
    public void onItemClicked(int index) {

        if (actionMode != null) {
            toggleSelection(index);
        }
        else{
            MainActivity.secondary_index = index;
            DataLoader.playAudio(index, playlistSongs, storage, context);
            queueAdapter.notifyDataSetChanged();
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

        contentValues.put("audio_id", playlistSongs.get(index).getId());

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

    private void deletePlaylistTracks(long playlistId, long audioId) {

        ContentResolver resolver = context.getContentResolver();
        try {
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
                    "external", playlistId);
            String selection = MediaStore.Audio.Playlists.Members.AUDIO_ID + " =? " ;
            String audioId1 = audioId + "";
            String[] selectionArg = { audioId1 };
            resolver.delete(uri, selection, selectionArg);
        } catch (Exception e) {
            e.printStackTrace();
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
                    DataLoader.playAudio(0, playlistSongs, storage, context);
                    break;

                case "Shuffle All":

                    DataLoader.playAudio(new Random().nextInt(playlistSongs.size()), playlistSongs, storage, context);
                    MediaControllerCompat.getMediaController((Activity) context).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                    break;

                case "Save Now Playing":
                    DataLoader.addToPlaylist(MediaPlayerService.audioList, context, PlaylistSongsFragment.this);
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
    public void onPrepareOptionsMenu(@NonNull Menu menu) {

        menu.removeItem(R.id.action_sort);

    }

    @Override
    public void onSongChanged() {
        queueAdapter.notifyDataSetChanged();
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

            ArrayList<Integer> indices = (ArrayList<Integer>) queueAdapter.getSelectedItems();
            ArrayList<Songs> songs = new ArrayList<>();

            for (int i=0; i<indices.size(); i++){
                songs.add(playlistSongs.get(indices.get(i)));
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
                    DataLoader.addToPlaylist(songs, context, PlaylistSongsFragment.this);
                    mode.finish();
                    return true;

                case R.id.selected_delete:
                    SongsFragment.deleteSongs(songs, context);
                    queueAdapter.notifyDataSetChanged();
                    mode.finish();
                    return true;


                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            queueAdapter.clearSelection();
            actionMode = null;
        }
    }


    private void toggleSelection(int position) {
        queueAdapter.toggleSelection(position);
        int count = queueAdapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 423) {

            if (resultCode == RESULT_OK) {
                ArrayList<String> newData = data.getStringArrayListExtra("data");
                if (newData != null) {

                    int x = -1;

                    for(int i = 0; i<MediaPlayerService.audioList.size(); i++){
                        Songs song = MediaPlayerService.audioList.get(i);
                        if (song.getId() == (playlistSongs.get(secondary_index).getId())){
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

                    playlistSongs.get(secondary_index).setTitle(newData.get(0));
                    playlistSongs.get(secondary_index).setAlbum(newData.get(1));
                    playlistSongs.get(secondary_index).setArtist(newData.get(2));
                    queueAdapter.notifyItemChanged(secondary_index);

                }
            }
        }
        else super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void onDestroyView() {
        toast = null;
        playlistSongs = null;
        root = null;
        playlistRecyclerView = null;
        queueAdapter = null;
        fastScroller = null;
        itemTouchHelper = null;
        storage = null;
        context = null;
        playlist_num_songs = null;
        actionMode = null;

        super.onDestroyView();
    }
}
