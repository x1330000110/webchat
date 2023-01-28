package com.socket.webchat.request;

import cn.hutool.core.collection.ListUtil;
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
import com.socket.webchat.model.enums.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 蓝奏云API（实验性）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LanzouCloudRequest {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36 Edg/106.0.1370.37";
    private static final String DOWNLOAD_URL = "https://api.kit9.cn/api/lanzouyun_netdisc/api.php?link={}";
    private static final String UPLOAD_URL = "https://pc.woozooo.com/fileup.php";
    private final LanzouProperties properties;

    /**
     * 上传文件到lanzou服务器，手动指定文件名
     *
     * @param type  文件类型
     * @param bytes 数据
     * @param hash  散列文件名
     * @return lanzou url
     */
    public String upload(FileType type, byte[] bytes, String hash) {
        return upload(type, new BytesResource(bytes, hash));
    }

    /**
     * 上传文件
     *
     * @param resource 资源文件
     * @return lanzouURL
     */
    public String upload(FileType type, Resource resource) {
        String body = HttpRequest.post(UPLOAD_URL)
                .header(Header.USER_AGENT, USER_AGENT)
                .cookie(getCookies())
                .form(getForm(resource, type))
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

    /**
     * 下载来自蓝奏云服务器的文件
     *
     * @param url 文件url
     * @return 若文件已失效返回null
     */
    public byte[] download(String url) {
        if (url == null) {
            return null;
        }
        return HttpRequest.get(url).header(Header.USER_AGENT, USER_AGENT).execute().bodyBytes();
    }

    /**
     * 获取蓝奏云分享文件的直链接
     *
     * @param lanzouURL 蓝奏云URL
     * @return 直链接
     */
    public String getResourceURL(String lanzouURL) {
        String url = StrUtil.format(DOWNLOAD_URL, lanzouURL);
        HttpResponse execute = HttpRequest.get(url)
                .header(Header.USER_AGENT, USER_AGENT)
                .execute();
        String body = execute.body();
        JSONObject json = JSONUtil.parseObj(body);
        if (json.getInt("code") != 200) {
            return null;
        }
        return json.getJSONObject("data").getStr("download_link");
    }

    private Map<String, Object> getForm(Resource resource, FileType type) {
        Map<String, Object> form = new HashMap<>();
        form.put("task", 1);
        form.put("ve", 2);
        form.put("id", "WU_FILE_" + RandomUtil.randomInt(10));
        form.put("name", resource.getName());
        form.put("type", "application/octet-stream");
        form.put("size", resource.readBytes().length);
        form.put("folder_id_bb_n", type.getCode());
        form.put("upload_file", resource);
        return form;
    }

    private List<HttpCookie> getCookies() {
        return ListUtil.of(
                new HttpCookie("ylogin", properties.getYlogin()),
                new HttpCookie("phpdisk_info", properties.getPhpdiskInfo())
        );
    }
}
