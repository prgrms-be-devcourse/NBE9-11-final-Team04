package com.team04.global.storage;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 운영 환경에서 이미지 파일을 AWS S3 버킷에 저장하고 공개 URL을 반환하는 저장소 클라이언트입니다. */
@Slf4j
@Component
@Profile("!local & !test")
@RequiredArgsConstructor
public class S3StorageClient implements StorageClient {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.region:ap-northeast-2}")
    private String region;

    /** 업로드 파일을 검증한 뒤 S3에 저장하고 브라우저 접근 가능한 공개 URL을 반환합니다. */

    @Override
    public String upload(MultipartFile file, String directory) {
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        String key = directory + "/" + UUID.randomUUID() + "." + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("[S3StorageClient] 파일 업로드 완료: bucket={}, key={}", bucket, key);
            return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        } catch (IOException e) {
            log.error("[S3StorageClient] 파일 읽기 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (RuntimeException e) {
            log.error("[S3StorageClient] S3 업로드 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /** 이미지 파일 존재 여부, 크기, 확장자 허용 목록을 검증합니다. */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    /** 원본 파일명에서 소문자 확장자를 추출합니다. */
    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
