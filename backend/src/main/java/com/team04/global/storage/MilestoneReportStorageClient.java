package com.team04.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface MilestoneReportStorageClient {

    /**
     * 완료/소명 보고서 첨부 파일을 저장하고 객체 key를 반환합니다.
     * DB에는 공개 URL이 아니라 객체 key를 저장합니다.
     */
    String upload(MultipartFile file, String subDirectory);

    /**
     * 저장된 객체 key를 접근 가능한 URL로 변환합니다.
     * 운영 환경에서는 Presigned URL을 반환합니다.
     */
    String getAccessUrl(String fileKey);
}
