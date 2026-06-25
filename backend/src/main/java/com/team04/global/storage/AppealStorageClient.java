package com.team04.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface AppealStorageClient {
    /**
     * 소명 자료 파일을 저장하고 객체 키를 반환합니다.
     * @return 객체 키 (예: "expert/appeal/uuid_파일명")
     */
    String upload(MultipartFile file);

    /**
     * 객체 키로 접근 가능한 URL을 반환합니다.
     * 로컬: 정적 URL, 운영: Presigned URL (15분 유효)
     */
    String getAccessUrl(String fileKey);
}