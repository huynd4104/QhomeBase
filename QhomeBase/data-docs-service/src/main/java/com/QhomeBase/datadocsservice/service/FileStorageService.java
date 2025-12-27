package com.QhomeBase.datadocsservice.service;

import com.QhomeBase.datadocsservice.config.FileStorageProperties;
import com.QhomeBase.datadocsservice.dto.FileUploadResponse;
import com.QhomeBase.datadocsservice.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;
    private final FileStorageProperties fileStorageProperties;
    
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg", 
            "image/png",
            "image/webp",
            "image/gif"
    );
    
    private static final List<String> ALLOWED_CONTRACT_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/heic",
            "image/heif"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_CONTRACT_FILE_SIZE = 20 * 1024 * 1024;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getLocation())
                .toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("File storage location initialized at: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public FileUploadResponse uploadImage(
            MultipartFile file, 
            UUID ownerId, 
            UUID uploadedBy,
            String category) {
        
        validateImageFile(file);
        
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        
        String fileName = UUID.randomUUID().toString() + "." + fileExtension;
        
        String datePath = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        Path targetLocation = this.fileStorageLocation
                .resolve(category)
                .resolve(ownerId.toString())
                .resolve(datePath)
                .resolve(fileName);
        
        try {
            Files.createDirectories(targetLocation.getParent());
            
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            String fileUrl = String.format("%s/%s/%s/%s/%s",
                    fileStorageProperties.getBaseUrl(),
                    category,
                    ownerId,
                    datePath,
                    fileName);
            
            log.info("File uploaded successfully: {} by user: {}", fileName, uploadedBy);
            
            return FileUploadResponse.success(
                    UUID.randomUUID(),
                    fileName,
                    originalFileName,
                    fileUrl,
                    file.getContentType(),
                    file.getSize(),
                    uploadedBy
            );
            
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String category, String ownerId, String date, String fileName) {
        try {
            Path filePath = this.fileStorageLocation
                    .resolve(category)
                    .resolve(ownerId)
                    .resolve(date)
                    .resolve(fileName)
                    .normalize();
            
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new com.QhomeBase.datadocsservice.exception.FileNotFoundException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new com.QhomeBase.datadocsservice.exception.FileNotFoundException("File not found: " + fileName, ex);
        }
    }

    public void deleteFile(String category, String ownerId, String date, String fileName) {
        try {
            Path filePath = this.fileStorageLocation
                    .resolve(category)
                    .resolve(ownerId)
                    .resolve(date)
                    .resolve(fileName)
                    .normalize();
            
            Files.deleteIfExists(filePath);
            log.info("File deleted successfully: {}", fileName);
        } catch (IOException ex) {
            log.error("Could not delete file: {}", fileName, ex);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Failed to store empty file.");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds maximum limit of 10MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException(
                    "Invalid file type. Only JPEG, PNG, WEBP, and GIF images are allowed.");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.contains("..")) {
            throw new FileStorageException("Sorry! Filename contains invalid path sequence: " + fileName);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    public FileUploadResponse uploadContractFile(
            MultipartFile file,
            UUID contractId,
            UUID uploadedBy) {
        
        validateContractFile(file);
        
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        
        String fileName = UUID.randomUUID().toString() + "." + fileExtension;
        
        Path targetLocation = this.fileStorageLocation
                .resolve("contracts")
                .resolve(contractId.toString())
                .resolve(fileName);
        
        try {
            Files.createDirectories(targetLocation.getParent());
            
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            String fileUrl = String.format("%s/contracts/%s/%s",
                    fileStorageProperties.getBaseUrl(),
                    contractId,
                    fileName);
            
            log.info("Contract file uploaded successfully: {} for contract: {} by user: {}", 
                    fileName, contractId, uploadedBy);
            
            return FileUploadResponse.success(
                    UUID.randomUUID(),
                    fileName,
                    originalFileName,
                    fileUrl,
                    file.getContentType(),
                    file.getSize(),
                    uploadedBy
            );
            
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadContractFileAsResource(UUID contractId, String fileName) {
        try {
            Path filePath = this.fileStorageLocation
                    .resolve("contracts")
                    .resolve(contractId.toString())
                    .resolve(fileName)
                    .normalize();
            
            if (!filePath.startsWith(this.fileStorageLocation.resolve("contracts").normalize())) {
                throw new FileStorageException("Invalid file path: " + fileName);
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new com.QhomeBase.datadocsservice.exception.FileNotFoundException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new com.QhomeBase.datadocsservice.exception.FileNotFoundException("File not found: " + fileName, ex);
        }
    }

    public void deleteContractFile(UUID contractId, String fileName) {
        try {
            Path filePath = this.fileStorageLocation
                    .resolve("contracts")
                    .resolve(contractId.toString())
                    .resolve(fileName)
                    .normalize();
            
            if (!filePath.startsWith(this.fileStorageLocation.resolve("contracts").normalize())) {
                throw new FileStorageException("Invalid file path: " + fileName);
            }
            
            Files.deleteIfExists(filePath);
            log.info("Contract file deleted successfully: {} for contract: {}", fileName, contractId);
        } catch (IOException ex) {
            log.error("Could not delete contract file: {}", fileName, ex);
            throw new FileStorageException("Could not delete file: " + fileName, ex);
        }
    }

    private void validateContractFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File is empty or not provided. Please select a valid file (PDF, JPEG, PNG, HEIC) to upload.");
        }
        
        if (file.getSize() > MAX_CONTRACT_FILE_SIZE) {
            throw new FileStorageException("File size exceeds maximum limit of 20MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTRACT_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException(
                    "Invalid file type. Only PDF, JPEG, PNG, HEIC files are allowed.");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.contains("..")) {
            throw new FileStorageException("Sorry! Filename contains invalid path sequence: " + fileName);
        }
    }

    /**
     * Load file by simple path (for backward compatibility)
     * Example: "news/cover_maintenance.jpg" -> resolves to uploads/news/cover_maintenance.jpg
     * This method handles old URL format without ownerId/date structure
     * Also tries to find file in subdirectories if not found at root level
     */
    public Resource loadFileByPath(String filePath) {
        try {
            // Remove leading slash if present
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1);
            }
            
            log.debug("Loading file by path: {} | Storage location: {}", filePath, this.fileStorageLocation);
            
            // First, try direct path: uploads/news/cover_maintenance.jpg
            Path targetPath = this.fileStorageLocation.resolve(filePath).normalize();
            
            // Security check: ensure the resolved path is within storage location
            if (!targetPath.startsWith(this.fileStorageLocation.normalize())) {
                throw new FileStorageException("Invalid file path: " + filePath);
            }
            
            Resource resource = new UrlResource(targetPath.toUri());
            if (resource.exists() && resource.isReadable()) {
                log.info("File found at direct path: {}", targetPath);
                return resource;
            }
            
            // If not found, try to search in subdirectories
            // Example: news/cover_maintenance.jpg -> search in uploads/news/*/cover_maintenance.jpg
            log.debug("File not found at direct path: {}, searching in subdirectories...", targetPath);
            
            String[] pathParts = filePath.split("/");
            if (pathParts.length >= 2) {
                String category = pathParts[0]; // e.g., "news"
                String fileName = pathParts[pathParts.length - 1]; // e.g., "cover_maintenance.jpg"
                
                Path categoryPath = this.fileStorageLocation.resolve(category);
                if (Files.exists(categoryPath) && Files.isDirectory(categoryPath)) {
                    // Search recursively in category directory
                    try {
                        Path foundFile = Files.walk(categoryPath)
                                .filter(Files::isRegularFile)
                                .filter(p -> p.getFileName().toString().equals(fileName))
                                .findFirst()
                                .orElse(null);
                        
                        if (foundFile != null) {
                            log.info("File found in subdirectory: {}", foundFile);
                            Resource foundResource = new UrlResource(foundFile.toUri());
                            if (foundResource.exists() && foundResource.isReadable()) {
                                return foundResource;
                            }
                        }
                    } catch (IOException e) {
                        log.warn("Error searching for file in subdirectories: {}", e.getMessage());
                    }
                }
            }
            
            log.warn("File not found: {} | Searched at: {}", filePath, targetPath);
            throw new com.QhomeBase.datadocsservice.exception.FileNotFoundException("File not found: " + filePath);
            
        } catch (MalformedURLException ex) {
            log.error("Malformed URL for file path: {}", filePath, ex);
            throw new com.QhomeBase.datadocsservice.exception.FileNotFoundException("File not found: " + filePath, ex);
        } catch (IOException ex) {
            log.error("Error loading file: {}", filePath, ex);
            throw new FileStorageException("Could not load file: " + filePath, ex);
        }
    }
}

