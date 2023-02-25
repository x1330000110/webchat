package com.socket.client.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.script.JavaScriptEngine;
import com.socket.secure.util.Assert;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 小冰API接口（来自bing）
 *
 * @date 2022/6/13
 */
@Slf4j
@Component
public class BingAPIRequest implements InitializingBean {
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.134 Safari/537.36 Edg/103.0.1264.77";
    private JavaScriptEngine instance;

    /**
     * 发起小冰对话
     */
    @Async
    public ListenableFuture<String> dialogue(String keyword) {
        JSONObject json = new JSONObject();
        JSONObject enc = new JSONObject();
        enc.set("NormalizedQuery", getEncryptString(keyword));
        json.set("query", enc);
        json.set("from", "chatbox");
        HttpResponse execute = HttpRequest.post("https://cn.bing.com/english/zochatv2?cc=cn&ensearch=0")
                .header("user-agent", UA)
                .body(json.toString())
                .execute();
        String body = execute.body();
        try {
            return AsyncResult.forValue(JSONUtil.parseObj(body).getStr("content"));
        } catch (JSONException e) {
            return AsyncResult.forExecutionException(e);
        }
    }

    @SneakyThrows
    private String getEncryptString(String keyword) {
        return (String) instance.eval(StrUtil.format("a('{}','3d9d5f16-5df0-43d7-902e-19274eecdc41',256)", keyword));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("static/bing.min.js");
        Assert.notNull(stream, "找不到BING参数加密文件，无法使用小冰API。", BeanCreationException::new);
        JavaScriptEngine instance = JavaScriptEngine.instance();
        instance.eval(new InputStreamReader(stream));
        this.instance = instance;
    }
}
