package com.example.javaserver.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.javaserver.model.entity.FileInfo;
import com.example.javaserver.model.vo.BaseVo;
import com.example.javaserver.model.vo.FileInfoVo;
import com.example.javaserver.respository.FileRespository;
import com.example.javaserver.utils.JwtUtils;

// 加上这个注解，说明把这个类交给springboot管理
@Service
public class UploadFileService {
    // 生成唯一文件ID
    // String fileId = UUID.randomUUID().toString().replace("-", "");

    // 分片配置
    @Value("${file.chunk-dir:./data/chunks/temp}")
    private String tempDir;

    // 合并输出目录（用于生成最终文件前的临时合并文件）
    @Value("${file.chunk-merge-dir:./data/chunks/merging}")
    private String chunkMergeDir;

    // 上传完成后落库的根目录
    @Value("${file.upload-dir:./data/uploads}")
    private String uploadDir;

    @Value("${file.public-base-url:}")
    private String filePublicBaseUrl;

    // 图片、文档等类型化目录，便于按需归档
    @Value("${file.upload-image-dir:./data/uploads/images}")
    private String uploadImageDir;

    @Value("${file.upload-document-dir:./data/uploads/documents}")
    private String uploadDocumentDir;

    @Value("${file.upload-video-dir:./data/uploads/video}")
    private String uploadVideoDir;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private FileRespository fileRepository;

    String curUserToken;

    /**
     * 记录每个文件的合并锁，避免并发重复合并导致的文件占用异常
     */
    private final ConcurrentMap<String, ReentrantLock> mergeLocks = new ConcurrentHashMap<>(); 

    /*
     * 通过userId获取指定用户上传附件列表
     */
    public BaseVo<List<FileInfoVo>> getUploadFileList(int userId){
        if (userId <= 0) {
            return BaseVo.error("用户ID不合法", Collections.emptyList());
        }

        try {
            List<FileInfo> fileInfoList = fileRepository.getFileList(userId);
            System.out.println("fileInfoList" + fileInfoList);
            List<FileInfoVo> fileInfoVos = fileInfoList == null
                ? Collections.emptyList()
                : fileInfoList.stream()
                    .filter(Objects::nonNull)
                    .map(this::convertToFileInfoVo)
                    .collect(Collectors.toList());
            return BaseVo.success(fileInfoVos);
        } catch (Exception e) {
            System.out.println(e);
            return BaseVo.error("获取附件列表失败", Collections.emptyList());
        }
    }

    private FileInfoVo convertToFileInfoVo(FileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        FileInfoVo vo = new FileInfoVo();
        vo.setOriginalName(fileInfo.getOriginalName());
        vo.setUrl(buildFileUrl(fileInfo.getStoragePath()));
        return vo;
    }

