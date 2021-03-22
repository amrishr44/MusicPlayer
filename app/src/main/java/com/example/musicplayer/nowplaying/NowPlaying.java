package com.example.musicplayer.nowplaying;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageUtil;
import com.example.musicplayer.adapters.ItemClicked;
import com.example.musicplayer.adapters.PlaylistItemClicked;
import com.example.musicplayer.adapters.QueueAdapter;
import com.example.musicplayer.database.Albums;
import com.example.musicplayer.database.Artists;
import com.example.musicplayer.database.DataLoader;
import com.example.musicplayer.database.Songs;
import com.example.musicplayer.ui.EditTags;
import com.example.musicplayer.ui.albums.AlbumsFragment;
import com.example.musicplayer.ui.artists.ArtistsFragment;
import com.example.musicplayer.ui.equalizer.EqualizerActivity;
import com.example.musicplayer.ui.search.SearchActivity;
import com.example.musicplayer.ui.songs.SongsFragment;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Collections;

import static com.example.musicplayer.MediaPlayerService.button;

public class NowPlaying extends AppCompatActivity implements ItemClicked, PlaylistItemClicked {

    Toast toast;

    private int fromMainActivity = 0;
    private final int EDIT_TAGS_REQUEST_CODE = 420;
    public static int index = 0;
    private String totalSongNo;
    private int resumePosition;
    private boolean wasMoved = false;
    public static boolean shuffle = false;

    private RecyclerView recyclerView;
    private QueueAdapter queueAdapter;
    private  ItemTouchHelper itemTouchHelper;
    AlbumArtViewPagerAdapter view_pager_adapter;

    private AudioManager audioManager;

    private ImageView now_playing_shuffle, now_playing_previous, now_playing_pause, now_playing_next, now_playing_repeat;
    private TextView song_title, song_artist_album, song_number, now_playing_current_position, now_playing_duration, speed_0_25x, speed_0_50x, speed_0_75x, speed_1, speed_1_25x, speed_1_50x, speed_1_75x, speed_2;
    private SeekBar now_playing_seekbar, volume_seekbar;
    private ViewPager2 album_art_view_pager;

    private Intent playerIntent;
    private StorageUtil storage;
    private SharedPreferences speed;

    private final Handler song_handler = new Handler();

    private ActionMode actionMode;
    private final ActionModeCallback actionModeCallback = new ActionModeCallback();

    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat mediaController;

    private final MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback(){

        @Override
        public void onConnected() {

            MediaSessionCompat.Token token = mediaBrowser.getSessionToken();

            try {
                mediaController = new MediaControllerCompat(NowPlaying.this, token);

                new Runnable() {
                    @Override
                    public void run() {

                        mediaController.registerCallback(controllerCallback);
                        MediaControllerCompat.setMediaController(NowPlaying.this, mediaController);
                    }
                }.run();


                new Runnable() {
                    @Override
                    public void run() {
                        buttonControls();
                    }
                }.run();


            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
    };

    private final ItemTouchHelper.SimpleCallback call = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.DOWN | ItemTouchHelper.UP,ItemTouchHelper.END) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {

            final int fromPosition = viewHolder.getAdapterPosition();
            final int toPosition = target.getAdapterPosition();

            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(MediaPlayerService.audioList, i, i + 1);

                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(MediaPlayerService.audioList, i, i - 1);
                }
            }



            storage.storeAudio(MediaPlayerService.audioList);


            new Runnable() {
                @Override
                public void run() {

                    view_pager_adapter.notifyItemMoved(fromPosition, toPosition);
                    button = true;
                    wasMoved = true;
                    queueAdapter.notifyItemMoved(fromPosition, toPosition);
                    song_number.setText(MediaPlayerService.audioList.indexOf(MediaPlayerService.activeAudio)+1 + totalSongNo);
                    MediaPlayerService.audioIndex = MediaPlayerService.audioList.indexOf(MediaPlayerService.activeAudio);
                    storage.storeAudioIndexAndPostion(MediaPlayerService.audioList.indexOf(MediaPlayerService.activeAudio), MediaPlayerService.mediaPlayer.getCurrentPosition());
                }
            }.run();


            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            final int position = viewHolder.getAdapterPosition();

