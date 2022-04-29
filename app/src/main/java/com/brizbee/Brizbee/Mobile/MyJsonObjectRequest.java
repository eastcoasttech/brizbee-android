package com.brizbee.Brizbee.Mobile;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class MyJsonObjectRequest extends JsonObjectRequest {

    public MyJsonObjectRequest(int method, String url, @Nullable JSONObject jsonRequest, Response.Listener<JSONObject> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            // Patch the ability to parse with an OK response to POST request.
            if (getMethod() == Method.POST && response.statusCode == 200)
                return Response.success(null, HttpHeaderParser.parseCacheHeaders(response));

            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));

            // Patch the ability to parse with an empty response.
            if (jsonString.length() == 0) {
                return Response.success(null, HttpHeaderParser.parseCacheHeaders(response));
            }

            return Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            return Response.error(new ParseError(e));
        }
    }
}
