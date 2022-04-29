//
//  MyApplication.java
//  BRIZBEE Mobile for Android
//
//  Copyright © 2021 East Coast Technology Services, LLC
//
//  This file is part of BRIZBEE Mobile for Android.
//
//  BRIZBEE Mobile for Android is free software: you can redistribute
//  it and/or modify it under the terms of the GNU General Public
//  License as published by the Free Software Foundation, either
//  version 3 of the License, or (at your option) any later version.
//
//  BRIZBEE Mobile for Android is distributed in the hope that it will
//  be useful, but WITHOUT ANY WARRANTY; without even the implied
//  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with BRIZBEE Mobile for Android.
//  If not, see <https://www.gnu.org/licenses/>.
//

package com.brizbee.Brizbee.Mobile;

import android.app.Application;

import com.brizbee.Brizbee.Mobile.models.Organization;
import com.brizbee.Brizbee.Mobile.models.TimeZone;
import com.brizbee.Brizbee.Mobile.models.User;

import java.util.HashMap;

public class MyApplication extends Application {
    private String jwt;
    private Organization organization;
    private TimeZone[] timeZones;
    private User user;

    public HashMap<String, String> getAuthHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        if (jwt != null && !jwt.isEmpty()) {
            headers.put("Authorization", "Bearer " + jwt);
        }

        return headers;
    }

    public String getBaseUrl() {
        if (baseUrl == null || baseUrl.isEmpty())
        {
            return "https://app-brizbee-api-prod.azurewebsites.net";
        }

        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private String baseUrl;

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
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