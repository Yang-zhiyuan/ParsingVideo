package com.example.util;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

interface DownLoadListener {
    void onStartDownload(Context context);

    void onDownloading(Context context, int progress);

    void onErrorDownload(Context context);

    void onFinishDownload(Context context, String path);
}


public class VideoUtil implements DownLoadListener {
    private static final VideoUtil videoUtil = new VideoUtil();
    private static final Handler handler = new Handler();

    private VideoUtil() {
    }

    public static VideoUtil getInstance() {
        return videoUtil;
    }

    @Override
    public void onStartDownload(final Context context) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "开始下载", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDownloading(final Context context, final int progress) {

    }

    @Override
    public void onErrorDownload(final Context context) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onFinishDownload(final Context context, final String path) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "下载成功!文件保存目录:" + path, Toast.LENGTH_LONG).show();
                File file = new File(path);
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file)));
                // 扫描单个媒体文件，注意是文件，不是文件夹
                // new SingleMediaScanner(context, file);
//                // 通知图库更新
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    MediaScannerConnection.scanFile(context, new String[]{path}, null,
//                            new MediaScannerConnection.OnScanCompletedListener() {
//                                public void onScanCompleted(String path, Uri uri) {
//                                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//                                    mediaScanIntent.setData(uri);
//                                    context.sendBroadcast(mediaScanIntent);
//                                }
//                            });
//                } else {
//                    String relationDir = new File(path).getParent();
//                    File file1 = new File(relationDir);
//                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(file1.getAbsoluteFile())));
//                }
            }
        });
    }

