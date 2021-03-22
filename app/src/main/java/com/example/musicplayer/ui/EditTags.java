package com.example.musicplayer.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.musicplayer.R;
import com.example.musicplayer.database.Songs;

import java.util.ArrayList;

public class EditTags extends AppCompatActivity {

    public static Songs song;
    private ArrayList<String> newData;

    private EditText edit_tags_title, edit_tags_album, edit_tags_artist, edit_tags_track, edit_tags_year;

    private boolean wasChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tags);

        Toolbar toolbar = findViewById(R.id.et_toolbar);
        setSupportActionBar(toolbar);


        final ContentResolver contentResolver = getContentResolver();

        edit_tags_title = findViewById(R.id.edit_tags_title);
        edit_tags_album = findViewById(R.id.edit_tags_album);
        edit_tags_artist = findViewById(R.id.edit_tags_artist);
        edit_tags_track = findViewById(R.id.edit_tags_track);
        edit_tags_year = findViewById(R.id.edit_tags_year);

        TextView textView = findViewById(R.id.edit_song_data);
        textView.setText("You are editing: \n" + song.getData().trim());



        if (song != null){

            edit_tags_title.setText(song.getTitle());
            edit_tags_album.setText(song.getAlbum());
            edit_tags_artist.setText(song.getArtist());
            if (song.getTracknumber() > 0) edit_tags_track.setText(song.getTracknumber() + "");
            if (song.getYear() > 0) edit_tags_year.setText(song.getYear() + "");

        }

        Button bt_done = findViewById(R.id.bt_done);

        bt_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                ContentValues contentValue = new ContentValues();

                String track = edit_tags_track.getText().toString().trim();
                String year = edit_tags_year.getText().toString().trim();

                String title = edit_tags_title.getText().toString().trim();
                String album = edit_tags_album.getText().toString().trim();
                String artist = edit_tags_artist.getText().toString().trim();

                newData = new ArrayList<>();
                newData.add(title);
                newData.add(album);
                newData.add(artist);

                contentValue.put("title", title);
                contentValue.put("album", album);
                contentValue.put("artist", artist);

                if (!track.equals("") && !track.isEmpty())
                    contentValue.put("track", Integer.parseInt(track));
                if (!year.equals("") && !year.isEmpty())
                    contentValue.put("year", Long.valueOf(year));

                long album_id = 0;
                long album_artist_id = -1;
                long artist_id = 0;

                Cursor cursor = contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{"_id", "artist_id"}, "album =? ", new String[]{edit_tags_album.getText().toString().trim()}, null);

                if (cursor != null && cursor.getCount()>0){

                    cursor.moveToFirst();
                    do {
                        album_id = cursor.getLong(0);
                        album_artist_id = cursor.getLong(1);


                    }while(cursor.moveToNext());

                    cursor.close();

                }


                cursor = contentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "artist =? ", new String[]{edit_tags_artist.getText().toString().trim()}, null);

                if (cursor != null && cursor.getCount()>0){

                    cursor.moveToFirst();
                    do {
                        artist_id = cursor.getLong(0);
                    }while(cursor.moveToNext());

                    cursor.close();

                }

                if (artist_id != 0){
                    contentValue.put("artist_id", artist_id);
                }
                else{

                    cursor = contentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[]{"max(_id)"}, null, null, null);

                    if (cursor != null && cursor.getCount() > 0) {

                        cursor.moveToFirst();

                        long ar_id;

                        do {
                            ar_id = cursor.getLong(0)+1;
                        }while (cursor.moveToNext());

                        contentValue.put("artist_id", ar_id);
                        cursor.close();

                    }

                }

                if (album_artist_id == artist_id && album_id != 0){
                    contentValue.put("album_id", album_id);
                }
                else if(album_id == 0){

                    cursor = contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{"max(_id)"}, null, null, null);

                    if (cursor != null && cursor.getCount()>0) {
                        cursor.moveToFirst();

                        long al_id;

                        do {
                            al_id = cursor.getLong(0) + 1;
                        } while (cursor.moveToNext());

                        contentValue.put("album_id", al_id);
                    }
                    if (cursor != null) {

                        cursor.close();
                    }
                }

                contentResolver.update(uri, contentValue, "_id =? ", new String[]{String.valueOf(song.getId())});

                contentValue.clear();

                contentValue.put("audio_id", song.getId());

                Toast.makeText(EditTags.this, "Done!", Toast.LENGTH_SHORT).show();

                wasChanged = true;

            }
        });

        Button bt_cancel = findViewById(R.id.edit_tags_bt_cancel);

        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wasChanged){
                    Intent intent = new Intent();
                    intent.putStringArrayListExtra("data", newData);
                    setResult(RESULT_OK, intent);
                }
                else setResult(RESULT_CANCELED);
                EditTags.this.finish();
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {

        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (wasChanged){
            Intent intent = new Intent();
            intent.putStringArrayListExtra("data", newData);
            setResult(RESULT_OK, intent);
        }
        else setResult(RESULT_CANCELED);
       super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        newData = null;
        edit_tags_title = edit_tags_album = edit_tags_artist = edit_tags_track = edit_tags_year = null;

        super.onDestroy();
    }
}
