package com.brizbee.Brizbee.Mobile.models;

import java.util.Date;

public class User {
    private Date CreatedAt;
    private String EmailAddress;
    private int Id;
    private String Name;
    private boolean RequiresLocation;
    private String TimeZone;
    private boolean UsesMobileClock;
    private boolean UsesTimesheets;

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

    public boolean getUsesMobileClock() {
        return UsesMobileClock;
    }

    public void setUsesMobileClock(boolean usesMobileClock) {
        UsesMobileClock = usesMobileClock;
    }

    public boolean getUsesTimesheets() {
        return UsesTimesheets;
    }

    public void setUsesTimesheets(boolean usesTimesheets) {
        UsesTimesheets = usesTimesheets;
    }
}
