package cn.qianlan.klinedemo;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OkHttp的工具类
 */
public class OkHttpUtil {

    private static OkHttpClient okHttpClient;
    private static Handler handler = new Handler();

    public static void initOkHttp() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.MINUTES)
                .readTimeout(60, TimeUnit.MINUTES)
                .writeTimeout(60, TimeUnit.MINUTES)
                .retryOnConnectionFailure(false);

        //处理https协议
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }}, new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
            sc = null;
        }
        if (sc != null) {
            okHttpClient = builder.hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    })
                    .sslSocketFactory(sc.getSocketFactory())
                    .build();
        } else {
            okHttpClient = builder.build();
        }
    }

    /**
     * 取消所有请求
     */
    public static void cancel() {
        okHttpClient.dispatcher().cancelAll();
    }

    /**
     * 下载json
     */
    public static void getJSON(String url, OnDataListener dataListener) {
        if (!TextUtils.isEmpty(url)) {
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            okHttpClient.newCall(request).enqueue(new OkHttpCallback(url, dataListener));
        }
    }

    private static void postJson(String url, RequestBody body, OnDataListener dataListener) {
        if (!TextUtils.isEmpty(url)) {
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            okHttpClient.newCall(request).enqueue(new OkHttpCallback(url, dataListener));
        }
    }


    /**
     * 结果回调
     */
    static class OkHttpCallback implements Callback {

        private String url;
        private OnDataListener dataListener;

        public OkHttpCallback(String url, OnDataListener dataListener) {
            this.url = url;
            this.dataListener = dataListener;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            onFail(dataListener, url, e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            onResp(dataListener, url, response.body().string());
        }
    }

    private static void onFail(final OnDataListener dataListener, final String url, final IOException e) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (dataListener != null) {
                        Log.e("loge", "onFailure: " + url + "\n" + e.getMessage());
                        dataListener.onFailure(url, e.getMessage());
                    }
                } catch (Exception e) {
                    Log.e("loge", "E: " + e.getMessage());
                }
            }
        });
    }

    private static void onResp(final OnDataListener dataListener, final String url, final String json) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (dataListener != null && !TextUtils.isEmpty(json)) {
                        dataListener.onResponse(url, json);
                    }
                } catch (Exception e) {
                    Log.e("loge", json + "\nE: " + e.getMessage());
                    if (dataListener != null) {
                        dataListener.onFailure(url, e.getMessage());
                    }
                }
            }
        });
    }

    public interface OnDataListener {
        void onResponse(String url, String json);
        void onFailure(String url, String error);
    }
}
