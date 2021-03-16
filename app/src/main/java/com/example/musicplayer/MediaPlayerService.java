package com.example.musicplayer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.example.musicplayer.database.Songs;
import com.example.musicplayer.nowplaying.NowPlaying;
import com.example.musicplayer.ui.equalizer.EqualizerActivity;
import com.example.musicplayer.ui.songs.SongsFragment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MediaPlayerService extends MediaBrowserServiceCompat implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener {


    private  int resumePosition;    //Used to store the current position of the song
    public static boolean button = false;   //Used to show if the previous/next button were pressed
    private final int NOTIFICATION_ID = 69;  //Notification ID
    private final String CHANNEL_ID = "CHANNEL_1";   //Channel ID for the notification
    private boolean isShuffleOn;
    private int repeatModeValue = 0;    // 0 - Repeat off, 1 - Repeat the song playing now, 2- Repeat all songs
    private boolean isForeground = false;
    private Toast toast;

    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;


    //List of available Audio files
    public static ArrayList<Songs> audioList;
    public static int audioIndex = -1;
    public static com.example.musicplayer.database.Songs activeAudio; //an object of the currently playing audio

    public static MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    MediaSessionCompat mediaSession;
    PlaybackStateCompat.Builder playbackState;
    MediaMetadataCompat.Builder metadataBuilder;

    NotificationCompat.Builder notificationBuilder;
    NotificationManager manager;

    //Notification views for compressed and expanded notifications
    RemoteViews notificationContentView;
    RemoteViews notificationBigContentView;

    private StorageUtil storage;
    private SharedPreferences speed, preferences;

    @Override
    public void onCreate() {
        super.onCreate();

        storage = new StorageUtil(getApplicationContext());
        //Listen for new Audio to play -- BroadcastReceiver
        register_playNewAudio();


        new Runnable() {
            @Override
            public void run() {

                mediaSession = new MediaSessionCompat(MediaPlayerService.this, MediaPlayerService.class.getSimpleName());
                mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
                createNotificationChannel();
                playbackState = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP).setState(PlaybackStateCompat.STATE_CONNECTING, PlaybackStateCompat.ACTION_PREPARE, (float) 1.0);
                mediaSession.setPlaybackState(playbackState.build());
                mediaSession.setCallback(new MyMediaSessionCallback());
                setSessionToken(mediaSession.getSessionToken());
            }
        }.run();


        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(getApplicationContext(), MediaButtonReceiver.class);
        PendingIntent mbrIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);
        mediaSession.setMediaButtonReceiver(mbrIntent);

        speed = getSharedPreferences("SPEED", MODE_PRIVATE);
        preferences = getSharedPreferences("PREFERENCES", MODE_PRIVATE);

    }



    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {

      if (TextUtils.equals(clientPackageName,getPackageName())) return new BrowserRoot(getString(R.string.app_name), null);
      return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

    }


    @Override
    public void onCompletion(MediaPlayer mp) {

        if (repeatModeValue == 2 && !isShuffleOn && audioIndex == audioList.size() - 1){
            //Repeat all songs on, Shuffle off and end of playlist
            audioIndex = 0;
        }
        else if (repeatModeValue == 0) {
            //Repeat off
            if (!isShuffleOn) {
                audioIndex = audioList.indexOf(activeAudio) + 1;  //Shuffle off, selects next song
            } else {
                audioIndex = new Random().nextInt(audioList.size()); //Shuffle on, selects random song from playlist
            }
        }


        resumePosition = 0;

        if (audioIndex != -1 && audioIndex < audioList.size()) {
            //index is in a valid range
            activeAudio = audioList.get(audioIndex);
            stopMedia();
            initMediaPlayer();
        } else {
            //index in invalid range, therefore stops playing

            audioIndex = audioList.indexOf(activeAudio)-1;
            stopMedia();
            removeAudioFocus();

            mediaSession.setPlaybackState(playbackState.setState(PlaybackStateCompat.STATE_STOPPED, 0, (float)1.0).build());

            notificationContentView.setImageViewResource(R.id.content_view_pause, R.drawable.play_white);
            notificationBigContentView.setImageViewResource(R.id.big_cv_pause, R.drawable.play_white);

            notificationBuilder.setCustomContentView(notificationContentView)
                                .setCustomBigContentView(notificationBigContentView);

            manager.notify(NOTIFICATION_ID,notificationBuilder.build());

            stopForeground(false);

            removeAudioFocus();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED || mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_STOPPED) mediaSession.getController().getTransportControls().stop();
                }
            },600000);
        }


    }


    @Override
    public void onPrepared(MediaPlayer mp) {

        playMedia();
    }


    @Override
    public void onAudioFocusChange(int focusChange) {

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mediaPlayer.start();
                        }
                    }).start();
                }
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) pauseMedia();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }


    //The system calls this method when an activity, requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        MediaButtonReceiver.handleIntent(mediaSession, intent);

        try {
            if (intent.getAction().equals("SHUFFLE")){
                mediaSession.getController().getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
            }
        }catch (Exception ignored){

        }


        if (!mediaSession.isActive()) {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }

            try {

                //Load data from SharedPreferences
                audioList = storage.loadAudio();
                audioIndex = storage.loadAudioIndexAndPosition()[0];
                resumePosition = storage.loadAudioIndexAndPosition()[1];

                if (audioIndex != -1 && audioIndex < audioList.size()) {
                    //index is in a valid range
                    activeAudio = audioList.get(audioIndex);
                } else {
                    stopSelf();
                }
            } catch (NullPointerException e) {
                stopSelf();
            }


            metadataBuilder = new MediaMetadataCompat.Builder();
            register_playNewAudio();
            registerBecomingNoisyReceiver();

            // Initialize Media Player
            initMediaPlayer();

            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            notificationContentView = new RemoteViews(getPackageName(), R.layout.content_view);
            notificationBigContentView = new RemoteViews(getPackageName(), R.layout.big_content_view);

            setNotification();

            notificationBuilder = new NotificationCompat.Builder(this);
            notificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.getSessionToken()))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, NowPlaying.class), 0))    //Intent for opening the app on clicking the notification
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.cassette)
                    .setChannelId(CHANNEL_ID);

        }


        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener();

        //Listen for new Audio to play -- BroadcastReceiver
        register_playNewAudio();

        return START_NOT_STICKY;


    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                unregisterReceiver(becomingNoisyReceiver);
                unregisterReceiver(playNewAudio);
            }
            stopMedia();
        }

        if (EqualizerActivity.virtualizer != null) EqualizerActivity.virtualizer.release();
        if (EqualizerActivity.bassBoost != null) EqualizerActivity.bassBoost.release();
        if (EqualizerActivity.equalizer != null) EqualizerActivity.equalizer.release();

        if (mediaSession.isActive()) mediaSession.setActive(false);
        mediaSession.release();

        MainActivity.serviceBound = false;
    }






    private boolean requestAudioFocus() {
        return audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void removeAudioFocus() {
        audioManager.abandonAudioFocus(this);
    }



    private void initMediaPlayer() {

        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source


        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(activeAudio.getData());
            mediaPlayer.prepareAsync();
        } catch (IOException e) {

            audioList.remove(audioIndex);
            storage.storeAudio(audioList);
            mediaSession.setPlaybackState(playbackState.setState(PlaybackStateCompat.STATE_ERROR, 0, 1.0f).build());
            mediaSession.getController().getTransportControls().skipToNext();
        }

    }

    private void playMedia() {

        //Request audio focus
        if (!requestAudioFocus()) {
            //Could not gain focus
            stopSelf();
        }

        if (!mediaPlayer.isPlaying()) {
            // Media Player not playing
            final Uri myuri = Uri.parse("content://media/external/audio/albumart/" + activeAudio.getAlbumid());

            if (resumePosition > 0){
                mediaPlayer.seekTo(resumePosition); //Sets the current position of the song
            }


            // Gets information from the currently playing song and posts the notification

            try {
                InputStream is = getContentResolver().openInputStream(myuri);
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (MediaPlayerService.activeAudio.getAlbumid() != -100) {

                            notificationBigContentView.setImageViewUri(R.id.big_cv_small_album_art, myuri);
                            notificationBigContentView.setImageViewUri(R.id.big_cv_bg, myuri);
                            notificationContentView.setImageViewUri(R.id.content_view_album_art, myuri);
                        }
                        else {

                            MediaMetadataRetriever m = new MediaMetadataRetriever();
                            m.setDataSource(MediaPlayerService.activeAudio.getData());
                            byte[] art = m.getEmbeddedPicture();
                            Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
                            notificationBigContentView.setImageViewBitmap(R.id.big_cv_small_album_art, songImage);
                            notificationBigContentView.setImageViewBitmap(R.id.big_cv_bg, songImage);
                            notificationContentView.setImageViewBitmap(R.id.content_view_album_art, songImage);
                        }
                    }
                });
                is.close();
            }catch (Exception ignored){
                if (MediaPlayerService.activeAudio.getAlbumid() != -100) {

                    notificationBigContentView.setImageViewResource(R.id.big_cv_bg, R.mipmap.cassette_image_foreground);
                    notificationBigContentView.setImageViewResource(R.id.big_cv_small_album_art, R.mipmap.cassette_image_foreground);
                    notificationContentView.setImageViewResource(R.id.content_view_album_art, R.mipmap.cassette_image_foreground);
                }
                else {

                    MediaMetadataRetriever m = new MediaMetadataRetriever();
                    m.setDataSource(MediaPlayerService.activeAudio.getData());
                    byte[] art = m.getEmbeddedPicture();
                    Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
                    notificationBigContentView.setImageViewBitmap(R.id.big_cv_small_album_art, songImage);
                    notificationBigContentView.setImageViewBitmap(R.id.big_cv_bg, songImage);
                    notificationContentView.setImageViewBitmap(R.id.content_view_album_art, songImage);
                }
            }

            final String title = activeAudio.getTitle();
            final String artist= activeAudio.getArtist();
            final String album = activeAudio.getAlbum();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album);

                    storage.storeAudioIndexAndPostion(audioIndex, 0);
                }
            }).start();

            notificationContentView.setTextViewText(R.id.content_view_title, title);
            notificationContentView.setTextViewText(R.id.content_view_artist_album, artist + " - " + album);

            notificationBigContentView.setTextViewText(R.id.big_cv_title, title);
            notificationBigContentView.setTextViewText(R.id.big_cv_artist_album, artist + " - " + album);


            notificationBuilder.setCustomContentView(notificationContentView)
                    .setCustomBigContentView(notificationBigContentView);


            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isForeground) {
                        startForeground(NOTIFICATION_ID,notificationBuilder.build());
                        isForeground = true;
                    }
                    else manager.notify(NOTIFICATION_ID, notificationBuilder.build());
                }
            }, 0);

            if (preferences.getBoolean("LOCKSCREEN", true)) {
                // Lockscreen wallpaper is changed to the song album art while playing
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.drawable.cassette_green));

            }
            else metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null);


            mediaSession.setActive(true);
            mediaSession.setPlaybackState(playbackState.setState(PlaybackStateCompat.STATE_PLAYING, 0, (float) 1.0).build());
            mediaSession.setMetadata(metadataBuilder.build());


            new Thread(new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.start();
                }
            }).start();

            registerBecomingNoisyReceiver();
            requestAudioFocus();

        }

    }


    //Stops the song
    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

    }

    // Pauses the song
    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {

            notificationContentView.setImageViewResource(R.id.content_view_pause, R.drawable.play_white);
            notificationBigContentView.setImageViewResource(R.id.big_cv_pause, R.drawable.play_white);

            notificationBuilder.setCustomContentView(notificationContentView)
                    .setCustomBigContentView(notificationBigContentView);
            manager.notify(NOTIFICATION_ID,notificationBuilder.build());

            mediaPlayer.pause();

            // Stores the current position of the currently playing song
            resumePosition = mediaPlayer.getCurrentPosition();
            storage.storeAudioIndexAndPostion(audioIndex, resumePosition);

            unregisterReceiver(becomingNoisyReceiver);
            unregisterReceiver(playNewAudio);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaSession.setPlaybackState(playbackState.setState(PlaybackStateCompat.STATE_PAUSED, resumePosition , mediaPlayer.getPlaybackParams().getSpeed()).build());
            }
            else mediaSession.setPlaybackState(playbackState.setState(PlaybackStateCompat.STATE_PAUSED, resumePosition , (float) 1.0).build());

           removeAudioFocus();

            stopForeground(false);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED || mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_STOPPED) mediaSession.getController().getTransportControls().stop();
                }
            },600000);

        }
    }

    //Resumes the song
    private void resumeMedia() {

        //Request audio focus
        if (!requestAudioFocus()) {
            //Could not gain focus
            stopSelf();
        }

        if (!mediaPlayer.isPlaying()) {

            notificationContentView.setImageViewResource(R.id.content_view_pause, R.drawable.pause_white);
            notificationBigContentView.setImageViewResource(R.id.big_cv_pause, R.drawable.pause_white);

            notificationBuilder.setCustomContentView(notificationContentView)
                    .setCustomBigContentView(notificationBigContentView);
            startForeground(NOTIFICATION_ID, notificationBuilder.build());

            if (mediaPlayer.getDuration() != resumePosition) {

                mediaPlayer.seekTo(storage.loadAudioIndexAndPosition()[1]); //Resumes from the position it was paused at
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.start();
                }
            }).start();

            registerBecomingNoisyReceiver();
            register_playNewAudio();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaSession.setPlaybackState(playbackState.setState(PlaybackStateCompat.STATE_PLAYING, resumePosition, mediaPlayer.getPlaybackParams().getSpeed()).build());
            }
            else mediaSession.setPlaybackState(playbackState.setState(PlaybackStateCompat.STATE_PLAYING, resumePosition, (float) 1.0).build());

        }
    }






    //Becoming noisy
    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }



    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }



    private final BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Get the new media index form SharedPreferences
            audioList = storage.loadAudio();
            audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndexAndPosition()[0];
            resumePosition = 0;
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }

            try {
                if (intent.getAction().equals("SHUFFLE")){
                    mediaSession.getController().getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                }
            }catch (Exception ignored){

            }

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            mediaPlayer.reset();
            stopMedia();
            initMediaPlayer();
        }
    };


    private void register_playNewAudio() {
        //Register playNewMedia receiver

        IntentFilter filter = new IntentFilter(SongsFragment.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }



    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "MUSIC PLAYER";
            String description = "Play Music";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

    }



    public class MyMediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {

            resumeMedia();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            super.onSkipToQueueItem(id);
        }

        @Override
        public void onPause() {

            pauseMedia();
        }

        @Override
        public void onSkipToNext() {

            if (repeatModeValue != 1) {
                if (!isShuffleOn) audioIndex = audioList.indexOf(activeAudio) + 1;
                else audioIndex = new Random().nextInt(audioList.size());
            }

            resumePosition =0;

            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
                stopMedia();
                initMediaPlayer();
            }
            else if(audioIndex == audioList.size()){

                audioIndex = 0;
                activeAudio =audioList.get(audioIndex);
                stopMedia();
                initMediaPlayer();

            }
            button = true;

        }

        @Override
        public void onSkipToPrevious() {

            if(mediaPlayer.getCurrentPosition() > 5000){

                mediaPlayer.seekTo(0);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mediaPlayer.start();
                    }
                }).start();
            }
            else {

                if (repeatModeValue != 1) {
                    if (!isShuffleOn) audioIndex = audioList.indexOf(activeAudio) - 1;
                    else audioIndex = new Random().nextInt(audioList.size());
                }


                resumePosition = 0;

                if (audioIndex != -1 && audioIndex < audioList.size()) {
                    //index is in a valid range
                    activeAudio = audioList.get(audioIndex);
                    stopMedia();
                    initMediaPlayer();
                } else if (audioIndex == -1) {

                    audioIndex = audioList.size() - 1;
                    activeAudio = audioList.get(audioIndex);
                    stopMedia();
                    initMediaPlayer();

                }

                button = true;
            }
        }



        @Override
        public void onStop() {

            resumePosition = 0;
            storage.storeAudioIndexAndPostion(audioIndex, resumePosition);
            mediaSession.setPlaybackState(playbackState.setState(PlaybackStateCompat.STATE_STOPPED, resumePosition , (float) 1.0).build());
            stopForeground(true);
            removeAudioFocus();
            if (phoneStateListener != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
            mediaSession.setActive(false);
            MainActivity.serviceBound = false;
            SharedPreferences equalizer = getSharedPreferences("EQUALIZER", MODE_PRIVATE);
            equalizer.edit().clear().apply();
            if (EqualizerActivity.virtualizer != null) EqualizerActivity.virtualizer.release();
            if (EqualizerActivity.bassBoost != null) EqualizerActivity.bassBoost.release();
            if (EqualizerActivity.equalizer != null) EqualizerActivity.equalizer.release();
            stopSelf();
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {

            if (shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL){
                isShuffleOn = true;
                mediaSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
            }
            else {
                isShuffleOn =false;
                mediaSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
            }

        }

        @Override
        public void onSetRepeatMode(int repeatMode) {

            repeatModeValue = repeatMode;

            if (repeatMode == 0) mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);
            else if (repeatMode == 1) mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE);
            else mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL);
        }


        @Override
        public void onCustomAction(String action, Bundle extras) {

            try {
                if (action.equals("SPEED")){

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed.getFloat("SPEED", (float) 1.0)));
                        if (speed.getInt("STATE", PlaybackStateCompat.STATE_PAUSED) != PlaybackStateCompat.STATE_PLAYING) mediaPlayer.pause();
                        mediaSession.setPlaybackState(playbackState.setState(speed.getInt("STATE", PlaybackStateCompat.STATE_PAUSED), mediaPlayer.getCurrentPosition(), speed.getFloat("SPEED", (float) 1.0)).build());
                    }

                }
            } catch (IllegalStateException e) {
                speed.edit().putFloat("SPEED", (float) 1.0).putInt("STATE", mediaSession.getController().getPlaybackState().getState()).apply();
                if (toast != null) toast.cancel();
                toast = null;
                toast = Toast.makeText(getApplicationContext(), "Unable to change Playback speed!", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }


    private void setNotification() {

        //Intents for the notification buttons
        PendingIntent stopSong;
        PendingIntent previousSong;
        PendingIntent nextSong;
        PendingIntent playpauseSong;

        stopSong = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP);
        previousSong = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        nextSong = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        playpauseSong = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE);

        notificationContentView.setImageViewResource(R.id.content_view_pause, R.drawable.pause_white);
        notificationContentView.setImageViewResource(R.id.content_view_stop, R.drawable.stop_white);
        notificationContentView.setImageViewResource(R.id.content_view_previous, R.drawable.previous_white);
        notificationContentView.setImageViewResource(R.id.content_view_next, R.drawable.next_white);

        notificationBigContentView.setImageViewResource(R.id.big_cv_pause, R.drawable.pause_white);
        notificationBigContentView.setImageViewResource(R.id.big_cv_stop, R.drawable.stop_white);
        notificationBigContentView.setImageViewResource(R.id.big_cv_previous, R.drawable.previous_white);
        notificationBigContentView.setImageViewResource(R.id.big_cv_next, R.drawable.next_white);

        notificationContentView.setOnClickPendingIntent(R.id.content_view_pause, playpauseSong);
        notificationContentView.setOnClickPendingIntent(R.id.content_view_stop, stopSong);
        notificationContentView.setOnClickPendingIntent(R.id.content_view_previous, previousSong);
        notificationContentView.setOnClickPendingIntent(R.id.content_view_next, nextSong);

        notificationBigContentView.setOnClickPendingIntent(R.id.big_cv_pause, playpauseSong);
        notificationBigContentView.setOnClickPendingIntent(R.id.big_cv_stop, stopSong);
        notificationBigContentView.setOnClickPendingIntent(R.id.big_cv_previous, previousSong);
        notificationBigContentView.setOnClickPendingIntent(R.id.big_cv_next, nextSong);

    }


}
