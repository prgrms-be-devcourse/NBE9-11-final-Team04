package com.team04.domain.auth.oauth.client;

import com.team04.domain.auth.oauth.OAuthProperties;
import com.team04.domain.auth.oauth.dto.GoogleTokenResponse;
import com.team04.domain.auth.oauth.dto.GoogleUserInfo;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

    private final RestClient restClient;
    private final OAuthProperties oAuthProperties;

    public GoogleUserInfo getUserInfo(String code, String redirectUri) {
        log.info("[Google OAuth] token exchange 시작. redirectUri={}", redirectUri);
        String accessToken = exchangeCodeForToken(code, redirectUri);
        log.info("[Google OAuth] token 획득 성공. userinfo 요청 시작");
        return fetchUserInfo(accessToken);
    }

    private String exchangeCodeForToken(String code, String redirectUri) {
        OAuthProperties.Google google = oAuthProperties.getGoogle();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", redirectUri);
        params.add("client_id", google.getClientId());
        params.add("client_secret", google.getClientSecret());

        GoogleTokenResponse response = restClient.post()
                .uri(google.getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    try {
                        String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.error("[Google OAuth] token exchange 실패. status={}, body={}", res.getStatusCode(), body);
                    } catch (Exception e) {
                        log.error("[Google OAuth] token exchange 실패. status={}", res.getStatusCode());
                    }
                    throw new CustomException(ErrorCode.OAUTH_AUTHENTICATION_FAILED);
                })
                .body(GoogleTokenResponse.class);

        return response.accessToken();
    }

    private GoogleUserInfo fetchUserInfo(String accessToken) {
        return restClient.get()
                .uri(oAuthProperties.getGoogle().getUserinfoUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    try {
                        String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.error("[Google OAuth] userinfo 요청 실패. status={}, body={}", res.getStatusCode(), body);
                    } catch (Exception e) {
                        log.error("[Google OAuth] userinfo 요청 실패. status={}", res.getStatusCode());
                    }
                    throw new CustomException(ErrorCode.OAUTH_AUTHENTICATION_FAILED);
                })
                .body(GoogleUserInfo.class);
    }
}
