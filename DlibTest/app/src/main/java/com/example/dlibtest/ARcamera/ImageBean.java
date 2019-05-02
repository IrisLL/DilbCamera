package com.example.dlibtest.ARcamera;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ImageBean extends RealmObject {
    @PrimaryKey
    private long id;
    private String path;
    private String name;
    private long date;
    private boolean hasFace;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean isHasFace() {
        return hasFace;
    }

    public void setHasFace(boolean hasFace) {
        this.hasFace = hasFace;
    }

    public boolean isNotNull() {
        return path != null && name != null;
    }

    @Override
    public String toString() {
        return "ImageBean{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", date=" + date +
                ", hasFace=" + hasFace +
                '}';
    }
}
