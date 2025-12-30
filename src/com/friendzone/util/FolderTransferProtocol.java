package com.friendzone.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Protocol cho việc gửi thư mục với cấu trúc file/folder hoàn chỉnh
 */
public class FolderTransferProtocol {
    
    /**
     * Quét toàn bộ thư mục và trả về danh sách files với relative path
     */
    public static List<FileEntry> scanFolder(File folder) throws IOException {
        List<FileEntry> entries = new ArrayList<>();
        Path basePath = folder.toPath();
        
        try (Stream<Path> paths = Files.walk(basePath)) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     String relativePath = basePath.relativize(path).toString();
                     long size = path.toFile().length();
                     entries.add(new FileEntry(path.toFile(), relativePath, size));
                 });
        }
        
        return entries;
    }
    
    /**
     * Tính tổng kích thước của thư mục
     */
    public static long calculateFolderSize(File folder) throws IOException {
        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            return paths.filter(Files::isRegularFile)
                       .mapToLong(p -> p.toFile().length())
                       .sum();
        }
    }
    
    /**
     * Đếm số file trong thư mục
     */
    public static int countFiles(File folder) throws IOException {
        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            return (int) paths.filter(Files::isRegularFile).count();
        }
    }
    
    /**
     * Tạo FOLDER_START message
     */
    public static Map<String, String> createFolderStartMessage(
            String transferId, String folderName, int totalFiles, long totalSize) {
        Map<String, String> data = new HashMap<>();
        data.put("transferId", transferId);
        data.put("folderName", folderName);
        data.put("totalFiles", String.valueOf(totalFiles));
        data.put("totalSize", String.valueOf(totalSize));
        return data;
    }
    
    /**
     * Tạo message cho từng file trong folder
     */
    public static Map<String, String> createFileInFolderMessage(
            String transferId, String relativePath, int fileIndex, int totalFiles) {
        Map<String, String> data = new HashMap<>();
        data.put("transferId", transferId);
        data.put("relativePath", relativePath);
        data.put("fileIndex", String.valueOf(fileIndex));
        data.put("totalFiles", String.valueOf(totalFiles));
        return data;
    }
    
    /**
     * Tạo FOLDER_COMPLETE message
     */
    public static Map<String, String> createFolderCompleteMessage(
            String transferId, int totalFiles, long totalSize) {
        Map<String, String> data = new HashMap<>();
        data.put("transferId", transferId);
        data.put("totalFiles", String.valueOf(totalFiles));
        data.put("totalSize", String.valueOf(totalSize));
        return data;
    }
    
    /**
     * Recreate folder structure and save file
     */
    public static File saveFileInFolder(File baseFolder, String relativePath, byte[] fileData) 
            throws IOException {
        Path targetPath = Paths.get(baseFolder.getAbsolutePath(), relativePath);
        
        // Create parent directories if needed
        Files.createDirectories(targetPath.getParent());
        
        // Write file
        Files.write(targetPath, fileData);
        
        return targetPath.toFile();
    }
    
    /**
     * Entry cho mỗi file trong folder
     */
    public static class FileEntry {
        public final File file;
        public final String relativePath;
        public final long size;
        
        public FileEntry(File file, String relativePath, long size) {
            this.file = file;
            this.relativePath = relativePath;
            this.size = size;
        }
    }
}