    private String buildFileUrl(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return "";
        }
        String normalizedPath = storagePath.startsWith("/") ? storagePath : "/" + storagePath;
        String filePrefix = "/files";
        if (filePublicBaseUrl == null || filePublicBaseUrl.isBlank()) {
            return filePrefix + normalizedPath;
        }
        String normalizedBase = filePublicBaseUrl.endsWith("/")
            ? filePublicBaseUrl.substring(0, filePublicBaseUrl.length() - 1)
            : filePublicBaseUrl;
        return normalizedBase + filePrefix + normalizedPath;
    }

    /**
     * 保存单个分片文件。
     * 注意：如果前端传入的 fileName 包含分片信息（如 .chunk1），会自动提取真正的文件名。
     * 
     * @param file        分片文件
     * @param chunkNumber 当前分片编号（从1开始）
     * @param fileName    文件名（可能包含分片信息，如 "file.mp4.chunk1"）
     * @param totalChunks 总分片数
     * @throws IOException 文件保存失败时抛出
     */
    public void saveChunk(MultipartFile file, int chunkNumber, String fileName, int totalChunks, String authorization) throws IOException{
        curUserToken = authorization;
        
        // 1. 提取真正的文件名（去除可能存在的分片后缀，如 .chunk1, .chunk2 等）
        String realFileName = extractRealFileName(fileName);
        
        // 2. 构造分片临时目录，所有分片都保存在同一个目录下
        // 例如：data/chunks/temp/b8ef49578fc7ab1e237056600e43feb1.mp4
        Path chunkDirPath = Paths.get(tempDir).toAbsolutePath().resolve(realFileName);
        Files.createDirectories(chunkDirPath);

        // 3. 构造当前分片文件路径，命名格式：chunk-1、chunk-2 ...
        Path chunkPath = chunkDirPath.resolve("chunk-" + chunkNumber);
        File chunkFile = chunkPath.toFile();

        // 4. 断点续传：如果已经存在且大小一致，直接跳过，避免重复上传
        if (chunkFile.exists() && chunkFile.length() == file.getSize()) {
            // 检查是否所有分片都已上传完成，如果是则自动合并
            checkAndMergeIfComplete(realFileName, totalChunks);
            return;
        }

        // 5. 覆盖写入（如果之前不完整，先删掉再写）
        if (chunkFile.exists() && !chunkFile.delete()) {
            throw new IOException("无法覆盖分片文件：" + chunkFile.getAbsolutePath());
        }

        // 6. 保存当前分片
        file.transferTo(chunkFile);

        // 7. 检查是否所有分片都已上传完成，如果是则自动合并
        checkAndMergeIfComplete(realFileName, totalChunks);
    }

    /**
     * 检查所有分片是否都已上传完成，如果完成则自动触发合并。
     * 这是一个内部方法，用于在每次分片上传后检查是否可以进行合并。
     * 
     * @param realFileName 真正的文件名（不含分片信息）
     * @param totalChunks  总分片数
     */
    private void checkAndMergeIfComplete(String realFileName, int totalChunks) {
        try {
            Path chunkDirPath = Paths.get(tempDir).toAbsolutePath().resolve(realFileName);
            if (!Files.exists(chunkDirPath) || !Files.isDirectory(chunkDirPath)) {
                return; // 目录不存在，说明还没有分片上传
            }

            for (int i = 1; i <= totalChunks; i++) {
                Path chunkPath = chunkDirPath.resolve("chunk-" + i);
                if (!Files.exists(chunkPath)) {
                    return;
                }
            }

            mergeChunks(realFileName, totalChunks);
        } catch (IOException e) {
            // 合并失败不影响分片保存，只记录错误，后续可以手动触发合并
            System.err.println("自动合并文件失败：" + realFileName + ", 错误：" + e.getMessage());
        }
    }

    /**
     * 合并指定文件名的所有分片，并将最终文件移动到对应的业务目录下。
     * 
     * @param fileName    原始文件名（含扩展名，如果包含分片信息会自动提取）
     * @param totalChunks 前端告知的分片总数，用于校验分片完整性
     * @return            最终文件的绝对路径，方便后续业务层落库或返回给前端
     * @throws IOException 合并失败时抛出异常
     */
    public Path mergeChunks(String fileName, int totalChunks) throws IOException {
        if (totalChunks <= 0) {
            throw new IllegalArgumentException("totalChunks 必须大于 0");
        }

        // 1. 提取真正的文件名（去除可能存在的分片后缀）
        String realFileName = extractRealFileName(fileName);

        // 2. 获取并锁定当前文件的合并锁，防止并发重复合并
        ReentrantLock lock = mergeLocks.computeIfAbsent(realFileName, key -> new ReentrantLock());
        lock.lock();
        try {
            return doMergeChunks(realFileName, totalChunks);
        } finally {
            lock.unlock();
            mergeLocks.remove(realFileName, lock);
        }
    }

    /**
     * 真正执行文件合并的内部方法，调用之前需确保已经加锁
     */
    private Path doMergeChunks(String realFileName, int totalChunks) throws IOException {
        Path chunkDirPath = Paths.get(tempDir).toAbsolutePath().resolve(realFileName);
        Path destinationDir = resolveDestinationDirectory(realFileName).toAbsolutePath();
        Path finalFilePath = destinationDir.resolve(realFileName);

        if (!Files.exists(chunkDirPath) || !Files.isDirectory(chunkDirPath)) {
            // 如果分片目录已经被其他线程清理，但最终文件已存在，则直接返回即可
            if (Files.exists(finalFilePath)) {
                return finalFilePath;
            }
            throw new IOException("未找到分片目录：" + chunkDirPath);
        }

        // 验证所有分片是否都存在
        for (int i = 1; i <= totalChunks; i++) {
            Path chunkPath = chunkDirPath.resolve("chunk-" + i);
            if (!Files.exists(chunkPath)) {
                throw new IOException("缺少第 " + i + " 个分片：" + chunkPath.toAbsolutePath());
            }
        }

        // 创建临时合并文件
        Path mergeDirPath = Paths.get(chunkMergeDir).toAbsolutePath();
        Files.createDirectories(mergeDirPath);
        Path mergingFilePath = mergeDirPath.resolve(realFileName + ".merging");

        try (OutputStream outputStream = new BufferedOutputStream(
                Files.newOutputStream(mergingFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            for (int i = 1; i <= totalChunks; i++) {
                Path chunkPath = chunkDirPath.resolve("chunk-" + i);
                Files.copy(chunkPath, outputStream);
            }
            outputStream.flush();
        } catch (IOException mergeError) {
            Files.deleteIfExists(mergingFilePath);
            throw mergeError;
        }

        Files.createDirectories(destinationDir);
        // 原子移动合并后的文件
        Files.move(mergingFilePath, finalFilePath, StandardCopyOption.REPLACE_EXISTING);

        // 将当前最终文件信息存储到数据库
        saveFinalFilePath(finalFilePath);

        // 清理分片
        deleteDirectoryRecursively(chunkDirPath);

        return finalFilePath;
    }

    /**
     * 从可能包含分片信息的文件名中提取真正的文件名。
     * 例如："file.mp4.chunk1" -> "file.mp4"
     * 
     * @param fileName 可能包含分片信息的文件名
     * @return 提取后的真实文件名
     */
    private String extractRealFileName(String fileName) {
        // 如果文件名包含 .chunk 后跟数字的模式，则提取前面的部分
        // 例如：b8ef49578fc7ab1e237056600e43feb1.mp4.chunk1 -> b8ef49578fc7ab1e237056600e43feb1.mp4
        if (fileName != null && fileName.matches(".*\\.chunk\\d+$")) {
            // 移除 .chunk 及后面的数字部分
            return fileName.replaceAll("\\.chunk\\d+$", "");
        }
        return fileName;
    }

    /**
     * 根据文件扩展名选择落地目录。
     * 可按需扩展更多类型，默认为 uploadDir。
     */
    private Path resolveDestinationDirectory(String fileName) {
        String lowerCaseName = fileName.toLowerCase(Locale.ROOT);
        if (lowerCaseName.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
            return Paths.get(uploadImageDir);
        }
        if (lowerCaseName.matches(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt)$")) {
            return Paths.get(uploadDocumentDir);
        }
        if(lowerCaseName.matches(".*\\.(mp4|avi|mkv|mov)$")){
            return Paths.get(uploadVideoDir);
        }
        return Paths.get(uploadDir);
    }

    /**
     * 递归删除目录，分片合并成功后用于释放磁盘空间。
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /*
     * 将最终文件存入到库中
     */
    private void saveFinalFilePath(Path finalFilePath){
        String curUserId = jwtUtils.getUserId(curUserToken);
        FileInfo fileinfo = new FileInfo();
        fileinfo.setUserId(Integer.parseInt(curUserId));
        fileinfo.setOriginalName(finalFilePath.getFileName().toString());
        fileinfo.setStoragePath(toRelativePath(finalFilePath));
        try {
            fileinfo.setSizeBytes(Math.toIntExact(Files.size(finalFilePath)));
            fileinfo.setContentType(Files.probeContentType(finalFilePath));
        } catch (IOException e) {
            throw new RuntimeException("读取文件属性失败: " + finalFilePath, e);
        }

        // 调用数据层，通过mybatis存表数据
        fileRepository.insertFile(fileinfo);

        System.out.println("当前用户id" + curUserId);
    }

    private String toRelativePath(Path absolutePath) {
        Path normalizedAbsolute = absolutePath.toAbsolutePath().normalize();
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();

        if (normalizedAbsolute.startsWith(basePath)) {
            return "/" + basePath.relativize(normalizedAbsolute).toString().replace("\\", "/");
        }

        Path uploadsRoot = basePath.getParent();
        if (uploadsRoot != null && normalizedAbsolute.startsWith(uploadsRoot)) {
            return "/" + uploadsRoot.relativize(normalizedAbsolute).toString().replace("\\", "/");
        }

        return normalizedAbsolute.toString().replace("\\", "/");
    }
}
