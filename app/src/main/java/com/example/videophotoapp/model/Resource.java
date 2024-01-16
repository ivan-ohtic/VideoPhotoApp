package com.example.videophotoapp.model;

public class Resource {
    private String id;
    private int order;
    private String name;
    private int duration;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * Determina si el recurso es un video basado en la extensión del nombre del archivo.
     *
     * @return true si el nombre del archivo termina con ".mp4", false en caso contrario.
     */
    public boolean isVideo() {
        return name != null && name.toLowerCase().endsWith(".mp4");
    }
}
