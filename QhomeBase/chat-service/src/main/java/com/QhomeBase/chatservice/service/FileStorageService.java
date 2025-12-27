package com.QhomeBase.chatservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${chat.upload.directory:uploads/chat}")
    private String uploadDirectory;

    @Value("${chat.cdn.base-url:}")
    private String cdnBaseUrl;

    /**
     * Upload image for chat message
     */
    public String uploadImage(MultipartFile file, UUID groupId) throws IOException {
        log.info("Uploading image for group: {}", groupId);
        log.info("Upload directory: {}", uploadDirectory);
        
        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDirectory, groupId.toString());
        Files.createDirectories(uploadPath);
        log.info("Created upload path: {}", uploadPath.toAbsolutePath());

        // Preserve original file extension
        String originalFileName = file.getOriginalFilename();
        String extension = ".jpg"; // default
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        String fileName = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(fileName);
        log.info("Saving file to: {}", filePath.toAbsolutePath());
        
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Verify file was saved
        if (!Files.exists(filePath)) {
            throw new IOException("File was not saved successfully: " + filePath);
        }
        log.info("File saved successfully, size: {} bytes", Files.size(filePath));

        String imageUrl = getImageUrl(groupId.toString(), fileName);
        log.info("Uploaded image URL: {}", imageUrl);
        log.info("Full file path: {}", filePath.toAbsolutePath());
        return imageUrl;
    }

    /**
     * Upload multiple images for chat messages
     */
    public List<String> uploadImages(List<MultipartFile> files, UUID groupId) throws IOException {
        log.info("Uploading {} images for group: {}", files.size(), groupId);
        
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String imageUrl = uploadImage(file, groupId);
                imageUrls.add(imageUrl);
            } catch (IOException e) {
                log.error("Error uploading image {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                throw new IOException("Failed to upload image: " + file.getOriginalFilename(), e);
            }
        }
        
        log.info("Successfully uploaded {} images", imageUrls.size());
        return imageUrls;
    }

    /**
     * Upload audio file for chat message
     */
    public String uploadAudio(MultipartFile file, UUID groupId) throws IOException {
        log.info("Uploading audio for group: {}", groupId);
        
        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDirectory, groupId.toString(), "audio");
        Files.createDirectories(uploadPath);

        String fileName = UUID.randomUUID().toString() + ".m4a"; // Default to m4a for voice messages
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String audioUrl = getAudioUrl(groupId.toString(), fileName);
        log.info("Uploaded audio: {}", audioUrl);
        return audioUrl;
    }

    /**
     * Upload video file for chat message
     */
    public String uploadVideo(MultipartFile file, UUID groupId) throws IOException {
        log.info("Uploading video for group: {}", groupId);
        
        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDirectory, groupId.toString(), "video");
        Files.createDirectories(uploadPath);

        String originalFileName = file.getOriginalFilename();
        String extension = ".mp4"; // Default to mp4
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        String fileName = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String videoUrl = getVideoUrl(groupId.toString(), fileName);
        log.info("Uploaded video: {}", videoUrl);
        return videoUrl;
    }

    /**
     * Upload file for chat message
     */
    public String uploadFile(MultipartFile file, UUID groupId) throws IOException {
        log.info("Uploading file for group: {}", groupId);
        
        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDirectory, groupId.toString(), "files");
        Files.createDirectories(uploadPath);

        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        String fileName = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = getFileUrl(groupId.toString(), fileName);
        log.info("Uploaded file: {}", fileUrl);
        return fileUrl;
    }

    /**
     * Delete image file
     */
    public void deleteImage(UUID groupId, String fileName) {
        try {
            Path filePath = Paths.get(uploadDirectory, groupId.toString(), fileName);
            Files.deleteIfExists(filePath);
            log.info("Deleted image: {}", filePath);
        } catch (IOException e) {
            log.error("Error deleting image: {}", fileName, e);
        }
    }

    /**
     * Delete file
     */
    public void deleteFile(UUID groupId, String fileName) {
        try {
            Path filePath = Paths.get(uploadDirectory, groupId.toString(), "files", fileName);
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            log.error("Error deleting file: {}", fileName, e);
        }
    }

    private String getImageUrl(String groupId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            return cdnBaseUrl + "/chat/" + groupId + "/" + fileName;
        }
        return "/uploads/chat/" + groupId + "/" + fileName;
    }

    private String getAudioUrl(String groupId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            return cdnBaseUrl + "/chat/" + groupId + "/audio/" + fileName;
        }
        return "/uploads/chat/" + groupId + "/audio/" + fileName;
    }

    private String getVideoUrl(String groupId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            return cdnBaseUrl + "/chat/" + groupId + "/video/" + fileName;
        }
        return "/uploads/chat/" + groupId + "/video/" + fileName;
    }

    private String getFileUrl(String groupId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            return cdnBaseUrl + "/chat/" + groupId + "/files/" + fileName;
        }
        return "/uploads/chat/" + groupId + "/files/" + fileName;
    }
}

