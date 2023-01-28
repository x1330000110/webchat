package com.socket.webchat.request.bean;

import com.socket.webchat.request.VideoParseRequest;
import lombok.Getter;

import java.util.Arrays;
import java.util.function.Function;

@Getter
public enum VideoType {
    VIP_VIDEO(VideoParseRequest::parseVipVideo),
    SHORT_VIDEO(VideoParseRequest::parseShortVideo);

    private final Function<String, String> exec;
    private final String key;

    VideoType(Function<String, String> exec) {
        this.key = name().toLowerCase();
        this.exec = exec;
    }

    public static VideoType of(String type) {
        return Arrays.stream(values())
                .filter(e -> e.key.equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }
}