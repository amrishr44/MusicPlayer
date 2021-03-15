package com.example.musicplayer.database;

public class Artists {

    private long id;
    private String artist;

    public Artists(long id, String artist) {
        this.id = id;
        this.artist = artist;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
