package com.example.musicplayer.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.musicplayer.R;

public class SettingsActivity extends AppCompatActivity {

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);


        preferences = getSharedPreferences("PREFERENCES", Context.MODE_PRIVATE);

        RadioGroup radioGroup = findViewById(R.id.radiogroup_start);

        switch (preferences.getString("START", "None")){

            case "None":
                radioGroup.check(R.id.radioButton_none);
                break;

            case "Songs":
                radioGroup.check(R.id.radioButton_songs);
                break;

            case "Albums":
                radioGroup.check(R.id.radioButton_albums);
                break;

            case "Artists":
                radioGroup.check(R.id.radioButton_artists);
                break;

            case "Folders":
                radioGroup.check(R.id.radioButton_folders);
                break;

            case "Playlists":
                radioGroup.check(R.id.radioButton_playlists);
                break;

        }



        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if (checkedId == R.id.radioButton_none) preferences.edit().putString("START", "None").apply();
                else if (checkedId == R.id.radioButton_songs) preferences.edit().putString("START", "Songs").apply();
                else if (checkedId == R.id.radioButton_albums) preferences.edit().putString("START", "Albums").apply();
                else if (checkedId == R.id.radioButton_artists) preferences.edit().putString("START", "Artists").apply();
                else if (checkedId == R.id.radioButton_folders) preferences.edit().putString("START", "Folders").apply();
                else if (checkedId == R.id.radioButton_playlists) preferences.edit().putString("START", "Playlists").apply();
            }
        });

        final CheckBox checkbox_songs = findViewById(R.id.checkbox_songs);

        checkbox_songs.setChecked(preferences.getBoolean("SONGS", true));

        checkbox_songs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checkbox_songs.isChecked() && preferences.getInt("BROWSERS", 5) != 1){
                    preferences.edit().putBoolean("SONGS", true).putInt("BROWSERS", preferences.getInt("BROWSERS", 5) + 1).apply();
                }
                else if (!checkbox_songs.isChecked() && preferences.getInt("BROWSERS", 5) == 1){
                    Toast.makeText(SettingsActivity.this , "Atleast one browser must be visible", Toast.LENGTH_SHORT).show();
                    checkbox_songs.setChecked(true);
                }
                else{
                    preferences.edit().putBoolean("SONGS", false).putInt("BROWSERS", preferences.getInt("BROWSERS", 5) - 1).apply();
                }

            }
        });

        final CheckBox checkbox_albums = findViewById(R.id.checkbox_albums);

        checkbox_albums.setChecked(preferences.getBoolean("ALBUMS", true));

        checkbox_albums.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checkbox_albums.isChecked() && preferences.getInt("BROWSERS", 5) != 1){
                    preferences.edit().putBoolean("ALBUMS", true).putInt("BROWSERS", preferences.getInt("BROWSERS", 5) + 1).apply();
                }
                else if (!checkbox_albums.isChecked() && preferences.getInt("BROWSERS", 5) == 1){
                    Toast.makeText(SettingsActivity.this, "Atleast one browser must be visible", Toast.LENGTH_SHORT).show();
                    checkbox_albums.setChecked(true);
                }
                else{
                    preferences.edit().putBoolean("ALBUMS", false).putInt("BROWSERS", preferences.getInt("BROWSERS", 5) - 1).apply();
                }

            }
        });

        final CheckBox checkbox_artists = findViewById(R.id.checkbox_artists);

        checkbox_artists.setChecked(preferences.getBoolean("ARTISTS", true));

        checkbox_artists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checkbox_artists.isChecked() && preferences.getInt("BROWSERS", 5) != 1){
                    preferences.edit().putBoolean("ARTISTS", true).putInt("BROWSERS", preferences.getInt("BROWSERS", 5) + 1).apply();
                }
                else if (!checkbox_artists.isChecked() && preferences.getInt("BROWSERS", 5) == 1){
                    Toast.makeText(SettingsActivity.this, "Atleast one browser must be visible", Toast.LENGTH_SHORT).show();
                    checkbox_artists.setChecked(true);
                }
                else{
                    preferences.edit().putBoolean("ARTISTS", false).putInt("BROWSERS", preferences.getInt("BROWSERS", 5) - 1).apply();
                }

            }
        });

        final CheckBox checkbox_folders = findViewById(R.id.checkbox_folders);

        checkbox_folders.setChecked(preferences.getBoolean("FOLDERS", true));

        checkbox_folders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checkbox_folders.isChecked() && preferences.getInt("BROWSERS", 5) != 1){
                    preferences.edit().putBoolean("FOLDERS", true).putInt("BROWSERS", preferences.getInt("BROWSERS", 5) + 1).apply();
                }
                else if (!checkbox_folders.isChecked() && preferences.getInt("BROWSERS", 5) == 1){
                    Toast.makeText(SettingsActivity.this, "Atleast one browser must be visible", Toast.LENGTH_SHORT).show();
                    checkbox_folders.setChecked(true);
                }
                else{
                    preferences.edit().putBoolean("FOLDERS", false).putInt("BROWSERS", preferences.getInt("BROWSERS", 5) - 1).apply();
                }

            }
        });

        final CheckBox checkbox_playlists = findViewById(R.id.checkbox_playlist);

        checkbox_playlists.setChecked(preferences.getBoolean("PLAYLISTS", true));

        checkbox_playlists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checkbox_playlists.isChecked() && preferences.getInt("BROWSERS", 5) != 1){
                    preferences.edit().putBoolean("PLAYLISTS", true).putInt("BROWSERS", preferences.getInt("BROWSERS", 5) + 1).apply();
                }
                else if (!checkbox_playlists.isChecked() && preferences.getInt("BROWSERS", 5) == 1){
                    Toast.makeText(SettingsActivity.this, "Atleast one browser must be visible", Toast.LENGTH_SHORT).show();
                    checkbox_playlists.setChecked(true);
                }
                else{
                    preferences.edit().putBoolean("PLAYLISTS", false).putInt("BROWSERS", preferences.getInt("BROWSERS", 5) - 1).apply();
                }

            }
        });

        final CheckBox checkbox_wallpaper = findViewById(R.id.checkbox_wallpaper);

        checkbox_wallpaper.setChecked(preferences.getBoolean("LOCKSCREEN", true));

        checkbox_wallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checkbox_wallpaper.isChecked()) preferences.edit().putBoolean("LOCKSCREEN", true).apply();
                else preferences.edit().putBoolean("LOCKSCREEN", false).apply();

            }
        });


    }

    @Override
    public boolean onSupportNavigateUp() {
        this.finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        preferences = null;

        super.onDestroy();
    }
}
