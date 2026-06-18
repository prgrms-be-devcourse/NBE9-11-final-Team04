package com.team04.infra.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtp(String to, String otp){
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[SeedLink] 이메일 인증 코드");
            message.setText("인증 코드 : " + otp + "\n\n5분 내에 입력해주세요.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("OTP 이메일 발송 실패: {}", to, e);
        }
    }
}
