package com.team04.domain.auth.oauth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    private Google google = new Google();
    private Kakao kakao = new Kakao();

    @Getter
    @Setter
    public static class Google {
        private String clientId;
        private String clientSecret;
        private String authorizeUri = "https://accounts.google.com/o/oauth2/v2/auth";
        private String tokenUri = "https://oauth2.googleapis.com/token";
        private String userinfoUri = "https://www.googleapis.com/oauth2/v2/userinfo";
    }

    @Getter
    @Setter
    public static class Kakao {
        private String clientId;
        private String authorizeUri = "https://kauth.kakao.com/oauth/authorize";
        private String tokenUri = "https://kauth.kakao.com/oauth/token";
        private String userinfoUri = "https://kapi.kakao.com/v2/user/me";
    }
}
