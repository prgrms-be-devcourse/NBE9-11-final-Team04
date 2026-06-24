package com.team04.domain.auth.oauth.client;

import com.team04.domain.auth.oauth.OAuthProperties;
import com.team04.domain.auth.oauth.dto.KakaoTokenResponse;
import com.team04.domain.auth.oauth.dto.KakaoUserInfo;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private final RestClient restClient;
    private final OAuthProperties oAuthProperties;

    public KakaoUserInfo getUserInfo(String code, String redirectUri) {
        String accessToken = exchangeCodeForToken(code, redirectUri);
        return fetchUserInfo(accessToken);
    }

    private String exchangeCodeForToken(String code, String redirectUri) {
        OAuthProperties.Kakao kakao = oAuthProperties.getKakao();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", redirectUri);
        params.add("client_id", kakao.getClientId());

        KakaoTokenResponse response = restClient.post()
                .uri(kakao.getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .onStatus(status -> status.isError(),
                        (req, res) -> { throw new CustomException(ErrorCode.OAUTH_AUTHENTICATION_FAILED); })
                .body(KakaoTokenResponse.class);

        return response.accessToken();
    }

    private KakaoUserInfo fetchUserInfo(String accessToken) {
        return restClient.get()
                .uri(oAuthProperties.getKakao().getUserinfoUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(status -> status.isError(),
                        (req, res) -> { throw new CustomException(ErrorCode.OAUTH_AUTHENTICATION_FAILED); })
                .body(KakaoUserInfo.class);
    }
}
