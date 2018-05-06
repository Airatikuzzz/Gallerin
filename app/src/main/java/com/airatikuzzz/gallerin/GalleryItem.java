package com.airatikuzzz.gallerin;

import com.google.gson.annotations.SerializedName;

/**
 * Created by maira on 14.07.2017.
 */

public class GalleryItem {
    @SerializedName("title")
    private String mCaption;
    @SerializedName("id")
    private String mId;

    public String getId() {
        return mId;
    }

    @SerializedName("url_s")
    private String mUrl;
    @SerializedName("owner")
    private String mOwner;
    @SerializedName("full_url")
    private String mUrlFull;

    public String getUrlFull() {
        return mUrlFull;
    }

    public void setUrlFull(String urlFull) {
        mUrlFull = urlFull;
    }

    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    @Override
    public String toString() {
        return mCaption;
    }
}
