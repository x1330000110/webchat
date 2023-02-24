package com.socket.webchat.request;

import cn.hutool.core.io.resource.BytesResource;
import cn.hutool.core.io.resource.Resource;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.socket.secure.util.Assert;
import com.socket.webchat.constant.properties.LanzouProperties;
import com.socket.webchat.custom.storage.ResourceStorage;
import com.socket.webchat.model.enums.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

/**
 * 蓝奏云API（实验性）
 */
@Slf4j
@RequiredArgsConstructor
public class LanzouCloudRequest implements ResourceStorage, InitializingBean {
    private static final String DOWNLOAD_URL = "https://api.kit9.cn/api/lanzouyun_netdisc/api.php?link={}";
    private static final String UPLOAD_URL = "https://pc.woozooo.com/fileup.php";
    private final LanzouProperties properties;
    private List<HttpCookie> cookies;

    public String upload(FileType type, byte[] bytes, String hash) {
        return upload(type, new BytesResource(bytes, hash));
    }

    @Override
    public byte[] download(String url) {
        return HttpRequest.get(url)
                .header(Header.USER_AGENT, USER_AGENT)
                .execute()
                .bodyBytes();
    }

    public String getOriginalURL(String lanzouURL) {
        String url = StrUtil.format(DOWNLOAD_URL, lanzouURL);
        HttpResponse execute = HttpRequest.get(url).header(Header.USER_AGENT, USER_AGENT).execute();
        String body = execute.body();
        JSONObject json = JSONUtil.parseObj(body);
        if (json.getInt("code") != 200) {
            return null;
        }
        return json.getJSONObject("data").getStr("download_link");
    }

    private String upload(FileType type, Resource resource) {
        JSONObject args = buildArgs(resource, type);
        String body = HttpRequest.post(UPLOAD_URL)
                .header(Header.USER_AGENT, USER_AGENT)
                .cookie(cookies)
                .form(args)
                .execute()
                .body();
        // 解析结果
        JSONObject json = JSONUtil.parseObj(body);
        Integer zt = json.getInt("zt");
        // 登录信息过期
        Assert.isTrue(zt == 1, "文件上传服务暂时关闭", IllegalStateException::new);
        JSONObject text = json.getJSONArray("text").getJSONObject(0);
        return text.getStr("is_newd") + "/" + text.getStr("f_id");
    }

    private JSONObject buildArgs(Resource resource, FileType type) {
        JSONObject form = new JSONObject();
        form.set("ve", 2);
        form.set("task", 1);
        form.set("name", resource.getName());
        form.set("upload_file", resource);
        form.set("folder_id_bb_n", type.getCode());
        form.set("type", "application/octet-stream");
        form.set("size", resource.readBytes().length);
        form.set("id", "WU_FILE_" + RandomUtil.randomInt(10));
        return form;
    }

    @Override
    public void afterPropertiesSet() {
        List<HttpCookie> list = new ArrayList<>();
        list.add(new HttpCookie("ylogin", properties.getYlogin()));
        list.add(new HttpCookie("phpdisk_info", properties.getPhpdiskInfo()));
        this.cookies = list;
    }
}
