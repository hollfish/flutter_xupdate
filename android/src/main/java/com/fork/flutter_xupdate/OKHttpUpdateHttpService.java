/*
 * Copyright (C) 2018 forkjys(forkjys@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fork.flutter_xupdate;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.fork.xupdate.logs.UpdateLog;
import com.fork.xupdate.proxy.IUpdateHttpService;
import com.fork.xupdate.utils.UpdateUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;
import com.zhy.http.okhttp.callback.StringCallback;
import com.zhy.http.okhttp.request.RequestCall;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * 使用okhttp
 *
 * @author fork
 * @since 2018/7/10 下午4:04
 */
public class OKHttpUpdateHttpService implements IUpdateHttpService {

    private boolean mIsPostJson;

    public OKHttpUpdateHttpService(boolean isPostJson) {
        this(20000, isPostJson);
    }

    /**
     * 构造方法
     *
     * @param timeout    请求超时响应时间
     * @param isPostJson 是否使用json
     */
    public OKHttpUpdateHttpService(int timeout, boolean isPostJson) {
        mIsPostJson = isPostJson;
        OkHttpUtils.initClient(new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .build());
        UpdateLog.d("设置请求超时响应时间:" + timeout + "ms, 是否使用json:" + isPostJson);
    }

    @Override
    public void asyncGet(@NonNull String url, @NonNull Map<String, Object> params, final @NonNull IUpdateHttpService.Callback callBack) {
        OkHttpUtils.get()
                .url(url)
                .params(transform(params))
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        callBack.onError(e);
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        callBack.onSuccess(response);
                    }
                });
    }

    @Override
    public void asyncPost(@NonNull String url, @NonNull Map<String, Object> params, final @NonNull IUpdateHttpService.Callback callBack) {
        //这里默认post的是Form格式，使用json格式的请修改 post -> postString
        RequestCall requestCall;
        if (mIsPostJson) {
            requestCall = OkHttpUtils.postString()
                    .url(url)
                    .content(new Gson().toJson(params))
                    .mediaType(MediaType.parse("application/json; charset=utf-8"))
                    .build();
        } else {
            requestCall = OkHttpUtils.post()
                    .url(url)
                    .params(transform(params))
                    .build();
        }
        requestCall
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        callBack.onError(e);
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        callBack.onSuccess(response);
                    }
                });
    }

    @Override
    public void download(@NonNull String url, @NonNull String path, @NonNull String fileName, final @NonNull IUpdateHttpService.DownloadCallback callback) {
        OkHttpUtils.get()
                .url(url)
                .tag(url)
                .build()
                .execute(new FileCallBack(path, fileName) {
                    @Override
                    public void inProgress(float progress, long total, int id) {
                        callback.onProgress(progress, total);
                    }

                    @Override
                    public void onError(Call call, Exception e, int id) {
                        callback.onError(e);
                    }

                    @Override
                    public void onResponse(File response, int id) {
                        callback.onSuccess(response);
                    }

                    @Override
                    public void onBefore(Request request, int id) {
                        super.onBefore(request, id);
                        callback.onStart();
                    }
                });
    }

    @Override
    public void cancelDownload(@NonNull String url) {
        OkHttpUtils.getInstance().cancelTag(url);
    }

    private Map<String, String> transform(Map<String, Object> params) {
        Map<String, String> map = new TreeMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toString());
        }
        return map;
    }

}