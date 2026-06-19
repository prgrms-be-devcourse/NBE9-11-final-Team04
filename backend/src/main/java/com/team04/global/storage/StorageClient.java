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
}
