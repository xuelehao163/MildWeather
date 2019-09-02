package com.example.administrator.mildweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by ${windy} on 02.
 *
 */

public class Basic {
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }

}
