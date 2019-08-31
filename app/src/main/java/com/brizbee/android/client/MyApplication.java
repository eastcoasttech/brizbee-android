package com.brizbee.android.client;

import android.app.Application;

import com.brizbee.android.client.models.Organization;
import com.brizbee.android.client.models.TimeZone;
import com.brizbee.android.client.models.User;

import org.json.JSONObject;

public class MyApplication extends Application {
    private String authExpiration;
    private String authToken;
    private String authUserId;
    private Organization organization;
    private TimeZone[] timeZones;
    private User user;

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

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public TimeZone[] getTimeZones() {
        return timeZones;
    }

    public void setTimeZones(TimeZone[] timeZones) {
        this.timeZones = timeZones;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}