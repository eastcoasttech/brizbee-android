package com.brizbee.android.client;

import android.app.Application;

public class MyApplication extends Application {
    private String authExpiration;
    private String authToken;
    private String authUserId;

    public String getAuthExpiration() {
        return authExpiration;
    }

    public void setAuthExpiration(String authExpiration) {
        this.authExpiration = authExpiration;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }
}