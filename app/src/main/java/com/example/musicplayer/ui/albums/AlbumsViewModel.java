package com.example.musicplayer.ui.albums;

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

import com.example.musicplayer.database.Albums;

import java.util.ArrayList;

public class AlbumsViewModel extends AndroidViewModel {

    private final MutableLiveData<ArrayList<Albums>> myAlbums;
    private final SharedPreferences sort;

    public AlbumsViewModel(@NonNull Application application) {
        super(application);

        sort = application.getSharedPreferences("Sort", Context.MODE_PRIVATE);

        myAlbums = new MutableLiveData<>();
        myAlbums.setValue(loadAlbums());

    }

    public LiveData<ArrayList<Albums>> getAlbums(){

        return myAlbums;
    }

    public void refresh(){
        myAlbums.postValue(loadAlbums());
    }


    private ArrayList<Albums> loadAlbums(){

        ArrayList<Albums> arrayList = new ArrayList<>();

        ContentResolver contentResolver = getApplication().getContentResolver();
        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String sortOrder;
        if (sort != null) {
            if (sort.getBoolean("AlbumReverse", false)) sortOrder = sort.getString("AlbumSortOrder", MediaStore.Audio.Albums.DEFAULT_SORT_ORDER) + " DESC";
            else sortOrder = sort.getString("AlbumSortOrder", MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
        }
        else sortOrder = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER;


        Cursor cursor = contentResolver.query(uri, new String[]{"_id","album","album_art", "numsongs", "artist"}, null, null, sortOrder);

        if (cursor != null && cursor.getCount()>0) {

            cursor.moveToFirst();

            do {

                long id = cursor.getLong(0);
                String album = cursor.getString(1).trim();
                String albumArt = cursor.getString(2);
                int numSongs = cursor.getInt(3);
                String artist = cursor.getString(4).trim();


                arrayList.add(new Albums(id, album, albumArt, numSongs, artist));
            }
            while (cursor.moveToNext());

        }
        if (cursor != null) {

            cursor.close();
        }


        return arrayList;

    }

}
