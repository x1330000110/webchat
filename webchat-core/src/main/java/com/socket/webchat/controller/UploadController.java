package com.socket.webchat.controller;

import cn.hutool.core.util.StrUtil;
import com.socket.webchat.model.condition.FileCondition;
import com.socket.webchat.model.enums.FileType;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @date 2022/6/17
 */
@RestController
@RequestMapping(UploadService.MAPPING)
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;

    @PostMapping("/audio")
    public HttpStatus uploadAudio(FileCondition condition) throws IOException {
        uploadService.upload(condition, FileType.AUDIO);
        return HttpStatus.SUCCESS.body();
    }

    @PostMapping("/image")
    public HttpStatus uploadImage(FileCondition condition) throws IOException {
        String path = uploadService.upload(condition, FileType.IMAGE);
        return HttpStatus.SUCCESS.body(StrUtil.isEmpty(path) ? null : path);
    }

    @PostMapping("/blob")
    public HttpStatus uploadBlob(FileCondition condition) throws IOException {
        uploadService.upload(condition, FileType.BLOB);
        return HttpStatus.SUCCESS.body();
    }

    @GetMapping("/{mid}")
    public ResponseEntity<Object> mappingFile(@PathVariable String mid, HttpServletResponse response) throws IOException {
        String url = uploadService.getResourceURL(mid);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        response.sendRedirect(url);
        return null;
    }

    @GetMapping("/image/{hash}")
    public ResponseEntity<Resource> mappingImage(@PathVariable String hash, HttpServletResponse response) throws IOException {
        String url = uploadService.getResourceURL(FileType.IMAGE, hash);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        response.sendRedirect(url);
        return null;
    }

    @GetMapping("/convert/{mid}")
    public HttpStatus convertText(@PathVariable String mid) {
        String text = uploadService.convertText(mid);
        if (text == null) {
            return HttpStatus.FAILURE.message("找不到指定记录");
        }
        return HttpStatus.SUCCESS.body(text);
    }
}
