package com.team04.infra.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendAdminInvite(String to, String inviteUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setSubject("[SeedLink] 관리자 초대");
            helper.setText(
                "<p>아래 버튼을 클릭해 관리자 계정을 생성해주세요.</p>" +
                "<a href=\"" + inviteUrl + "\" style=\"display:inline-block;padding:12px 24px;" +
                "background-color:#4F46E5;color:white;text-decoration:none;border-radius:6px;" +
                "font-weight:bold;\">관리자 계정 생성하기</a>" +
                "<p style=\"color:#888;font-size:12px;\">링크는 24시간 후 만료됩니다.</p>",
                true
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("관리자 초대 이메일 발송 실패: {}", to, e);
        }
    }

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
