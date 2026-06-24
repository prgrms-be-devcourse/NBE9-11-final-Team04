package com.team04.global.util;

import java.util.Arrays;
import java.util.List;

/** 이미지 URL 목록과 DB 저장용 문자열 사이의 변환을 담당하는 유틸리티입니다. */
public final class ImageUrlConverter {

    private ImageUrlConverter() {
    }

    /** 이미지 URL 목록을 콤마 구분 문자열로 합치며 값이 없으면 null을 반환합니다. */
    public static String join(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        return String.join(",", urls);
    }

    /** 콤마 구분 이미지 URL 문자열을 목록으로 파싱하며 null이면 빈 목록을 반환합니다. */
    public static List<String> parse(String urls) {
        if (urls == null || urls.isBlank()) {
            return List.of();
        }
        return Arrays.stream(urls.split(","))
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .toList();
    }
}
