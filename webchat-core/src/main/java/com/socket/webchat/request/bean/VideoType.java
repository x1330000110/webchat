package com.socket.webchat.request.bean;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum VideoType {
    VIP_VIDEO("https://api.leafone.cn/api/jx?url={}"),
    SHORT_VIDEO("https://api.leafone.cn/api/dsp?url={}");

    private final String url;
    private final String key;

    VideoType(String url) {
        this.key = name().toLowerCase();
        this.url = url;
    }

    public static VideoType of(String type) {
        return Arrays.stream(values()).filter(e -> e.key.equalsIgnoreCase(type)).findFirst().orElse(null);
    }
}