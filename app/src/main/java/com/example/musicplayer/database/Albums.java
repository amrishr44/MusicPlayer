package com.example.musicplayer.database;

public class Albums {

    private long id;
    private String album;
    private final String albumArt;
    private final int numSongs;
    private String artist;

    public Albums(long id, String album, String albumArt, int numSongs, String artist) {
        this.id = id;
        this.album = album;
        this.albumArt = albumArt;
        this.numSongs = numSongs;
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getAlbumArt() {
        return albumArt;
    }

    public int getNumSongs() {
        return numSongs;
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
