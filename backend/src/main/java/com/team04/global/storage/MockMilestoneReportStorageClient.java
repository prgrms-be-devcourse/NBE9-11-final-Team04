package com.team04.global.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@Profile("test")
public class MockMilestoneReportStorageClient implements MilestoneReportStorageClient {

    @Override
    public String upload(MultipartFile file, String subDirectory) {
        log.info("[MockMilestoneReportStorageClient] 파일 업로드 Mock: {}", file.getOriginalFilename());
        return "milestone/reports/" + subDirectory + "/mock-" + file.getOriginalFilename();
    }

    @Override
    public String getAccessUrl(String fileKey) {
        return "http://mock-storage/" + fileKey;
    }
}
