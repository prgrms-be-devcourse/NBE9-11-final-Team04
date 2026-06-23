package com.team04.global.storage;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * [로컬 환경 전용 파일 저장소 클라이언트]
 * * - 실제 운영 환경(S3)과 동일한 인터페이스(StorageClient)를 구현합니다.
 * 내 컴퓨터(로컬 디스크)의 특정 폴더를 임시 가상 S3 창고로 활용합니다.
 */
@Slf4j
@Component
@Profile({"local", "test"})
public class LocalStorageClient implements StorageClient {

    // application.yml에서 경로를 읽어옴 (지정되지 않은 경우 프로젝트 루트의 ./uploads 사용)
    @Value("${storage.local.base-path:./uploads}")
    private String basePath;

    // 업로드 완료 후 웹 브라우저가 접근할 수 있는 가상 베이스 URL 주소
    @Value("${storage.local.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    /**
     * 파일을 로컬 디스크에 물리적으로 저장하고, 브라우저에서 접근 가능한 가상 URL 주소를 반환합니다.
     * * @param file      사용자가 업로드한 파일 객체 (바이너리 데이터 및 메타데이터 포함)
     * @param directory 저장 영역을 분리하기 위한 하위 디렉토리 경로 (예: "expert/qualification")
     * @return DB에 문자열(String)로 저장될 최종 파일 접근 URL 주소
     */
    @Override
    public String upload(MultipartFile file, String directory) {
        try {
            // [경로 설정] 기지 경로와 도메인별 디렉토리를 결합하여 저장 위치 객체화 (ex: ./uploads/expert/qualification)
            Path dirPath = Paths.get(basePath, directory).toAbsolutePath().normalize();
            // [폴더 자동 생성] 해당 경로에 폴더가 없다면 하위 폴더까지 통째로 생성 (이미 존재하면 무시)
            Files.createDirectories(dirPath);

            // [파일명 고유화] 파일명 중복으로 인한 덮어쓰기 문제를 막기 위해 랜덤 UUID를 원본 파일명 앞에 결합
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            // [최종 파일 목적지 설정] 생성된 폴더 경로 뒤에 고유화된 파일명을 결합
            Path filePath = dirPath.resolve(fileName);
            // [물리적 저장] MultipartFile 안의 진짜 바이너리 데이터를 내 하드디스크 파일로 복사/출력
            file.transferTo(filePath.toFile());

            log.info("[LocalStorageClient] 파일 저장 완료: {}", filePath);
            return baseUrl + "/" + directory + "/" + fileName;

        } catch (IOException e) {
            log.error("[LocalStorageClient] 파일 저장 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}
