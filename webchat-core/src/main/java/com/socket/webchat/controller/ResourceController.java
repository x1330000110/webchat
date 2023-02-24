package com.socket.webchat.controller;

import com.socket.secure.util.Assert;
import com.socket.webchat.model.condition.FileCondition;
import com.socket.webchat.model.condition.URLCondition;
import com.socket.webchat.model.enums.FileType;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.model.enums.VideoType;
import com.socket.webchat.service.ResourceService;
import com.socket.webchat.util.Enums;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@RestController
@RequestMapping(ResourceService.MAPPING)
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService resourceService;

    @PostMapping("/audio")
    public HttpStatus uploadAudio(FileCondition condition) throws IOException {
        String path = resourceService.upload(condition, FileType.AUDIO);
        return HttpStatus.SUCCESS.body(path);
    }

    @PostMapping("/image")
    public HttpStatus uploadImage(FileCondition condition) throws IOException {
        String path = resourceService.upload(condition, FileType.IMAGE);
        return HttpStatus.SUCCESS.body(path);
    }

    @PostMapping("/blob")
    public HttpStatus uploadBlob(FileCondition condition) throws IOException {
        resourceService.upload(condition, FileType.BLOB);
        return HttpStatus.SUCCESS.body();
    }

    @PostMapping("/resolve")
    public HttpStatus resolve(@RequestBody URLCondition condition) {
        resourceService.saveResolve(condition);
        return HttpStatus.SUCCESS.body();
    }

    @GetMapping("/{mid}")
    public ResponseEntity<Object> mappingFile(@PathVariable String mid, HttpServletResponse response) throws IOException {
        String url = resourceService.getResourceURL(mid);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        response.sendRedirect(url);
        return null;
    }

    @GetMapping("/image/{hash}")
    public ResponseEntity<Resource> mappingImage(@PathVariable String hash, HttpServletResponse response) throws IOException {
        String url = resourceService.getResourceURL(FileType.IMAGE, hash);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        response.sendRedirect(url);
        return null;
    }

    @GetMapping("/convert/{mid}")
    public HttpStatus convertText(@PathVariable String mid) {
        String text = resourceService.convertText(mid);
        if (text == null) {
            return HttpStatus.FAILURE.message("找不到指定记录");
        }
        return HttpStatus.SUCCESS.body(text);
    }

    @GetMapping("/resolveURL")
    public HttpStatus resolveURL(URLCondition condition) {
        String url = condition.getUrl();
        VideoType parse = Enums.of(VideoType.class, condition.getType());
        Assert.notNull(parse, IllegalArgumentException::new);
        String data = parse.getParser().apply(url);
        return url == null ? HttpStatus.FAILURE.body() : HttpStatus.SUCCESS.body(data);
    }
}
