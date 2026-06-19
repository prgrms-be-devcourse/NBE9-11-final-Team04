package com.team04.global.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@Profile("!local")
public class S3StorageClient implements StorageClient {
    // TODO: S3 스크립트 준비 후 구현
    // @Value("${cloud.aws.s3.bucket}") private String bucket;
    // private final AmazonS3 amazonS3;

    @Override
    public String upload(MultipartFile file, String directory) {
        // TODO: 실제 S3 업로드 구현
        throw new UnsupportedOperationException("S3 업로드 미구현");
    }
}
