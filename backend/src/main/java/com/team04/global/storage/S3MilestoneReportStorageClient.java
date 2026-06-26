package com.team04.global.storage;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@Profile("!local & !test")
public class S3MilestoneReportStorageClient implements MilestoneReportStorageClient {

    private static final String DIRECTORY = "milestone/reports";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "pdf", "doc", "docx");
    private static final Duration PRESIGNED_URL_EXPIRY = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3MilestoneReportStorageClient(
            S3Client s3Client,
            @Value("${cloud.aws.s3.bucket}") String bucket,
            @Value("${cloud.aws.s3.region:ap-northeast-2}") String region
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public String upload(MultipartFile file, String subDirectory) {
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        String fileKey = DIRECTORY + "/" + sanitizeSubDirectory(subDirectory) + "/"
                + UUID.randomUUID() + "." + extension;

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
            log.info("[S3MilestoneReportStorageClient] 파일 업로드 완료: bucket={}, key={}", bucket, fileKey);
            return fileKey;
        } catch (IOException e) {
            log.error("[S3MilestoneReportStorageClient] 파일 읽기 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (RuntimeException e) {
            log.error("[S3MilestoneReportStorageClient] S3 업로드 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public String getAccessUrl(String fileKey) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRY)
                .getObjectRequest(req -> req
                        .bucket(bucket)
                        .key(fileKey)
                        .build())
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        log.info("[S3MilestoneReportStorageClient] Presigned URL 생성: fileKey={}", fileKey);
        return presignedRequest.url().toString();
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
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeSubDirectory(String subDirectory) {
        if (subDirectory == null || subDirectory.isBlank()
                || subDirectory.contains("/") || subDirectory.contains("..")) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return subDirectory.toLowerCase(Locale.ROOT);
    }
}
