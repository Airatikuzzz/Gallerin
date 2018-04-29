package com.airatikuzzz.gallerin;

/**
 * Created by maira on 27.04.2018.
 */

public enum Method {
    LIST_PHOTOS("LIST_PHOTOS"),
    SEARCH("SEARCH");

    public String getValue() {
        return value;
    }

    Method(String value) {

        this.value = value;
    }

    private String value;

}
