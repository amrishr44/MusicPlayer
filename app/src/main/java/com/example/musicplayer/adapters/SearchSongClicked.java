package com.example.musicplayer.adapters;

import com.example.musicplayer.database.Songs;

public interface SearchSongClicked {

    void onSearchSongClicked(int index, Songs song);
    void onSearchSongLongClicked(int index);
}
