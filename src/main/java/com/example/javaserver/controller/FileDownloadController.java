package com.example.javaserver.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 文件下载控制器
 *
 * <p>负责将上传目录中的文件以附件形式返回给前端。支持任意层级的子目录，并在解析路径时
 * 做了安全校验，避免访问到上传目录之外的文件。</p>
 */
@RestController
public class FileDownloadController {

    /** 上传根目录的绝对路径，例如 ./data/uploads */
    private final Path uploadRoot;
    /** 用来从 /files/** 这种通配路径里提取真实文件路径的工具 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public FileDownloadController(@Value("${file.upload-dir:./data/uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @GetMapping("/files/**")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) throws IOException {
        // 1. 先解析请求中匹配到的相对路径，例如 documents/xxx.pdf
        String relativePath = resolveRelativePath(request);
        if (relativePath == null || relativePath.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        // 2. 拼接成绝对路径，并做安全检查（禁止跳出上传目录）
        Path filePath = uploadRoot.resolve(relativePath).normalize();
        if (!filePath.startsWith(uploadRoot) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            return ResponseEntity.notFound().build();
        }

        // 3. 读取文件作为 Resource，并猜测内容类型
        Resource resource = toResource(filePath);
        String contentType = Files.probeContentType(filePath);
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        // 4. 设置 Content-Disposition 让浏览器触发“下载”而不是直接预览
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(filePath.getFileName().toString(), StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(MediaType.parseMediaType(contentType))
            .contentLength(Files.size(filePath))
            .body(resource);
    }

    /** 将文件路径转换成 Spring 可识别的 UrlResource */
    private Resource toResource(Path filePath) throws MalformedURLException {
        return new UrlResource(filePath.toUri());
    }

    /**
     * 从 Servlet 请求里取出被 /files/** 匹配到的真实相对路径，并对 URL 编码的字符进行解码。
     *
     * <p>示例：访问 /files/documents/%E5%89%8D%E7%AB%AF.pdf 时，这个方法会返回
     * documents/前端.pdf</p>
     */
    private String resolveRelativePath(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        Object pathWithinMapping = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        if (!(pattern instanceof String) || !(pathWithinMapping instanceof String)) {
            return null;
        }

        String extracted = pathMatcher.extractPathWithinPattern((String) pattern, (String) pathWithinMapping);
        if (extracted == null || extracted.isBlank()) {
            return null;
        }

        try {
            String decoded = URLDecoder.decode(extracted, StandardCharsets.UTF_8);
            return decoded.replace("\\", "/");
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

