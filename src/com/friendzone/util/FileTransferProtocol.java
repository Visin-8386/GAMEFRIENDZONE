package com.friendzone.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Protocol cho việc gửi file lớn với chunks và progress tracking
 */
public class FileTransferProtocol {
    
    // Chunk size: 64KB per chunk (balance giữa speed và overhead)
    public static final int CHUNK_SIZE = 64 * 1024; // 64KB
    
    // Warning threshold: 500MB (hiển thị cảnh báo nhưng vẫn cho phép gửi)
    public static final long WARNING_FILE_SIZE = 500 * 1024 * 1024; // 500MB
    
    /**
     * Tạo transfer ID duy nhất
     */
    public static String generateTransferId(long senderId, long recipientId) {
        return "transfer_" + senderId + "_" + recipientId + "_" + System.currentTimeMillis();
    }
    
    /**
     * Tính số chunks cần thiết cho file
     */
    public static int calculateTotalChunks(long fileSize) {
        return (int) Math.ceil((double) fileSize / CHUNK_SIZE);
    }
    
    /**
     * Đọc chunk từ file
     */
    public static byte[] readChunk(File file, int chunkIndex) throws IOException {
        long offset = (long) chunkIndex * CHUNK_SIZE;
        int length = CHUNK_SIZE;
        
        // Chunk cuối có thể nhỏ hơn
        if (offset + length > file.length()) {
            length = (int) (file.length() - offset);
        }
        
        byte[] buffer = new byte[length];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.skip(offset);
            fis.read(buffer, 0, length);
        }
        
        return buffer;
    }
    
    /**
     * Ghi chunk vào file
     */
    public static void writeChunk(File file, byte[] chunkData, int chunkIndex) throws IOException {
        long offset = (long) chunkIndex * CHUNK_SIZE;
        
        // Tạo file nếu chưa tồn tại
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "rw")) {
            raf.seek(offset);
            raf.write(chunkData);
        }
    }
    
    /**
     * Format file size thành human-readable
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Tạo message START cho file transfer
     */
    public static Map<String, String> createStartMessage(String transferId, long roomId, 
                                                         String fileName, long fileSize) {
        Map<String, String> data = new HashMap<>();
        data.put("transferId", transferId);
        data.put("roomId", String.valueOf(roomId));
        data.put("fileName", fileName);
        data.put("fileSize", String.valueOf(fileSize));
        data.put("totalChunks", String.valueOf(calculateTotalChunks(fileSize)));
        return data;
    }
    
    /**
     * Tạo message CHUNK cho file transfer
     */
    public static Map<String, String> createChunkMessage(String transferId, int chunkIndex, 
                                                         int totalChunks, byte[] chunkData) {
        Map<String, String> data = new HashMap<>();
        data.put("transferId", transferId);
        data.put("chunkIndex", String.valueOf(chunkIndex));
        data.put("totalChunks", String.valueOf(totalChunks));
        data.put("chunkData", Base64.getEncoder().encodeToString(chunkData));
        data.put("chunkSize", String.valueOf(chunkData.length));
        return data;
    }
    
    /**
     * Tạo message COMPLETE cho file transfer
     */
    public static Map<String, String> createCompleteMessage(String transferId, String checksum) {
        Map<String, String> data = new HashMap<>();
        data.put("transferId", transferId);
        data.put("checksum", checksum);
        return data;
    }
    
    /**
     * Tính checksum (simple MD5-like) cho file integrity
     */
    public static String calculateChecksum(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return "no-checksum";
        }
    }
}
