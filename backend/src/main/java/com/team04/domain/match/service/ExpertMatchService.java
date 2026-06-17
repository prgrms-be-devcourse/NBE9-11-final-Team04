package com.team04.domain.match.service;

import com.team04.domain.match.dto.request.ExpertMatchRespondRequest;
import com.team04.domain.match.dto.response.ExpertMatchResponse;
import com.team04.domain.match.entity.ExpertMatch;
import com.team04.domain.match.entity.MatchStatus;
import com.team04.domain.match.repository.ExpertMatchRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpertMatchService {

    private final ExpertMatchRepository expertMatchRepository;

    // GET /experts/matches — 내 매칭 요청 목록
    @Transactional(readOnly = true)
    public List<ExpertMatchResponse> getMatches(Long userId) {
        return expertMatchRepository.findAllByUserId(userId)
                .stream()
                .map(ExpertMatchResponse::from)
                .toList();
    }

    // PATCH /experts/matches/{matchId} — 수락/거절
    @Transactional
    public ExpertMatchResponse respond(Long userId, Long matchId, ExpertMatchRespondRequest request) {

        ExpertMatch match = expertMatchRepository.findByIdAndUserId(matchId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATCH_NOT_FOUND));

        // REJECTED인데 거절 사유가 없으면 예외
        if (request.status() == MatchStatus.REJECTED
                && (request.rejectReason() == null || request.rejectReason().isBlank())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (request.status() == MatchStatus.ACCEPTED) {
            match.accept();
        } else if (request.status() == MatchStatus.REJECTED) {
            match.reject(request.rejectReason());
        } else {
            // PENDING으로 다시 되돌리는 건 불가
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        return ExpertMatchResponse.from(match);
    }
}