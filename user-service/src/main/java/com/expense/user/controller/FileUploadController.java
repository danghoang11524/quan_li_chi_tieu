package com.expense.user.controller;

import com.expense.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Value("${file.upload.dir:./uploads/avatars}")
    private String uploadDir;

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file) {

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is empty"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File must be an image"));
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File size must be less than 5MB"));
            }

            // Create upload directory if not exists
            File uploadDirectory = new File(uploadDir);
            if (!uploadDirectory.exists()) {
                uploadDirectory.mkdirs();
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
            String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = Paths.get(uploadDir, filename);
            Files.write(filePath, file.getBytes());

            // Return URL (in production, this should be a CDN or cloud storage URL)
            String avatarUrl = "/uploads/avatars/" + filename;

            Map<String, String> response = new HashMap<>();
            response.put("avatarUrl", avatarUrl);
            response.put("filename", filename);

            return ResponseEntity.ok(ApiResponse.success("Avatar uploaded successfully", response));

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }
}
