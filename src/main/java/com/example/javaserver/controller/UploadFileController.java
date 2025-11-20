package com.example.javaserver.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.javaserver.model.dto.RequestFileInfoDTO;
import com.example.javaserver.model.vo.BaseVo;
import com.example.javaserver.model.vo.FileInfoVo;
import com.example.javaserver.service.UploadFileService;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/file")
public class UploadFileController {
    @Autowired
    UploadFileService uploadFileService;

     // 分片配置
     @Value("${file.chunk-dir:./data/chunks/temp}")
     private String chunkDir;

    // 合并配置 
    @Value("${file.chunk-merge-dir:./data/chunks/merging}")
    private String chunkMergeDir;

    // 最终存储配置
    @Value("${file.upload-dir:./data/uploads}")
    private String uploadDir;
    
    @Value("${file.upload-image-dir:./data/uploads/images}")
    private String uploadImageDir;
    
    @Value("${file.upload-document-dir:./data/uploads/documents}")
    private String uploadDocumentDir;
    

    /**
     * 上传单个分片文件。
     * 当所有分片上传完成后，系统会自动触发合并。
     * 
     * @param file        分片文件
     * @param chunkNumber 当前分片编号（从1开始）
     * @param totalChunks 总分片数
     * @param fileName    文件名（可能包含分片信息，如 "file.mp4.chunk1"）
     * @return 上传结果
     */
    @PostMapping("upload")
    public BaseVo<Object> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam("chunkNumber") int chunkNumber,
        @RequestParam("totalChunks") int totalChunks,
        @RequestParam("fileName") String fileName,
        @RequestHeader("Authorization") String authorization
    ) {
        try{
            uploadFileService.saveChunk(file, chunkNumber, fileName, totalChunks, authorization);
            return BaseVo.success(null);
        }catch(IOException err){
            System.out.println("文件上传失败" + err);
            return BaseVo.error("文件上传失败", null);
        }
    }

    @PostMapping("getUploadFileList")
    public BaseVo<List<FileInfoVo>> getUploadFileList(@RequestBody RequestFileInfoDTO requestParams) {
        return uploadFileService.getUploadFileList(requestParams.getUserId());
    }    
}