//    private boolean deleteSingleFile(String filePath$Name) {
//        File file = new File(filePath$Name);
//        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
//        if (file.exists() && file.isFile()) {
//            return file.delete();
//        } else {
//            return false;
//        }
//    }

    public static final String FILE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();

    public void writeXY(Context context, int x, int y) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("x", x);
        editor.putInt("y", y);
        editor.commit();
    }

    public int getX(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("x", 0);
    }

    public int getY(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("y", 0);
    }

    public boolean checkStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public boolean checkAlertWindowsPermission(Context context) {
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
            ex.printStackTrace();
        }
        return false;
    }

    public void parse(final Context context, String url) {
        if (!checkStoragePermission(context)) {
            Toast.makeText(context, "没有存储权限", Toast.LENGTH_SHORT).show();
            return;
        }
        final OkHttpClient okHttpClient = new OkHttpClient();
        Pattern pattern = Patterns.WEB_URL;
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            url = matcher.group(0);
            if (url.contains("douyin")) {
                try {
                    onStartDownload(context);
                    Request request = new Request.Builder().url(url).build();
                    okHttpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            onErrorDownload(context);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {

                            String html = response.body().string();
                            Pattern patternCompile = Pattern
                                    .compile("(?<=playAddr: \")https?://.+(?=\",)");
                            Matcher matcher = patternCompile.matcher(html);
                            while (matcher.find()) {
                                Request request = new Request.Builder().url(matcher.group(0).replaceAll("playwm", "play")).addHeader("User-Agent",
                                        "Mozilla/5.0 (iPhone; CPU iPhone OS 12_1_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/16D57 Version/12.0 Safari/604.1").build();
                                Log.e("TAG", matcher.group(0).replaceAll("playwm", "play"));
                                okHttpClient.newCall(request).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        onErrorDownload(context);
                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        InputStream is = null;
                                        byte[] buf = new byte[2048];
                                        int len = 0;
                                        FileOutputStream fos = null;
                                        String FILE_NAME = UUID.randomUUID().toString().replaceAll("-", "");
                                        File file = new File(FILE_PATH + "/DouYinDL/" + FILE_NAME + ".mp4");
                                        if (!file.getParentFile().exists()) {
                                            file.getParentFile().mkdirs();
                                        }
                                        try {
                                            is = response.body().byteStream();
                                            long total = response.body().contentLength();
                                            fos = new FileOutputStream(file);
                                            long sum = 0;
                                            while ((len = is.read(buf)) != -1) {
                                                fos.write(buf, 0, len);
                                                sum += len;
                                                int progress = (int) (sum * 1.0f / total * 100);
                                                onDownloading(context, progress);
                                            }
                                            fos.flush();
                                            onFinishDownload(context, file.getAbsolutePath());
                                        } catch (Exception e) {
                                            onErrorDownload(context);
                                            e.printStackTrace();
                                        } finally {
                                            try {
                                                if (is != null) {
                                                    is.close();
                                                }
                                                if (fos != null) {
                                                    fos.close();
                                                }
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    onErrorDownload(context);
                }
            } else if (url.contains("weishi")) {
                try {
                    matcher = Pattern.compile("\\w{17}").matcher(url);
                    if (matcher.find()) {
                        String feedId = matcher.group(0);
                        String matchUrl = "https://h5.qzone.qq.com/webapp/json/weishi/WSH5GetPlayPage?feedid=" + feedId;
                        Request request = new Request.Builder().url(matchUrl).build();
                        onStartDownload(context);
                        okHttpClient.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                onErrorDownload(context);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String result = response.body().string();
                                Request request = new Request.Builder().url(JsonUtil.getJsonValue(result, "data.feeds[0].video_url")).build();
                                okHttpClient.newCall(request).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        onErrorDownload(context);
                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) {
                                        InputStream is = null;
                                        byte[] buf = new byte[2048];
                                        int len = 0;
                                        FileOutputStream fos = null;
                                        String FILE_NAME = UUID.randomUUID().toString().replaceAll("-", "");
                                        File file = new File(FILE_PATH + "/WeiShiDL/" + FILE_NAME + ".mp4");
                                        if (!file.getParentFile().exists()) {
                                            file.getParentFile().mkdirs();
                                        }
                                        try {
                                            is = response.body().byteStream();
                                            long total = response.body().contentLength();
                                            fos = new FileOutputStream(file);
                                            long sum = 0;
                                            while ((len = is.read(buf)) != -1) {
                                                fos.write(buf, 0, len);
                                                sum += len;
                                                int progress = (int) (sum * 1.0f / total * 100);
                                                onDownloading(context, progress);
                                            }
                                            fos.flush();
                                            onFinishDownload(context, file.getAbsolutePath());
                                        } catch (Exception e) {
                                            onErrorDownload(context);
                                            e.printStackTrace();
                                        } finally {
                                            try {
                                                if (is != null) {
                                                    is.close();
                                                }
                                                if (fos != null) {
                                                    fos.close();
                                                }
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                });
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    onErrorDownload(context);
                }
            } else if (url.contains("zuiyou")) {
                try {
                    matcher = Pattern.compile("\\d{9}").matcher(url);
                    if (matcher.find()) {
                        String pid = matcher.group(0);
                        url = "https://share.izuiyou.com/api/post/detail";
                        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{\"pid\":" + pid + "}");
                        Request request = new Request.Builder().url(url).post(body).build();
                        onStartDownload(context);
                        okHttpClient.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                onErrorDownload(context);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String result = response.body().string();
                                Request request = new Request.Builder().url(JsonUtil.getJsonValue(result, "data.post.videos." + JsonUtil.getJsonValue(result, "data.post.imgs[0].id") + ".url")).build();
                                okHttpClient.newCall(request).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        onErrorDownload(context);
                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) {

                                        InputStream is = null;
                                        byte[] buf = new byte[2048];
                                        int len = 0;
                                        FileOutputStream fos = null;
                                        String FILE_NAME = UUID.randomUUID().toString().replaceAll("-", "");
                                        File file = new File(FILE_PATH + "/ZuiYouDL/" + FILE_NAME + ".mp4");
                                        if (!file.getParentFile().exists()) {
                                            file.getParentFile().mkdirs();
                                        }
                                        try {
                                            is = response.body().byteStream();
                                            long total = response.body().contentLength();
                                            fos = new FileOutputStream(file);
                                            long sum = 0;
                                            while ((len = is.read(buf)) != -1) {
                                                fos.write(buf, 0, len);
                                                sum += len;
                                                int progress = (int) (sum * 1.0f / total * 100);
                                                onDownloading(context, progress);
                                            }
                                            fos.flush();
                                            onFinishDownload(context, file.getAbsolutePath());
                                        } catch (Exception e) {
                                            onErrorDownload(context);
                                            e.printStackTrace();
                                        } finally {
                                            try {
                                                if (is != null) {
                                                    is.close();
                                                }
                                                if (fos != null) {
                                                    fos.close();
                                                }
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                });
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    onErrorDownload(context);
                }
            } else if (url.contains("pipix")) {
                try {
                    onStartDownload(context);
                    Request request = new Request.Builder().url(url).build();
                    okHttpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            onErrorDownload(context);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String url = response.request().url().toString();
                            Matcher matcher = Pattern.compile("\\d{19}").matcher(url);
                            if (matcher.find()) {
                                String itemId = matcher.group(0);
                                url = "https://is.snssdk.com/bds/item/detail/?app_name=super&aid=1319&item_id="
                                        + itemId;
                                Request request = new Request.Builder().url(url).build();
                                okHttpClient.newCall(request).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        onErrorDownload(context);
                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        String result = response.body().string();
                                        String url = JsonUtil.getJsonValue(result,
                                                "data.data.video.video_fallback.url_list[0].url");
                                        Request request = new Request.Builder().url(url).build();
                                        okHttpClient.newCall(request).enqueue(new Callback() {
                                            @Override
                                            public void onFailure(Call call, IOException e) {
                                                onErrorDownload(context);
                                            }

                                            @Override
                                            public void onResponse(Call call, Response response) throws IOException {
                                                InputStream is = null;
                                                byte[] buf = new byte[2048];
                                                int len = 0;
                                                FileOutputStream fos = null;
                                                String FILE_NAME = UUID.randomUUID().toString().replaceAll("-", "");
                                                File file = new File(FILE_PATH + "/PiPiXiaDL/" + FILE_NAME + ".mp4");
                                                if (!file.getParentFile().exists()) {
                                                    file.getParentFile().mkdirs();
                                                }
                                                try {
                                                    is = response.body().byteStream();
                                                    long total = response.body().contentLength();
                                                    fos = new FileOutputStream(file);
                                                    long sum = 0;
                                                    while ((len = is.read(buf)) != -1) {
                                                        fos.write(buf, 0, len);
                                                        sum += len;
                                                        int progress = (int) (sum * 1.0f / total * 100);
                                                        onDownloading(context, progress);
                                                    }
                                                    fos.flush();
                                                    onFinishDownload(context, file.getAbsolutePath());
                                                } catch (Exception e) {
                                                    onErrorDownload(context);
                                                    e.printStackTrace();
                                                } finally {
                                                    try {
                                                        if (is != null) {
                                                            is.close();
                                                        }
                                                        if (fos != null) {
                                                            fos.close();
                                                        }
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    onErrorDownload(context);
                }
            } else if (url.contains("chenzhongtech") || url.contains("gifshow") || url.contains("kuaishou")) {
                try {
                    String srcStr = "client_key=3c2cd3f3&shareText=" + url;
                    Map<String, String> map = SingatureUtil.getMapFromStr(srcStr);
                    map.put("sig", SingatureUtil.genSignature(map));
                    url = "http://api.gifshow.com/rest/n/tokenShare/info/byText";
                    RequestBody body = new FormBody.Builder()
                            .add("client_key", map.get("client_key"))
                            .add("shareText", map.get("shareText"))
                            .add("sig", map.get("sig")).build();
                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader(
                                    "User-Agent",
                                    "Mozilla/5.0 (iPhone; CPU iPhone OS 12_1_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/16D57 Version/12.0 Safari/604.1")
                            .post(body).build();
                    onStartDownload(context);
                    okHttpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            onErrorDownload(context);
                            Log.e("error", e.getMessage());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String result = response.body().string();
                            Request request = new Request.Builder().url(JsonUtil.getJsonValue(result,
                                    "shareTokenDialog.feed.main_mv_url")).build();
                            okHttpClient.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    onErrorDownload(context);
                                    Log.e("error", e.getMessage());
                                }

                                @Override
                                public void onResponse(Call call, Response response) {
                                    InputStream is = null;
                                    byte[] buf = new byte[2048];
                                    int len = 0;
                                    FileOutputStream fos = null;
                                    String FILE_NAME = UUID.randomUUID().toString().replaceAll("-", "");
                                    File file = new File(FILE_PATH + "/KuaiShouDL/" + FILE_NAME + ".mp4");
                                    if (!file.getParentFile().exists()) {
                                        file.getParentFile().mkdirs();
                                    }
                                    try {
                                        is = response.body().byteStream();
                                        long total = response.body().contentLength();
                                        fos = new FileOutputStream(file);
                                        long sum = 0;
                                        while ((len = is.read(buf)) != -1) {
                                            fos.write(buf, 0, len);
                                            sum += len;
                                            int progress = (int) (sum * 1.0f / total * 100);
                                            onDownloading(context, progress);
                                        }
                                        fos.flush();
                                        onFinishDownload(context, file.getAbsolutePath());
                                    } catch (Exception e) {
                                        onErrorDownload(context);
                                        e.printStackTrace();
                                    } finally {
                                        try {
                                            if (is != null) {
                                                is.close();
                                            }
                                            if (fos != null) {
                                                fos.close();
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    onErrorDownload(context);
                    Log.e("error", e.getMessage());
                }
            }
        } else {
            Toast.makeText(context, "请复制正确的视频链接", Toast.LENGTH_SHORT).show();
        }
    }

}
