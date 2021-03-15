package com.example.musicplayer.database;



public class Songs {


    private long id;
    private String data;
    private String title;
    private String album;
    private String artist;
    private final long artistid;
    private final long albumid;
    private final int tracknumber;
    private  int duration;
    private long year;


    public Songs(long id, String data, String title, String album, String artist, long artistid, long albumid, int tracknumber, int duration, long year) {
        this.id = id;
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.artistid = artistid;
        this.albumid = albumid;
        this.tracknumber = tracknumber;
        this.duration = duration;
        this.year = year;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public long getArtistid() {
        return artistid;
    }

    public long getAlbumid() {
        return albumid;
    }

    public int getTracknumber() {
        return tracknumber;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getYear() {
        return year;
    }

    public void setYear(long year) {
        this.year = year;
    }
}
