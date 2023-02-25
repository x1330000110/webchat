package com.socket.server.request.enums;

import com.socket.core.util.Enums;
import com.socket.server.request.VideoParseRequest;
import lombok.Getter;

import java.util.function.Function;

@Getter
public enum VideoType {
    VIP_VIDEO(VideoParseRequest::parseVipVideo),
    SHORT_VIDEO(VideoParseRequest::parseShortVideo);

    private final Function<String, String> parser;
    private final String key;

    VideoType(Function<String, String> parser) {
        this.key = Enums.key(this);
        this.parser = parser;
    }

    public String parseURL(String url) {
        return parser.apply(url);
    }
}