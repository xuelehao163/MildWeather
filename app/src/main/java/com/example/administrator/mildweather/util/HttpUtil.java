package com.example.administrator.mildweather.util;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by ${windy} on 27.
 * 网络连接
 */

public class HttpUtil {
    public static void sendOkhttpRequest(String adress, Callback callback){
        //新建客户端
        OkHttpClient client=new OkHttpClient();
        //发送请求
        Request request=new Request.Builder().url(adress).build();
        //获取返回数据
        client.newCall(request).enqueue(callback);
    }
}
