package com.example.musicplayer;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.example.musicplayer.adapters.SongChanged;
import com.example.musicplayer.database.Songs;
import com.example.musicplayer.nowplaying.NowPlaying;
import com.example.musicplayer.ui.folders.FoldersFragment;
import com.example.musicplayer.ui.search.SearchActivity;
import com.example.musicplayer.ui.songs.SongsFragment;
import com.google.android.material.navigation.NavigationView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    public static final int UNIQUE_REQUEST_CODE = 69;
    public static boolean serviceBound = false;
    public static int index = 0;

    int bottom;

    TextView control_tv_title, control_tv_artist;
    ImageView control_iv_play, control_album_art;
    LinearLayout control_linear_layout;

    DrawerLayout drawer;
    NavController navController;
    NavGraph navGraph;
    NavigationView navigationView;

    SharedPreferences preferences;

    Intent playerIntent;

    private StorageUtil storage;
    private SharedPreferences startDestination;

    MediaSessionCompat.Token token;
    MediaControllerCompat mediaController;
    MediaBrowserCompat mediaBrowser;

     MediaBrowserCompat.ConnectionCallback  connectionCallback = new MediaBrowserCompat.ConnectionCallback(){
         @Override
         public void onConnected() {

             token = mediaBrowser.getSessionToken();

             try {

                 mediaController = new MediaControllerCompat(MainActivity.this, token);

                 new Runnable() {
                     @Override
                     public void run() {

                         mediaController.registerCallback(controllerCallback);
                         MediaControllerCompat.setMediaController(MainActivity.this, mediaController);
                         buttonControls();
                     }
                 }.run();


             } catch (Exception e) {
                 e.printStackTrace();
             }
         }

         @Override
         public void onConnectionSuspended() {
             super.onConnectionSuspended();
         }

         @Override
         public void onConnectionFailed() {
             super.onConnectionFailed();
         }
     };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FoldersFragment.currentPath = "/";
            FoldersFragment.slashCount = 0;
        }

        new Runnable() {
            @Override
            public void run() {

                Toolbar toolbar = findViewById(R.id.toolbar);
                toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.popup_menu_green));
                toolbar.setNavigationIcon(R.drawable.nav_green);
                setSupportActionBar(toolbar);

                preferences = getSharedPreferences("PREFERENCES", MODE_PRIVATE);

                playerIntent = new Intent(getApplicationContext(), MediaPlayerService.class);

                startDestination = getSharedPreferences("START", MODE_PRIVATE);

                storage = new StorageUtil(MainActivity.this);

                control_tv_title = findViewById(R.id.control_tv_title);
                control_tv_artist = findViewById(R.id.control_tv_artist);
                control_iv_play = findViewById(R.id.control_iv_play);
                control_album_art = findViewById(R.id.control_album_art);
                control_linear_layout = findViewById(R.id.control_linear_layout);


                drawer = findViewById(R.id.drawer_layout);
            }
        }.run();


        new Runnable() {
            @Override
            public void run() {

                // Passing each menu ID as a set of Ids because each
                // menu should be considered as top level destinations.
                navigationView = findViewById(R.id.nav_view);

                mAppBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.nav_folders, R.id.nav_songs, R.id.nav_playlists, R.id.nav_artists, R.id.nav_albums)
                        .setDrawerLayout(drawer)
                        .build();

                getSupportActionBar().setHomeAsUpIndicator(R.drawable.back_green);

                navController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment);
                NavigationUI.setupActionBarWithNavController(MainActivity.this, navController, mAppBarConfiguration);
                NavigationUI.setupWithNavController(navigationView, navController);

                navGraph = navController.getGraph();
                if (preferences.getString("START", "None").equals("None")) {
                    navGraph.setStartDestination(startDestination.getInt("ID", R.id.nav_songs));
                } else {
                    switch (preferences.getString("START", "Songs")){

                        case "Songs":
                            navGraph.setStartDestination(R.id.nav_songs);
                            break;

                        case "Albums":
                            navGraph.setStartDestination(R.id.nav_albums);
                            break;

                        case "Artists":
                            navGraph.setStartDestination(R.id.nav_artists);
                            break;

                        case "Folders":
                            navGraph.setStartDestination(R.id.nav_folders);
                            break;

                        case "Playlists":
                            navGraph.setStartDestination(R.id.nav_playlists);
                            break;
                    }
                    
                }

                if (savedInstanceState != null && savedInstanceState.getBundle("BUNDLE_NAVSTATE") != null) {
                    navController.restoreState(savedInstanceState.getBundle("BUNDLE_NAVSTATE"));
                }
                else navController.setGraph(navGraph);


            }
        }.run();

        control_linear_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(MainActivity.this, NowPlaying.class));

            }
        });

        bottom = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                60,
                getResources().getDisplayMetrics());

        if (control_linear_layout.getVisibility() == View.VISIBLE) {

            LinearLayout nav_host_linear_layout = findViewById(R.id.main_fragment);
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) nav_host_linear_layout.getLayoutParams();
            layoutParams.setMargins(0, 0, 0, bottom);
            nav_host_linear_layout.setLayoutParams(layoutParams);

        }

        mediaBrowser = new MediaBrowserCompat(getApplicationContext(), new ComponentName(this, MediaPlayerService.class), connectionCallback, null);


        if (storage.loadAudio() != null) {

            MediaPlayerService.audioList = storage.loadAudio();
            MediaPlayerService.audioIndex = storage.loadAudioIndexAndPosition()[0];
            if (MediaPlayerService.audioList.size() != 0 && MediaPlayerService.audioIndex >-1) MediaPlayerService.activeAudio = MediaPlayerService.audioList.get(MediaPlayerService.audioIndex);
        }

        String action = getIntent().getAction();

        if (action != null) {

            switch (action) {
                case "Album":
                    navController.navigate(R.id.action_global_albumSongsFragment2);
                    break;
                case "Artist":
                    navController.navigate(R.id.action_global_artist_albums_frag);
                    break;
                case Intent.ACTION_VIEW:
                    playAudioFromFile(getIntent().getData());
                    break;
            }
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        menu.getItem(3).getSubMenu().clearHeader();
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {

        index = 0;

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @Override
    protected void onStart() {
        super.onStart();

        new Runnable() {
            @Override
            public void run() {
                mediaBrowser.connect();
            }
        }.run();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (SearchActivity.openAlbumSongs) {
            NavHostFragment.findNavController(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).navigate(R.id.action_global_albumSongsFragment2);
            SearchActivity.openAlbumSongs = false;
        }
        else if (SearchActivity.openArtistAlbums){
            NavHostFragment.findNavController(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).navigate(R.id.action_global_artist_albums_frag);
            SearchActivity.openArtistAlbums = false;
        }


        preferences = getSharedPreferences("PREFERENCES", MODE_PRIVATE);

        navigationView.getMenu().getItem(0).setVisible(preferences.getBoolean("SONGS", true));
        navigationView.getMenu().getItem(1).setVisible(preferences.getBoolean("ALBUMS", true));
        navigationView.getMenu().getItem(2).setVisible(preferences.getBoolean("ARTISTS", true));
        navigationView.getMenu().getItem(3).setVisible(preferences.getBoolean("FOLDERS", true));
        navigationView.getMenu().getItem(4).setVisible(preferences.getBoolean("PLAYLISTS", true));

    }



    @Override
    protected void onStop() {
        super.onStop();

        if (mediaController != null && mediaBrowser != null) {
            mediaController.unregisterCallback(controllerCallback);
            mediaBrowser.disconnect();
        }
    }



    private void playAudio() {

        new Runnable() {
            @Override
            public void run() {
                startService(playerIntent);
                serviceBound = true;
            }
        }.run();
    }




    private void buttonControls(){

        new Runnable() {
            @Override
            public void run() {

                if (MediaPlayerService.activeAudio == null) {

                    control_tv_title.setText(storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getTitle());
                    control_tv_artist.setText(storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getArtist());

                    if (storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getAlbumid() != -100) {
                        Glide.with(MainActivity.this).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getAlbumid()))
                                .error(R.mipmap.cassette_image_foreground)
                                .placeholder(R.mipmap.cassette_image_foreground)
                                .centerCrop()
                                .fallback(R.mipmap.cassette_image_foreground)
                                .into(control_album_art);
                    }
                    else{
                        MediaMetadataRetriever m = new MediaMetadataRetriever();
                        m.setDataSource(storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getData());
                        Glide.with(MainActivity.this).load(m.getEmbeddedPicture())
                                .error(R.mipmap.cassette_image_foreground)
                                .placeholder(R.mipmap.cassette_image_foreground)
                                .centerCrop()
                                .fallback(R.mipmap.cassette_image_foreground)
                                .into(control_album_art);
                    }
                }
                else{

                    control_tv_title.setText(MediaPlayerService.activeAudio.getTitle());
                    control_tv_artist.setText(MediaPlayerService.activeAudio.getArtist());

                    if (MediaPlayerService.activeAudio.getAlbumid() != -100) {
                        Glide.with(MainActivity.this).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), MediaPlayerService.activeAudio.getAlbumid()))
                                .error(R.mipmap.cassette_image_foreground)
                                .placeholder(R.mipmap.cassette_image_foreground)
                                .centerCrop()
                                .fallback(R.mipmap.cassette_image_foreground)
                                .into(control_album_art);
                    }
                    else {
                        MediaMetadataRetriever m = new MediaMetadataRetriever();
                        m.setDataSource(MediaPlayerService.activeAudio.getData());
                        Glide.with(MainActivity.this).load(m.getEmbeddedPicture())
                                .error(R.mipmap.cassette_image_foreground)
                                .placeholder(R.mipmap.cassette_image_foreground)
                                .centerCrop()
                                .fallback(R.mipmap.cassette_image_foreground)
                                .into(control_album_art);
                    }
                }

            }

        }.run();

        new Runnable() {
            @Override
            public void run() {

                if (MediaControllerCompat.getMediaController(MainActivity.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){

                    control_iv_play.setImageResource(R.drawable.pause_green);
                }
                else {

                    control_iv_play.setImageResource(R.drawable.play_green);
                }

                control_linear_layout.setVisibility(View.VISIBLE);

                if (control_linear_layout.getVisibility() == View.VISIBLE){

                    LinearLayout nav_host_linear_layout = findViewById(R.id.main_fragment);
                    CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) nav_host_linear_layout.getLayoutParams();
                    layoutParams.setMargins(0,0,0, bottom);
                    nav_host_linear_layout.setLayoutParams(layoutParams);
                }

                control_iv_play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (MediaControllerCompat.getMediaController(MainActivity.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){


                            MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().pause();
                            control_iv_play.setImageResource(R.drawable.play_green);
                        }
                        else if (MediaControllerCompat.getMediaController(MainActivity.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED){

                            MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().play();
                            control_iv_play.setImageResource(R.drawable.pause_green);
                        }
                        else {

                            playAudio();
                        }


                    }
                });

            }

        }.run();


    }

    private final MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {

            if (state.getState() == PlaybackStateCompat.STATE_PLAYING){
                control_iv_play.setImageResource(R.drawable.pause_green);
            }
            else {
                control_iv_play.setImageResource(R.drawable.play_green);
            }

            control_linear_layout.setVisibility(View.VISIBLE);

            LinearLayout nav_host_linear_layout = findViewById(R.id.main_fragment);
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) nav_host_linear_layout.getLayoutParams();
            layoutParams.setMargins(0,0,0, bottom);
            nav_host_linear_layout.setLayoutParams(layoutParams);

        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {


            control_tv_title.setText(storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getTitle());
            control_tv_artist.setText(storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getArtist());

            if (storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getAlbumid() != -100) {
                Glide.with(MainActivity.this).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getAlbumid()))
                        .error(R.mipmap.cassette_image_foreground)
                        .placeholder(R.mipmap.cassette_image_foreground)
                        .centerCrop()
                        .fallback(R.mipmap.cassette_image_foreground)
                        .into(control_album_art);
            }
            else{
                MediaMetadataRetriever m = new MediaMetadataRetriever();
                m.setDataSource(storage.loadAudio().get(storage.loadAudioIndexAndPosition()[0]).getData());
                Glide.with(MainActivity.this).load(m.getEmbeddedPicture())
                        .error(R.mipmap.cassette_image_foreground)
                        .placeholder(R.mipmap.cassette_image_foreground)
                        .centerCrop()
                        .fallback(R.mipmap.cassette_image_foreground)
                        .into(control_album_art);
            }

            control_linear_layout.setVisibility(View.VISIBLE);

            LinearLayout nav_host_linear_layout = findViewById(R.id.main_fragment);
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) nav_host_linear_layout.getLayoutParams();
            layoutParams.setMargins(0,0,0, bottom);
            nav_host_linear_layout.setLayoutParams(layoutParams);

            SongChanged songChanged = (SongChanged) getForegroundFragment();
            songChanged.onSongChanged();
        }

    };


    @Override
    public void onBackPressed() {

        index = 0;

        if (drawer.isDrawerOpen(GravityCompat.START)) drawer.closeDrawer(GravityCompat.START);

        else {
            if (getForegroundFragment().getClass().getSimpleName().equals(FoldersFragment.class.getSimpleName())) {
                String path = FoldersFragment.currentPath.substring(0, FoldersFragment.currentPath.length() - 1);

                if (path.lastIndexOf("/") != -1) {
                    path = path.substring(0, path.lastIndexOf("/") + 1);
                    FoldersFragment.currentPath = path;
                    FoldersFragment.slashCount--;
                }
            }

            super.onBackPressed();

        }
    }

    public Fragment getForegroundFragment(){
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        return navHostFragment == null ? null : navHostFragment.getChildFragmentManager().getFragments().get(0);
    }


    public void onFolderClicked(MenuItem item) {
        FoldersFragment.currentPath = "/";
        FoldersFragment.slashCount = 0;
        NavHostFragment.findNavController(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).navigate(R.id.action_global_nav_folders);
        drawer.closeDrawer(GravityCompat.START);

        if (preferences.getString("START", "None").equals("None")) {
            navGraph = navController.getGraph();
            navGraph.setStartDestination(item.getItemId());
            startDestination.edit().putInt("ID", item.getItemId()).apply();
            navController.setGraph(navGraph);
        }

        index = 0;
    }

    public void onAlbumClicked(MenuItem item) {
        NavHostFragment.findNavController(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).navigate(R.id.nav_albums);
        drawer.closeDrawer(GravityCompat.START);

        if (preferences.getString("START", "None").equals("None")) {
            navGraph = navController.getGraph();
            navGraph.setStartDestination(item.getItemId());
            startDestination.edit().putInt("ID", item.getItemId()).apply();
            navController.setGraph(navGraph);
        }

        index = 0;
    }

    public void onArtistClicked(MenuItem item) {
        NavHostFragment.findNavController(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).navigate(R.id.nav_artists);
        drawer.closeDrawer(GravityCompat.START);

        if (preferences.getString("START", "None").equals("None")) {
            navGraph = navController.getGraph();
            navGraph.setStartDestination(item.getItemId());
            startDestination.edit().putInt("ID", item.getItemId()).apply();
            navController.setGraph(navGraph);
        }

        index = 0;
    }

    public void onSongClicked(MenuItem item) {
        NavHostFragment.findNavController(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).navigate(R.id.nav_songs);
        drawer.closeDrawer(GravityCompat.START);

        if (preferences.getString("START", "None").equals("None")) {
            navGraph = navController.getGraph();
            navGraph.setStartDestination(item.getItemId());
            startDestination.edit().putInt("ID", item.getItemId()).apply();
            navController.setGraph(navGraph);
        }

        index = 0;
    }

    public void onPlaylistClicked(MenuItem item) {
        NavHostFragment.findNavController(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).navigate(R.id.nav_playlists);
        drawer.closeDrawer(GravityCompat.START);

        if (preferences.getString("START", "None").equals("None")) {
            navGraph = navController.getGraph();
            navGraph.setStartDestination(item.getItemId());
            startDestination.edit().putInt("ID", item.getItemId()).apply();
            navController.setGraph(navGraph);
        }

        index = 0;
    }

    private void playAudioFromFile(Uri fileUri){

        ArrayList<Songs> song = new ArrayList<>();

        ContentResolver contentResolver = getContentResolver();

        String path = fileUri.toString();

        boolean isPathInMedia = true;


        if (path.contains("content://com.android.providers.downloads.documents")) {

            isPathInMedia = false;

            String docId = path.substring(path.lastIndexOf("/")+1);

            Uri downloadUri = Uri.parse("content://downloads/public_downloads");
            // Append download document id at uri end.
            Uri downloadUriAppendId = ContentUris.withAppendedId(downloadUri, Long.parseLong(docId));

            Cursor downloadCursor = contentResolver.query(downloadUriAppendId, null, null, null, null);

            if (downloadCursor != null && downloadCursor.getCount() > 0) {

                downloadCursor.moveToFirst();

                path = downloadCursor.getString(downloadCursor.getColumnIndex(MediaStore.Audio.Media.DATA));

            }
            if (downloadCursor != null) {

                downloadCursor.close();
            }
        }
        else if (path.contains("content://com.android.externalstorage.documents/")){
            isPathInMedia = false;
            try {
                path = "/storage" + URLDecoder.decode(path.substring(path.lastIndexOf("/")), "UTF-8").replaceFirst(":", "/");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;


        try {

            Cursor cursor;
            if (isPathInMedia) {
                String selection = android.provider.MediaStore.Audio.Media.IS_MUSIC + "!= 0 AND " + MediaStore.Audio.Media._ID + " = " + path.substring(path.lastIndexOf("/") + 1);
                cursor = contentResolver.query(uri, new String[]{"_id", "_data", "title", "artist", "album", "duration", "track", "artist_id", "album_id", "year"}, selection, null, null);
            }
            else {
                String selection = android.provider.MediaStore.Audio.Media.IS_MUSIC + "!= 0 AND " + MediaStore.Audio.Media.DATA + " ? ";
                cursor = contentResolver.query(uri, new String[]{"_id", "_data", "title", "artist", "album", "duration", "track", "artist_id", "album_id", "year"}, selection, new String[]{path}, null);
            }

            if (cursor != null && cursor.getCount() > 0) {

                cursor.moveToFirst();

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

                song.add(new Songs(id, data, title, artist, album, artistId, albumId, trackNumber, duration, year));
            }
            else throw new Exception("bruh");
            if (cursor != null) {

                cursor.close();
            }
        }catch (Exception e){
            MediaMetadataRetriever m = new MediaMetadataRetriever();
            m.setDataSource(path);
            int duration = Integer.parseInt(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            String title = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

            if (title.equals("null") || album.equals("null") || title.equals("")){

                title = path;
                album = "";
                artist = "";
            }

            song.add(new Songs(-100, path, title, album, artist, -100, -100, -100, duration, -100));

        }

        if (!MainActivity.serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(this);
            storage.storeAudio(song);
            storage.storeAudioIndexAndPostion(0, 0);


            startService(playerIntent);
            MainActivity.serviceBound = true;

        }


        else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(this);
            storage.storeAudio(song);
            storage.storeAudioIndexAndPostion(0, 0);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(SongsFragment.Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {

                startActivity(new Intent(MainActivity.this, NowPlaying.class));
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 69){
            if (resultCode == RESULT_OK){

                try {
                    RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaPlayerService.activeAudio.getId()));
                    Toast.makeText(this, MediaPlayerService.activeAudio.getTitle() + " set as Ringtone!", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Permission was not Granted!", Toast.LENGTH_SHORT).show();
                }

            }
            else {

                Toast.makeText(this, "Permission was not Granted!", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String action = intent.getAction();

        if (action != null) {

            switch (action) {
                case "Album":
                    navController.navigate(R.id.action_global_albumSongsFragment2);
                    break;
                case "Artist":
                    navController.navigate(R.id.action_global_artist_albums_frag);
                    break;
                case Intent.ACTION_VIEW:
                    playAudioFromFile(intent.getData());
                    break;
            }
        }

    }

    @Override
    protected void onDestroy() {

        Glide.get(this).clearMemory();
        Glide.get(this).trimMemory(TRIM_MEMORY_COMPLETE);

        control_iv_play = control_album_art = null;
        control_tv_title = control_tv_artist = null;
        control_linear_layout = null;
        drawer = null;
        navController = null;
        navGraph = null;
        navigationView = null;
        preferences = null;
        playerIntent = null;
        storage = null;
        startDestination = null;
        token = null;
        mediaController = null;
        mediaBrowser = null;

        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBundle("BUNDLE_NAVSTATE", navController.saveState());
        super.onSaveInstanceState(outState);
    }


}
