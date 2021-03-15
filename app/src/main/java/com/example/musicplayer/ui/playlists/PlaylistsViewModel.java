package com.example.musicplayer.ui.playlists;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.musicplayer.database.Playlists;

import java.util.ArrayList;

public class PlaylistsViewModel extends AndroidViewModel {

    private final MutableLiveData<ArrayList<Playlists>> playlists;


    public PlaylistsViewModel(@NonNull Application application) {
        super(application);

        playlists = new MutableLiveData<>();
        playlists.setValue(loadPlaylists(application));
    }


    public LiveData<ArrayList<Playlists>> getPlaylists() {
        return playlists;
    }

    public void refresh(){
        playlists.postValue(loadPlaylists(getApplication()));
    }


    public static ArrayList<Playlists> loadPlaylists(Context context)
    {
        ArrayList<Playlists> arrayList = new ArrayList<>();

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri =  MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER;
        Cursor cursor = contentResolver.query(uri, new String[]{"_id","name"}, null, null, sortOrder);

        if (cursor != null && cursor.getCount()>0) {

            cursor.moveToFirst();

            do {
                long id = cursor.getLong(0);
                String name = cursor.getString(1).trim();


                arrayList.add(new Playlists(id, name));
            }
            while (cursor.moveToNext());

        }
        if (cursor != null) {

            cursor.close();
        }

        return arrayList;
    }


}