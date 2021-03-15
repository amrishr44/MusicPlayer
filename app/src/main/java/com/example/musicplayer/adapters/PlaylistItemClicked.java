package com.example.musicplayer.adapters;

import android.app.Dialog;

import com.example.musicplayer.database.Songs;

import java.util.ArrayList;

public interface PlaylistItemClicked {

    void onPlaylistItemClicked(int index, long id, Dialog dialog);
    void onPlaylistItemClicked(long id, Dialog dialog, ArrayList<Songs> mySongs);

}
