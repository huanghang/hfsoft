package com.dawn.java.util;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpURLconnectionClass {

    public static String getData(String urlpath) throws Exception {
        URL url = new URL(urlpath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.connect();
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            InputStream inputStream = conn.getInputStream();
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return parseWithJson(builder.toString());
        }
        conn.disconnect();
        return null;
    }

    public static String parseWithJson(String str) {
        String output = "";
        try {
            if (!str.equals("")) {
                JSONObject jsonStr = new JSONObject(str);
                int status = jsonStr.getInt("status");
                if (status == 0) {
                    JSONArray array = jsonStr.getJSONArray("data");
                    output = array.toString();
                } else {
                    Log.d("调用接口失败", jsonStr.getString("message"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }
}