            if (position != MediaPlayerService.audioIndex) {
                MediaPlayerService.audioList.remove(position);
                queueAdapter.notifyItemRemoved(position);
                if (position < MediaPlayerService.audioIndex) {
                    button = true;
                    MediaPlayerService.audioIndex--;
                }
                view_pager_adapter.notifyItemRemoved(position);
                storage.storeAudio(MediaPlayerService.audioList);

                totalSongNo = "/" + MediaPlayerService.audioList.size();
                song_number.setText((MediaPlayerService.audioIndex+1) + totalSongNo);

            }
            else {
                if (toast != null) toast.cancel();
                toast = Toast.makeText(NowPlaying.this, "Can't remove the currently playing song!", Toast.LENGTH_SHORT);
                toast.show();
                queueAdapter.notifyItemChanged(position);
                button = true;
                view_pager_adapter.notifyItemChanged(position);
            }

        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        Toolbar toolbar = findViewById(R.id.cb_toolbar);

        setSupportActionBar(toolbar);

        storage = new StorageUtil(getApplicationContext());
        playerIntent = new Intent(getApplicationContext(), MediaPlayerService.class);
        speed = getSharedPreferences("SPEED", MODE_PRIVATE);


        recyclerView = findViewById(R.id.npq_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        itemTouchHelper = new ItemTouchHelper(call);
        queueAdapterRunnable.run();
        itemTouchHelper.attachToRecyclerView(recyclerView);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        album_art_view_pager = findViewById(R.id.viewpager);
        now_playing_shuffle = findViewById(R.id.now_playing_shuffle);
        now_playing_previous = findViewById(R.id.now_playing_previous);
        now_playing_pause = findViewById(R.id.now_playing_pause);
        now_playing_next = findViewById(R.id.now_playing_next);
        now_playing_repeat = findViewById(R.id.now_playing_repeat);

        song_title = findViewById(R.id.song_title);
        song_artist_album = findViewById(R.id.song_artist_album);
        song_number = findViewById(R.id.song_number);

        now_playing_seekbar = findViewById(R.id.now_playing_seekBar);
        now_playing_current_position = findViewById(R.id.now_playing_current_position);
        now_playing_duration = findViewById(R.id.now_playing_duration);


        volume_seekbar = findViewById(R.id.volume_seekBar);

        SlidingUpPanelLayout now_playing_sliding_layout = findViewById(R.id.now_playing_sliding_layout);
        now_playing_sliding_layout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {

                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED && wasMoved) {
                    album_art_view_pager.setCurrentItem(MediaPlayerService.audioIndex);
                    wasMoved = false;
                }
            }
        });


        try {
            totalSongNo = "/" + MediaPlayerService.audioList.size();
        }catch (Exception e){

            totalSongNo = "/" + 0;
        }


        resumePosition = storage.loadAudioIndexAndPosition()[1];

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            album_art_view_pager.setPageTransformer(new DepthPageTransformer());
        }

        view_pager_adapter = new AlbumArtViewPagerAdapter(NowPlaying.this);

        album_art_view_pager.setAdapter(view_pager_adapter);
        album_art_view_pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(final int position) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        if (fromMainActivity == 2) {
                            if (!button) playAudio(position, 0);
                            button = false;
                        }
                        else if (fromMainActivity == 0){
                            album_art_view_pager.setCurrentItem(MediaPlayerService.audioIndex);
                            fromMainActivity = 1;
                        }
                        else {
                            fromMainActivity = 2;
                        }

                    }
                }, 300);


            }

        });


        ImageView npq_popup_menu = findViewById(R.id.npq_popup_menu);

        npq_popup_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                PopupMenu popupMenu = new PopupMenu(NowPlaying.this, v);

                popupMenu.inflate(R.menu.npq_menu);

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {


                        switch (item.getTitle().toString()) {

                            case "Go to album":
                                if (MediaPlayerService.activeAudio.getId() != -100){
                                    AlbumsFragment.album = loadAlbum(MediaPlayerService.activeAudio.getAlbumid());
                                    Intent albumIntent = new Intent(NowPlaying.this, MainActivity.class);
                                    albumIntent.setAction("Album");
                                    startActivity(albumIntent);
                                } else {
                                    Toast.makeText(NowPlaying.this, "Unable to perform task", Toast.LENGTH_SHORT).show();
                                }

                                break;

                            case "Go to artist":
                                if (MediaPlayerService.activeAudio.getId() != -100){
                                    ArtistsFragment.artist = loadArtist(MediaPlayerService.activeAudio.getArtistid());
                                    Intent artistIntent = new Intent(NowPlaying.this, MainActivity.class);
                                    artistIntent.setAction("Artist");
                                    startActivity(artistIntent);
                                }else {
                                    Toast.makeText(NowPlaying.this, "Unable to perform task", Toast.LENGTH_SHORT).show();
                                }

                                break;

                            case "Add to playlist":
                                if (MediaPlayerService.activeAudio.getId() != -100) {
                                    ArrayList<Songs> addSong = new ArrayList<>();
                                    addSong.add(MediaPlayerService.audioList.get(MediaPlayerService.audioIndex));
                                    DataLoader.addToPlaylist(addSong, NowPlaying.this, null);
                                }
                                else {
                                    Toast.makeText(NowPlaying.this, "Unable to perform task", Toast.LENGTH_SHORT).show();
                                }
                                break;


                            case "Lyrics":
                                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                                intent.putExtra("query", MediaPlayerService.activeAudio.getTitle() + " " + MediaPlayerService.activeAudio.getArtist() + " lyrics");
                                startActivity(intent);
                                break;

                            case "Edit tags":
                                if (MediaPlayerService.activeAudio.getId() != -100) {
                                    EditTags.song = MediaPlayerService.activeAudio;
                                    startActivityForResult(new Intent(NowPlaying.this, EditTags.class), EDIT_TAGS_REQUEST_CODE);
                                }
                                else {
                                    Toast.makeText(NowPlaying.this, "Unable to perform task", Toast.LENGTH_SHORT).show();
                                }
                                break;

                            case "Use as ringtone":

                                if (MediaPlayerService.activeAudio.getId() != -100){
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        if (!Settings.System.canWrite(NowPlaying.this)) {

                                            Intent ringtoneIntent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                            startActivityForResult(ringtoneIntent, 69);

                                        } else {
                                            RingtoneManager.setActualDefaultRingtoneUri(NowPlaying.this, RingtoneManager.TYPE_RINGTONE, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaPlayerService.activeAudio.getId()));
                                            if (toast != null) toast.cancel();
                                            toast = Toast.makeText(NowPlaying.this, MediaPlayerService.activeAudio.getTitle() + " set as Ringtone!", Toast.LENGTH_LONG);
                                            toast.show();
                                        }
                                    } else {
                                        RingtoneManager.setActualDefaultRingtoneUri(NowPlaying.this, RingtoneManager.TYPE_RINGTONE, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaPlayerService.activeAudio.getId()));
                                        if (toast != null) toast.cancel();
                                        toast = Toast.makeText(NowPlaying.this, MediaPlayerService.activeAudio.getTitle() + " set as Ringtone!", Toast.LENGTH_LONG);
                                        toast.show();
                                    }
                                }else  {
                                    Toast.makeText(NowPlaying.this, "Unable to perform task", Toast.LENGTH_SHORT).show();
                                }
                                break;

                            case "Delete":
                                if (MediaPlayerService.activeAudio.getId() != -100){
                                    queueAdapter.deleteSongs(MediaPlayerService.audioIndex);
                                }else {
                                    Toast.makeText(NowPlaying.this, "Unable to perform task", Toast.LENGTH_SHORT).show();
                                }
                                break;
                        }

                        return true;

                    }
                });

                popupMenu.show();

            }
        });

    }

    private final Runnable queueAdapterRunnable = new Runnable() {
        @Override
        public void run() {

            queueAdapter = new QueueAdapter(NowPlaying.this, null, null, itemTouchHelper);
            recyclerView.setAdapter(queueAdapter);

            if (MediaPlayerService.audioList.size()==1) {
                recyclerView.setVisibility(View.GONE);
            }
            else{
                recyclerView.setVisibility(View.VISIBLE);
            }

        }
    };



    @Override
    public void onItemClicked(int index) {

        if (actionMode != null) {
            toggleSelection(index);
        }
        else{
            playAudio(index, 0);
            button = true;
            queueAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onItemLongClicked(int index) {
        if (actionMode == null) actionMode = startSupportActionMode(actionModeCallback);
        toggleSelection(index);
    }

    @Override
    public void onPlaylistItemClicked(int index, long id, Dialog dialog) {

        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", id);

        ContentValues contentValues = new ContentValues();

        contentValues.put("audio_id", MediaPlayerService.audioList.get(index).getId());

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

        Toast.makeText(NowPlaying.this, "Song has been added to the playlist!", Toast.LENGTH_SHORT).show();

        dialog.dismiss();
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

                if (mySongs.size()>1) Toast.makeText(NowPlaying.this, mySongs.size() + " songs have been added to the playlist!", Toast.LENGTH_SHORT).show();
                else Toast.makeText(NowPlaying.this, "1 song has been added to the playlist!", Toast.LENGTH_SHORT).show();

            }
        }.run();


    }

    private void buttonControls() {


        if (MediaControllerCompat.getMediaController(NowPlaying.this).getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL || shuffle)
            now_playing_shuffle.setImageResource(R.drawable.shuffle_green);
        else now_playing_shuffle.setImageResource(R.drawable.shuffle_white);

        if (shuffle)  {
            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
            shuffle = false;
        }

        if (MediaControllerCompat.getMediaController(NowPlaying.this).getRepeatMode() == PlaybackStateCompat.REPEAT_MODE_ALL)
            now_playing_repeat.setImageResource(R.drawable.repeat_green);
        else if (MediaControllerCompat.getMediaController(NowPlaying.this).getRepeatMode() == PlaybackStateCompat.REPEAT_MODE_ONE)
            now_playing_repeat.setImageResource(R.drawable.repeat_one_green);
        else now_playing_repeat.setImageResource(R.drawable.repeat_white);

        if (MediaControllerCompat.getMediaController(NowPlaying.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING)
            now_playing_pause.setImageResource(R.drawable.pause_white);
        else now_playing_pause.setImageResource(R.drawable.play_white);


        song_title.setText(MediaPlayerService.audioList.get(storage.loadAudioIndexAndPosition()[0]).getTitle());
        song_artist_album.setText(MediaPlayerService.audioList.get(storage.loadAudioIndexAndPosition()[0]).getArtist() + " - " + MediaPlayerService.audioList.get(storage.loadAudioIndexAndPosition()[0]).getAlbum());
        song_number.setText((storage.loadAudioIndexAndPosition()[0]+1) + totalSongNo);


        new Runnable() {
            @Override
            public void run() {

                now_playing_shuffle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (MediaControllerCompat.getMediaController(NowPlaying.this).getShuffleMode() != PlaybackStateCompat.SHUFFLE_MODE_ALL) {

                            now_playing_shuffle.setImageResource(R.drawable.shuffle_green);

                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);

                        }
                        else {

                            now_playing_shuffle.setImageResource(R.drawable.shuffle_white);

                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);


                        }
                    }
                });


                now_playing_repeat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (MediaControllerCompat.getMediaController(NowPlaying.this).getRepeatMode() == PlaybackStateCompat.REPEAT_MODE_NONE){

                            now_playing_repeat.setImageResource(R.drawable.repeat_green);

                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL);

                        }
                        else if (MediaControllerCompat.getMediaController(NowPlaying.this).getRepeatMode() == PlaybackStateCompat.REPEAT_MODE_ALL){

                            now_playing_repeat.setImageResource(R.drawable.repeat_one_green);

                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE);
                        }
                        else{

                            now_playing_repeat.setImageResource(R.drawable.repeat_white);

                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);

                        }

                    }
                });

                now_playing_previous.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().skipToPrevious();
                    }
                });

                now_playing_next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().skipToNext();
                    }
                });

                now_playing_pause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        if (MediaControllerCompat.getMediaController(NowPlaying.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED){

                            now_playing_pause.setImageResource(R.drawable.pause_white);
                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().play();
                        }
                        else if (MediaControllerCompat.getMediaController(NowPlaying.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){

                            now_playing_pause.setImageResource(R.drawable.play_white);
                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().pause();
                        }
                        else{

                            now_playing_pause.setImageResource(R.drawable.pause_white);
                            playAudio(storage.loadAudioIndexAndPosition()[0], storage.loadAudioIndexAndPosition()[1]);
                        }

                    }
                });



            }
        }.run();

        new Runnable() {
            @Override
            public void run() {

                if (MediaPlayerService.mediaPlayer != null && MediaPlayerService.mediaPlayer.isPlaying()) {
                    now_playing_seekbar.setMax(MediaPlayerService.mediaPlayer.getDuration()/1000);
                    now_playing_seekbar.setProgress(1);
                    now_playing_seekbar.setProgress(MediaPlayerService.mediaPlayer.getCurrentPosition()/1000);
                    now_playing_current_position.setText(getTimeInMins(MediaPlayerService.mediaPlayer.getCurrentPosition()/1000));
                    now_playing_duration.setText(getTimeInMins(MediaPlayerService.mediaPlayer.getDuration()/1000));
                }
                else if (MediaPlayerService.mediaPlayer != null && !MediaPlayerService.mediaPlayer.isPlaying()){
                    now_playing_seekbar.setMax(MediaPlayerService.audioList.get(storage.loadAudioIndexAndPosition()[0]).getDuration()/1000);
                    now_playing_seekbar.setProgress(1);
                    now_playing_seekbar.setProgress(storage.loadAudioIndexAndPosition()[1]/1000);
                    now_playing_duration.setText(getTimeInMins(MediaPlayerService.audioList.get(storage.loadAudioIndexAndPosition()[0]).getDuration()/1000));
                    now_playing_current_position.setText(getTimeInMins(storage.loadAudioIndexAndPosition()[1]/1000));
                }
                else {
                    now_playing_seekbar.setMax(MediaPlayerService.audioList.get(storage.loadAudioIndexAndPosition()[0]).getDuration()/1000);
                    now_playing_seekbar.setProgress(1);
                    now_playing_seekbar.setProgress(resumePosition/1000);
                    now_playing_duration.setText(getTimeInMins(MediaPlayerService.audioList.get(storage.loadAudioIndexAndPosition()[0]).getDuration()/1000));
                    now_playing_current_position.setText(getTimeInMins(resumePosition/1000));
                }

            }
        }.run();


        now_playing_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (MediaPlayerService.mediaPlayer != null && (fromUser || progress==0)){
                    MediaPlayerService.mediaPlayer.seekTo(progress*1000);
                    now_playing_current_position.setText(getTimeInMins(progress));
                    storage.storeAudioIndexAndPostion(storage.loadAudioIndexAndPosition()[0],progress*1000);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        if (MediaPlayerService.mediaPlayer != null && MediaPlayerService.mediaPlayer.isPlaying()) NowPlaying.this.runOnUiThread(seekbarRunnable);

        volume_seekbar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volume_seekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

        volume_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new Runnable() {
                @Override
                public void run() {

                    speed_0_25x = findViewById(R.id.speed_0_25);
                    speed_0_50x = findViewById(R.id.speed_0_50);
                    speed_0_75x = findViewById(R.id.speed_0_75);
                    speed_1 = findViewById(R.id.speed_1_0);
                    speed_1_25x = findViewById(R.id.speed_1_25);
                    speed_1_50x = findViewById(R.id.speed_1_50);
                    speed_1_75x = findViewById(R.id.speed_1_75);
                    speed_2 = findViewById(R.id.speed_2_0);

                    float x = speed.getFloat("SPEED", 1.0f);

                    if (x == 0.25) {
                        speed_0_25x.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 0.50) {
                        speed_0_50x.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 0.75) {
                        speed_0_75x.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 1.0) {
                        speed_1.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 1.25) {
                        speed_1_25x.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 1.50) {
                        speed_1_50x.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 1.75) {
                        speed_1_75x.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 2.0) {
                        speed_2.setTextColor(getColor(R.color.programmer_green));
                    }


                    speed_0_25x.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            speed.edit().putFloat("SPEED", (float) 0.25).putInt("STATE", MediaControllerCompat.getMediaController(NowPlaying.this).getPlaybackState().getState()).apply();
                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().sendCustomAction("SPEED", null);

                        }
                    });

                    speed_0_50x.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            speed.edit().putFloat("SPEED", (float) 0.50).putInt("STATE", MediaControllerCompat.getMediaController(NowPlaying.this).getPlaybackState().getState()).apply();
                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().sendCustomAction("SPEED", null);


                        }
                    });

                    speed_0_75x.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            speed.edit().putFloat("SPEED", (float) 0.75).putInt("STATE", MediaControllerCompat.getMediaController(NowPlaying.this).getPlaybackState().getState()).apply();
                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().sendCustomAction("SPEED", null);

                        }
                    });

                    speed_1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            speed.edit().putFloat("SPEED", (float) 1.0).putInt("STATE", MediaControllerCompat.getMediaController(NowPlaying.this).getPlaybackState().getState()).apply();
                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().sendCustomAction("SPEED", null);

                        }
                    });

                    speed_1_25x.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            speed.edit().putFloat("SPEED", (float) 1.25).putInt("STATE", MediaControllerCompat.getMediaController(NowPlaying.this).getPlaybackState().getState()).apply();
                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().sendCustomAction("SPEED", null);

                        }
                    });

                    speed_1_50x.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            speed.edit().putFloat("SPEED", (float) 1.50).putInt("STATE", MediaControllerCompat.getMediaController(NowPlaying.this).getPlaybackState().getState()).apply();
                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().sendCustomAction("SPEED", null);

                        }
                    });

                    speed_1_75x.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            speed.edit().putFloat("SPEED", (float) 1.75).putInt("STATE", MediaControllerCompat.getMediaController(NowPlaying.this).getPlaybackState().getState()).apply();
                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().sendCustomAction("SPEED", null);

                        }
                    });

                    speed_2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            speed.edit().putFloat("SPEED", (float) 2.0).putInt("STATE", MediaControllerCompat.getMediaController(NowPlaying.this).getPlaybackState().getState()).apply();
                            MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().sendCustomAction("SPEED", null);

                        }
                    });


                }
            }.run();
        }

    }


    private final MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {

            if (state.getState() == PlaybackStateCompat.STATE_PLAYING){

                now_playing_pause.setImageResource(R.drawable.pause_white);
                NowPlaying.this.runOnUiThread(seekbarRunnable);
            }
            else{

                now_playing_pause.setImageResource(R.drawable.play_white);
                song_handler.removeCallbacks(seekbarRunnable);

                if (state.getState() != PlaybackStateCompat.STATE_PAUSED){
                    now_playing_current_position.setText(getTimeInMins(0));
                    now_playing_seekbar.setProgress(1);
                }
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                double x = state.getPlaybackSpeed();

                if (x != 0) {

                    speed_0_25x.setTextColor(getColor(R.color.notification_artist));
                    speed_0_50x.setTextColor(getColor(R.color.notification_artist));
                    speed_0_75x.setTextColor(getColor(R.color.notification_artist));
                    speed_1.setTextColor(getColor(R.color.notification_artist));
                    speed_1_25x.setTextColor(getColor(R.color.notification_artist));
                    speed_1_50x.setTextColor(getColor(R.color.notification_artist));
                    speed_1_75x.setTextColor(getColor(R.color.notification_artist));
                    speed_2.setTextColor(getColor(R.color.notification_artist));

                    if (x == 0.25) {
                        speed_0_25x.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 0.50) {
                        speed_0_50x.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 0.75) {
                        speed_0_75x.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 1.0) {
                        speed_1.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 1.25) {
                        speed_1_25x.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 1.50) {
                        speed_1_50x.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 1.75) {
                        speed_1_75x.setTextColor(getColor(R.color.programmer_green));
                    } else if (x == 2.0) {
                        speed_2.setTextColor(getColor(R.color.programmer_green));
                    }
                }
            }
        }



        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {

            song_title.setText(storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getTitle());
            song_artist_album.setText(storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getArtist() + " - " + storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getAlbum());
            song_number.setText((storage.loadAudioIndexAndPosition()[0]+1) + totalSongNo);

            queueAdapter.notifyDataSetChanged();

            now_playing_seekbar.setMax(MediaPlayerService.mediaPlayer.getDuration()/1000);

            song_handler.removeCallbacks(seekbarRunnable);

            NowPlaying.this.runOnUiThread(seekbarRunnable);

            now_playing_current_position.setText(getTimeInMins(MediaPlayerService.mediaPlayer.getCurrentPosition()/1000));
            now_playing_duration.setText(getTimeInMins(MediaPlayerService.mediaPlayer.getDuration()/1000));


            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    album_art_view_pager.setCurrentItem((storage.loadAudioIndexAndPosition()[0]));
                }
            }, 50);

        }

    };



    private void playAudio(final int audioIndex, final int position) {
        //Check is service is active
        new Runnable() {
            @Override
            public void run() {

                if (!MainActivity.serviceBound) {
                    //Store Serializable audioList to SharedPreferences
                    StorageUtil storage = new StorageUtil(getApplicationContext());
                    storage.storeAudioIndexAndPostion(audioIndex, position);


                    startService(playerIntent);
                    MainActivity.serviceBound = true;
                }


                else {
                    //Store the new audioIndex to SharedPreferences
                    StorageUtil storage = new StorageUtil(getApplicationContext());
                    storage.storeAudioIndexAndPostion(audioIndex, position);

                    Intent broadcastIntent = new Intent(SongsFragment.Broadcast_PLAY_NEW_AUDIO);
                    sendBroadcast(broadcastIntent);
                }

            }
        }.run();

    }




    @Override
    protected void onStart() {
        super.onStart();

        mediaBrowser = new MediaBrowserCompat(getApplicationContext(), new ComponentName(this, MediaPlayerService.class), connectionCallback, null);

        new Runnable() {
            @Override
            public void run() {

                mediaBrowser.connect();
                song_handler.removeCallbacks(seekbarRunnable);
            }
        }.run();

    }

    @Override
    protected void onStop() {
        super.onStop();

        mediaController.unregisterCallback(controllerCallback);
        mediaBrowser.disconnect();
    }


    @Override
    protected void onResume() {
        super.onResume();

        queueAdapter.notifyDataSetChanged();
        view_pager_adapter.notifyDataSetChanged();
    }

    public static String getTimeInMins (int time){

        int secs;

        if (time > 59){
            secs = time % 60;
            if (secs < 10) return (time / 60 + ":0" + secs);
            else return (time / 60 + ":" + secs);
        }
        else{
            if (time < 10) return ("0:0" + time);
            else return ("0:" + time);
        }

    }

    private final Runnable seekbarRunnable = new Runnable() {
        @Override
        public void run() {

            if (MediaPlayerService.mediaPlayer != null && MediaPlayerService.mediaPlayer.isPlaying()){
                try {
                    now_playing_seekbar.setProgress(MediaPlayerService.mediaPlayer.getCurrentPosition()/1000);
                    now_playing_current_position.setText(getTimeInMins(MediaPlayerService.mediaPlayer.getCurrentPosition()/1000));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            else if (MediaPlayerService.mediaPlayer == null){
                now_playing_seekbar.setProgress(1);
                now_playing_seekbar.setProgress(resumePosition/1000);
                now_playing_current_position.setText(getTimeInMins(resumePosition/1000));
            }

            song_handler.postDelayed(this, 1000);

        }
    };


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
                songs.add(MediaPlayerService.audioList.get(indices.get(i)));
            }

            switch (item.getTitle().toString()) {

                case "Play":
                    DataLoader.playAudio(0, songs, storage, NowPlaying.this);
                    button = true;
                    mode.finish();
                    return true;

                case "Enqueue":
                    if (MediaPlayerService.audioList != null) MediaPlayerService.audioList.addAll(songs);
                    else MediaPlayerService.audioList = songs;
                    storage.storeAudio(MediaPlayerService.audioList);
                    queueAdapter.notifyDataSetChanged();
                    mode.finish();
                    return true;

                case "Play next":
                    if (MediaPlayerService.audioList != null) MediaPlayerService.audioList.addAll(MediaPlayerService.audioIndex+1, songs);
                    else MediaPlayerService.audioList = songs;
                    storage.storeAudio(MediaPlayerService.audioList);
                    queueAdapter.notifyDataSetChanged();
                    mode.finish();
                    return true;

                case "Shuffle":
                    DataLoader.playAudio(0, songs, storage, NowPlaying.this);
                    button = true;
                    MediaControllerCompat.getMediaController(NowPlaying.this).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                    mode.finish();
                    return true;

                case "Add to playlist":
                    DataLoader.addToPlaylist(songs, NowPlaying.this, null);
                    mode.finish();
                    return true;

                case "Delete":
                    SongsFragment.deleteSongs(songs, NowPlaying.this);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.now_playing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getTitle().toString()){

            case "Search":
                button = true;
                startActivity(new Intent(this, SearchActivity.class));
                return true;

            case "Equalizer":
                button = true;
                startActivity(new Intent(this, EqualizerActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }


    private Albums loadAlbum(long album_id){

        ContentResolver contentResolver = getApplication().getContentResolver();
        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String selection = "_id =? ";
        Cursor cursor = contentResolver.query(uri, new String[]{"_id","album","album_art", "numsongs", "artist"}, selection, new String[]{"" + album_id}, null);

        if (cursor != null && cursor.getCount()>0) {

            cursor.moveToFirst();

            long id = cursor.getLong(0);
            String album = cursor.getString(1).trim();
            String albumArt = cursor.getString(2);
            int numSongs = cursor.getInt(3);
            String artist = cursor.getString(4).trim();

            cursor.close();

            return new Albums(id, album, albumArt, numSongs, artist);

        }

        return null;

    }

    private Artists loadArtist(long artist_id){

        ContentResolver contentResolver = getApplication().getContentResolver();
        Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        String selection = "_id =? ";
        Cursor cursor = contentResolver.query(uri, new String[]{"_id", "artist"}, selection, new String[]{"" + artist_id}, null);

        if (cursor != null && cursor.getCount()>0) {

            cursor.moveToFirst();

            long id = cursor.getLong(0);
            String artist = cursor.getString(1).trim();


            cursor.close();

            return new Artists(id, artist);

        }

        return null;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == 69){
            if (resultCode == RESULT_OK){

                try {
                    RingtoneManager.setActualDefaultRingtoneUri(NowPlaying.this, RingtoneManager.TYPE_RINGTONE, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaPlayerService.activeAudio.getId()));
                    if (toast != null) toast = null;
                    toast = Toast.makeText(NowPlaying.this, MediaPlayerService.activeAudio.getTitle() + " set as Ringtone!", Toast.LENGTH_LONG);
                    toast.show();
                } catch (Exception e) {
                    toast = Toast.makeText(NowPlaying.this, "Permission was not Granted!", Toast.LENGTH_SHORT);
                    toast.show();
                }

            }
            else {

                toast = Toast.makeText(NowPlaying.this, "Permission was not Granted!", Toast.LENGTH_SHORT);
                toast.show();
            }
        }



        else if (requestCode == EDIT_TAGS_REQUEST_CODE) {

            if (resultCode == RESULT_OK) {
                ArrayList<String> newData = data.getStringArrayListExtra("data");
                if (newData != null) {
                    song_title.setText(newData.get(0));
                    song_artist_album.setText(newData.get(2) + " - " + newData.get(1));
                    MediaPlayerService.audioList.get(MediaPlayerService.audioIndex).setTitle(newData.get(0));
                    MediaPlayerService.audioList.get(MediaPlayerService.audioIndex).setAlbum(newData.get(1));
                    MediaPlayerService.audioList.get(MediaPlayerService.audioIndex).setArtist(newData.get(2));
                    storage.storeAudio(MediaPlayerService.audioList);
                    queueAdapter.notifyItemChanged(MediaPlayerService.audioIndex);
                }
            }
        }

        else if (requestCode == 423) {

            if (resultCode == RESULT_OK) {
                ArrayList<String> newData = data.getStringArrayListExtra("data");
                if (newData != null) {
                    if (MediaPlayerService.audioIndex == index) {
                        song_title.setText(newData.get(0));
                        song_artist_album.setText(newData.get(2) + " - " + newData.get(1));
                    }
                    MediaPlayerService.audioList.get(index).setTitle(newData.get(0));
                    MediaPlayerService.audioList.get(index).setAlbum(newData.get(1));
                    MediaPlayerService.audioList.get(index).setArtist(newData.get(2));
                    storage.storeAudio(MediaPlayerService.audioList);
                    queueAdapter.notifyItemChanged(index);
                }
            }
        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        if(isTaskRoot()){
            startActivity(new Intent(this, MainActivity.class));
        }
        return super.onSupportNavigateUp();
    }


    @Override
    protected void onDestroy() {

        toast = null;
        totalSongNo = null;
        recyclerView = null;
        queueAdapter = null;
        itemTouchHelper = null;
        view_pager_adapter = null;
        audioManager = null;
        now_playing_shuffle = now_playing_previous = now_playing_pause = now_playing_next = now_playing_repeat = null;
        song_title = song_artist_album = song_number = now_playing_current_position = now_playing_duration = speed_0_25x = speed_0_50x = speed_0_75x = speed_1 = speed_1_25x = speed_1_50x = speed_1_75x = speed_2 = null;
        now_playing_seekbar = volume_seekbar = null;
        album_art_view_pager = null;
        playerIntent = null;
        storage = null;
        speed = null;
        actionMode = null;
        mediaBrowser = null;
        mediaController = null;

        Glide.get(this).clearMemory();
        Glide.get(this).trimMemory(TRIM_MEMORY_COMPLETE);
        song_handler.removeCallbacks(seekbarRunnable);

        super.onDestroy();
    }
}


