package com.example.javaserver.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@TableName("file_info")
@Data 
public class FileInfo {
    private int userId;
    private String originalName;
    private String contentType;
    private int sizeBytes;
    private String storagePath;
    @TableField
    private String updateTime;
}
