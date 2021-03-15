package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.musicplayer.database.Songs;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

//Used for storing and loading information of songs that are to be played
//It is stored in the form of json in SharedPreferences
public class StorageUtil {

    private final String STORAGE = " com.example.musicplayer.STORAGE";
    private SharedPreferences preferences;
    private final Context context;

    public StorageUtil(Context context) {
        this.context = context;
    }

    /**
     * Used to store the information of songs to be played
     *
     * @param arrayList - the list of songs to be played
     * */

    public void storeAudio(ArrayList<Songs> arrayList) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("audioArrayList", json);   //The list of songs are converted to json form and stored in a SharedPreferences file
        editor.apply();
    }

    /**
     * Used to load the information of songs to be played
     *
     * @return  - the list of songs to be played
     * */

    public ArrayList<Songs> loadAudio() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("audioArrayList", null);
        return gson.fromJson(json, new TypeToken<List<Songs>>() {}.getType());
    }

    /**
     * Used to store the index of the song to be played in the list of songs and the current position of the song
     *
     * @param index  - the index of the song in the list of songs
     * @param position  - the current position of the song
     * */

    public void storeAudioIndexAndPostion(int index, int position) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioIndex", index);
        editor.putInt("position", position);
        editor.apply();
    }

    /**
     * Used to load the index of the song to be played in the list of songs and the current position of the song
     *
     * @return  - (int [] {index, postion}) - an integer array containing the index of the song in the list of songs and the current position of the song
     * */

    public int[] loadAudioIndexAndPosition() {

        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return new int[]{preferences.getInt("audioIndex", -1), preferences.getInt("position", 0)};//return -1 if no data found
    }

}


