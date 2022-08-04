package com.socket.webchat.controller;

import cn.hutool.core.util.StrUtil;
import com.socket.secure.filter.anno.Encrypted;
import com.socket.webchat.model.enums.FilePath;
import com.socket.webchat.model.enums.HttpStatus;
import com.socket.webchat.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @date 2022/6/17
 */
@RestController
@RequestMapping(UploadService.MAPPING)
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;

    @Encrypted
    @PostMapping("/audio")
    public HttpStatus uploadAudio(MultipartFile audio, String mid) throws IOException {
        uploadService.upload(audio, FilePath.AUDIO, mid);
        return HttpStatus.SUCCESS.body();
    }

    @Encrypted
    @PostMapping("/image")
    public HttpStatus uploadImage(MultipartFile image, String mid) throws IOException {
        String path = uploadService.upload(image, FilePath.IMAGE, mid);
        return HttpStatus.SUCCESS.body(StrUtil.isEmpty(path) ? null : path);
    }

    @Encrypted
    @PostMapping("/blob")
    public HttpStatus uploadBlob(MultipartFile blob, String mid) throws IOException {
        uploadService.upload(blob, FilePath.BLOB, mid);
        return HttpStatus.SUCCESS.body();
    }

    @GetMapping("/{mid}")
    public ResponseEntity<Resource> mappingFile(@PathVariable String mid, HttpServletResponse response) throws IOException {
        OutputStream stream = uploadService.writeStream(mid, response.getOutputStream());
        if (stream == null) {
            return ResponseEntity.notFound().build();
        }
        return null;
    }

    @GetMapping("/image/{hash}")
    public ResponseEntity<Resource> mappingImage(@PathVariable String hash, HttpServletResponse response) throws IOException {
        OutputStream stream = uploadService.writeStream(FilePath.IMAGE, hash, response.getOutputStream());
        if (stream == null) {
            return ResponseEntity.notFound().build();
        }
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
