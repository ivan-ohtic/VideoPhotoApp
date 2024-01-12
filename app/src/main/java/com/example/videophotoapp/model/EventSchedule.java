package com.example.videophotoapp.model;

import java.util.List;

public class EventSchedule {
    private Schedule schedule;
    private List<Playlist> playlists;

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
    }
}
