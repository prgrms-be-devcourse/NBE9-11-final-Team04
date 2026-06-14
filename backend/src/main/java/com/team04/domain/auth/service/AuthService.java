package com.team04.domain.auth.service;

import com.team04.domain.auth.dto.*;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.user.status.UserStatus;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.util.JwtUtil;
import com.team04.infra.email.EmailService;
import com.team04.infra.redis.OtpRepository;
import com.team04.infra.redis.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpRepository otpRepository;
    private final EmailService emailService;

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int OTP_LENGTH = 6;

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    @Transactional
    public void signup(SignupRequest request){
        if(userRepository.existsByEmail(request.email())){
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        User user = User.create(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.nickname(),
                request.age(),
                request.role()
        );
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request){
        User user = getActiveUserOrThrow(request.email());

        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken, REFRESH_TOKEN_TTL);

        return new TokenResponse(accessToken, refreshToken);
    }

    public void logout(Long userId){
        refreshTokenRepository.delete(userId);
    }

    @Transactional(readOnly = true)
    public TokenResponse tokenRefresh(TokenRefreshRequest request){
        String refreshToken = request.refreshToken();

        if(!jwtUtil.validate(refreshToken)){
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        String stored = refreshTokenRepository.find(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        if(!stored.equals(refreshToken)){
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.ACCOUNT_WITHDRAWN);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), newRefreshToken, REFRESH_TOKEN_TTL);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void sendOtp(EmailSendRequest request){
        String otp = generateOtp();
        otpRepository.save(request.email(), otp, OTP_TTL);
        emailService.sendOtp(request.email(), otp);
    }

    public void verifyOtp(EmailVerifyRequest request){
        String stored = otpRepository.find(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.OTP_EXPIRED));

        if(!stored.equals(request.otp())){
            throw new CustomException(ErrorCode.INVALID_OTP);
        }

        otpRepository.delete(request.email());
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateOtp() {
        int otp = SECURE_RANDOM.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }

    //탈퇴,정지된 회원인지 권한 검증
    private User getActiveUserOrThrow(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.ACCOUNT_WITHDRAWN);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        return user;
    }
}
