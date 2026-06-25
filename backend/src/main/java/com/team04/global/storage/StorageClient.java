package com.team04.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageClient {
    /**
     * 파일을 저장하고 접근 가능한 URL을 반환합니다.
     *
     * @param file      업로드할 파일
     * @param directory 저장 경로 (예: "expert/appeal")
     * @return 저장된 파일 URL
     */
    String upload(MultipartFile file, String directory);

    /**
     * 저장소에 업로드된 파일을 삭제합니다.
     * 삭제 실패는 비즈니스 흐름을 막지 않도록 구현체에서 경고 로그로만 처리합니다.
     *
     * @param url 삭제할 파일 접근 URL
     */
    void delete(String url);
}
