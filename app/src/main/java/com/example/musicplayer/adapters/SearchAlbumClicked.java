package com.example.musicplayer.adapters;

import com.example.musicplayer.database.Albums;

public interface SearchAlbumClicked {

    void onSearchAlbumClicked(int index, Albums album);
    void onSearchAlbumLongClicked(int index);
}
