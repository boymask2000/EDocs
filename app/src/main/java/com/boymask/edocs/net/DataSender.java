package com.boymask.edocs.net;

import com.google.gson.Gson;
import com.litetech.libs.restservicelib.RestService;

public class DataSender {

    public static void send(Object data,  RestService.CallBack callBack) {
        String json = new Gson().toJson(data);

        String url = "http://192.168.1.128:8080/ServerTest/rest/user/register";
        RestService restService = new RestService( callBack);
        //Executing call,Note it's Async call
        restService.execute(url, json, "post");
        //restService.execute(url);
    }
}