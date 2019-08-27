package com.brizbee.android.client.models;

import java.time.LocalTime;
import java.util.Date;

public class User {
    private Date CreatedAt;
    private String EmailAddress;
    private int Id;
    private String Name;
    private boolean RequiresLocation;
    private String TimeZone;

    public Date getCreatedAt() {
        return CreatedAt;
    }

    public void setCreatedAt(Date createdAt) {
        CreatedAt = createdAt;
    }

    public String getEmailAddress() {
        return EmailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        EmailAddress = emailAddress;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public boolean getRequiresLocation() {
        return RequiresLocation;
    }

    public void setRequiresLocation(boolean requiresLocation) {
        RequiresLocation = requiresLocation;
    }

    public String getTimeZone() {
        return TimeZone;
    }

    public void setTimeZone(String timeZone) {
        TimeZone = timeZone;
    }
}
