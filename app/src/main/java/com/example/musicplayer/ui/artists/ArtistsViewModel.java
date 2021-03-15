package com.example.musicplayer.ui.artists;

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

import com.example.musicplayer.database.Artists;

import java.util.ArrayList;

public class ArtistsViewModel extends AndroidViewModel {

    private final MutableLiveData<ArrayList<Artists>> artists;
    private final SharedPreferences sort;

    public ArtistsViewModel(@NonNull Application application) {
        super(application);

        sort = application.getSharedPreferences("Sort", Context.MODE_PRIVATE);

        artists =new MutableLiveData<>();
        artists.setValue(loadArtists());

    }


    public LiveData<ArrayList<Artists>> getArtists(){

        return artists;
    }

    public void refresh(){

        artists.postValue(loadArtists());
    }

    private ArrayList<Artists> loadArtists(){

        ArrayList<Artists> arrayList = new ArrayList<>();

        ContentResolver contentResolver = getApplication().getContentResolver();
        Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;

        String sortOrder;
        if (sort != null) {
            if (sort.getBoolean("ArtistReverse", false)) sortOrder = sort.getString("ArtistSortOrder", MediaStore.Audio.Artists.DEFAULT_SORT_ORDER) + " DESC";
            else sortOrder = sort.getString("ArtistSortOrder", MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);
        }
        else sortOrder = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER;

        Cursor cursor = contentResolver.query(uri, new String[]{"_id", "artist"}, null, null, sortOrder);

        if (cursor != null && cursor.getCount()>0) {

            cursor.moveToFirst();

            do {

                long id = cursor.getLong(0);
                String artist = cursor.getString(1).trim();


                arrayList.add(new Artists(id, artist));
            }
            while (cursor.moveToNext());

        }
        if (cursor != null) {

            cursor.close();
        }


        return arrayList;
    }

}
