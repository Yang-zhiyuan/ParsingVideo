package com.example.util;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.parse.MainActivity;

import org.jsoup.Jsoup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsingVideo {
    public static final String FILE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/抖音无水印/";

    public static boolean checkStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkAlertWindowsPermission(Context context) {
        try {
            Object object = context.getSystemService(Context.APP_OPS_SERVICE);
            if (object == null) {
                return false;
            }
            Class localClass = object.getClass();
            Class[] arrayOfClass = new Class[3];
            arrayOfClass[0] = Integer.TYPE;
            arrayOfClass[1] = Integer.TYPE;
            arrayOfClass[2] = String.class;
            Method method = localClass.getMethod("checkOp", arrayOfClass);
            if (method == null) {
                return false;
            }
            Object[] arrayOfObject1 = new Object[3];
            arrayOfObject1[0] = 24;
            arrayOfObject1[1] = Binder.getCallingUid();
            arrayOfObject1[2] = context.getPackageName();
            int m = ((Integer) method.invoke(object, arrayOfObject1));
            return m == AppOpsManager.MODE_ALLOWED;
        } catch (Exception ex) {

        }
        return false;
    }
//    public static boolean checkAlertPermission(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED) {
//                return false;
//            }
//        }
//        return true;
//    }

    public static void parse(final Context context, final String url) {
        if (!checkStoragePermission(context)) {
            Toast.makeText(context, "没有存储权限", Toast.LENGTH_SHORT).show();
            return;
        }
        new AsyncTask<String, Integer, Integer>() {
            @Override
            protected void onPreExecute() {
                Toast.makeText(context, "开始下载", Toast.LENGTH_SHORT).show();
            }

            protected Integer doInBackground(String... urls) {
                try {
                    String url = urls[0];
                    Pattern pattern = Patterns.WEB_URL;
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.find()) {
                        url = matcher.group(0);
                    }
                    String htmls = null;
                    htmls = Jsoup.connect(url).ignoreContentType(true).execute()
                            .body();
                    Pattern patternCompile = Pattern
                            .compile("(?<=playAddr: \")https?://.+(?=\",)");
                    matcher = patternCompile.matcher(htmls);
                    String matchUrl = "";
                    while (matcher.find()) {
                        matchUrl = matcher.group(0).replaceAll("playwm", "play");
                    }
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Connection", "keep-alive");
                    headers.put(
                            "User-Agent",
                            "Mozilla/5.0 (iPhone; CPU iPhone OS 12_1_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/16D57 Version/12.0 Safari/604.1");
                    Log.e("TAG", matchUrl);
                    BufferedInputStream in = Jsoup.connect(matchUrl).headers(headers)
                            .timeout(10000).ignoreContentType(true).execute().bodyStream();
                    String FILE_NAME = UUID.randomUUID().toString().replaceAll("-", "");
                    File file = new File(FILE_PATH + FILE_NAME + ".mp4");
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                    int b;
                    while ((b = in.read()) != -1) {
                        out.write(b);
                    }
                    out.close();
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return 0;
                } catch (Exception e) {
                    e.printStackTrace();
                    return 0;
                }
                return 1;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result == 1) {
                    Toast.makeText(context, "下载成功!文件保存目录:" + FILE_PATH, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(url);
    }
}
