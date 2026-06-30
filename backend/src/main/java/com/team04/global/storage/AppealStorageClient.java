package com.team04.global.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 전문가 도메인의 비공개 파일(소명 자료, 자격증 등)을 저장하고 접근 URL을 제공하는 클라이언트.
 * 현재 이름은 초기 설계 당시 소명(appeal) 자료 중심이었으나,
 * 전문가 자격증 파일 등 비공개 파일 전반에 사용됩니다.
 * TODO: 역할이 더 명확한 이름(예: PrivateFileStorageClient)으로 리팩토링 예정
 */
public interface AppealStorageClient {
    /**
     * 비공개 파일을 저장하고 객체 키를 반환합니다.
     * @return 객체 키 (예: "expert/appeal/uuid_파일명", "expert/cert/uuid_파일명")
     */
    String upload(MultipartFile file);

    /**
     * 객체 키로 접근 가능한 URL을 반환합니다.
     * 로컬: 정적 URL, 운영: Presigned URL (15분 유효)
     */
    String getAccessUrl(String fileKey);
}