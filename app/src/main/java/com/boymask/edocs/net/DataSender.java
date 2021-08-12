package com.boymask.edocs.net;

import android.app.Activity;
import android.util.Log;

import com.boymask.edocs.EDocs;
import com.google.gson.Gson;
import com.litetech.libs.restservicelib.RestService;

public class DataSender {

    public static void send(Object data, Activity activity) {

        RestService.CallBack callback = (RestService.CallBack)activity;
        String json = new Gson().toJson(data);

        String serverAddress= EDocs.getServerAddress(activity);

     //   String url = "http://192.168.1.128:8080/ServerTest/rest/user/register";
        String url = "http://"+serverAddress+":8080/ServerTest/rest/user/register";
        Log.i("ss", url);
        RestService restService = new RestService( callback);
        //Executing call,Note it's Async call
        restService.execute(url, json, "post");
        //restService.execute(url);
    }
}