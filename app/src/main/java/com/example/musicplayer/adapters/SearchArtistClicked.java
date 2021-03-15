package com.example.musicplayer.adapters;

import com.example.musicplayer.database.Artists;

public interface SearchArtistClicked {

    void onSearchArtistClicked(int index, Artists artist);
    void onSearchArtistLongClicked(int index);
}
