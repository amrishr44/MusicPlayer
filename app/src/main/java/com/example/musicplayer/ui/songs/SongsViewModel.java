package com.example.musicplayer.ui.songs;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.musicplayer.database.Songs;

import java.util.ArrayList;

public class SongsViewModel extends AndroidViewModel {

    private final MutableLiveData<ArrayList<Songs>> mySongs;
    private final SharedPreferences sort;

    public SongsViewModel(@NonNull Application application) {
        super(application);

        mySongs = new MutableLiveData<>();
        sort = application.getSharedPreferences("Sort", Context.MODE_PRIVATE);
        mySongs.setValue(loadAudio());
    }

    void refresh(){

        mySongs.postValue(loadAudio());
    }




    public LiveData<ArrayList<Songs>> getSongs(){
        return mySongs;
    }



    private ArrayList<Songs> loadAudio()
    {
        ArrayList<Songs> arrayList = new ArrayList<>();

        ContentResolver contentResolver = getApplication().getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = android.provider.MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder;
        if (sort != null) {
            if (sort.getBoolean("SongReverse", false)) sortOrder = sort.getString("SongSortOrder", MediaStore.Audio.Media.DEFAULT_SORT_ORDER) + " DESC";
            else sortOrder = sort.getString("SongSortOrder", MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        }
        else sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
        Cursor cursor = contentResolver.query(uri, new String[]{"_id","_data", "title", "artist", "album", "duration", "track", "artist_id", "album_id", "year"}, selection, null, sortOrder);

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


                arrayList.add(new Songs(id, data, title, album, artist, artistId, albumId, trackNumber, duration, year));
            }
            while (cursor.moveToNext());

        }
        if (cursor != null) {

            cursor.close();
        }

        return arrayList;
    }


}