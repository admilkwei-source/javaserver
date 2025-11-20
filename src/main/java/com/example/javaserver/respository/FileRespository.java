package com.example.javaserver.respository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.javaserver.model.entity.FileInfo;

@Mapper
// mybatis写法
public interface FileRespository {
    // 文件上传步骤成功，存入附件信息到附件表
    @Insert("""
        INSERT INTO file_info (
            user_id,
            original_name,
            content_type,
            size_bytes,
            storage_path,
            update_time
        ) VALUES (
            #{userId},
            #{originalName},
            #{contentType},
            #{sizeBytes},
            #{storagePath},
            NOW()
        )
        """)
    int insertFile(FileInfo fileInfo);

    @Select("""
        SELECT
            user_id,
            original_name,
            content_type,
            size_bytes,
            storage_path,
            update_time
        FROM file_info
        WHERE user_id = #{userId}
        """)
    List<FileInfo> getFileList(@Param("userId") int userId);
}