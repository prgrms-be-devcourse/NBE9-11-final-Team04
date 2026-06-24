package com.team04.global.storage;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
@Profile("local")
public class LocalAppealStorageClient implements AppealStorageClient {

    private static final String DIRECTORY = "expert/appeal";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final java.util.Set<String> ALLOWED_EXTENSIONS =
            java.util.Set.of("jpg", "jpeg", "png", "pdf", "doc", "docx");

    @Value("${storage.local.base-path:./uploads}")
    private String basePath;

    @Value("${storage.local.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    @Override
    public String upload(MultipartFile file) {
        validateFile(file);

        try {
            Path dirPath = Paths.get(basePath, DIRECTORY).toAbsolutePath().normalize();
            Files.createDirectories(dirPath);

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = dirPath.resolve(fileName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath);
            }

            String fileKey = DIRECTORY + "/" + fileName;
            log.info("[LocalAppealStorageClient] 파일 저장 완료: {}", filePath);
            return fileKey;

        } catch (IOException e) {
            log.error("[LocalAppealStorageClient] 파일 저장 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public String getAccessUrl(String fileKey) {
        return baseUrl + "/" + fileKey;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(java.util.Locale.ROOT);
    }
}