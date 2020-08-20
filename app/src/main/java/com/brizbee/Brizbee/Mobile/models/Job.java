package com.brizbee.Brizbee.Mobile.models;

import java.util.Date;

public class Job {
    private Date CreatedAt;
    private int Id;
    private String Name;
    private String Number;

    public Date getCreatedAt() {
        return CreatedAt;
    }

    public void setCreatedAt(Date createdAt) {
        CreatedAt = createdAt;
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

    public String getNumber() {
        return Number;
    }

    public void setNumber(String number) {
        Number = number;
    }

    @Override
    public String toString() {
        return String.format("%s - %s", getNumber(), getName());
    }
}
