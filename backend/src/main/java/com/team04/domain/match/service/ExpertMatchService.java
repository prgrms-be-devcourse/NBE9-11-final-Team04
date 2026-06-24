package com.team04.domain.match.service;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.match.dto.request.ExpertMatchRespondRequest;
import com.team04.domain.match.dto.request.MatchRequest;
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
    private final IdeaRepository ideaRepository;
    private final ExpertProfileRepository expertProfileRepository;


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

    // POST /experts/{expertProfileId} — 매칭 요청
    @Transactional
    public ExpertMatchResponse requestMatch(Long userId, Long expertProfileId, MatchRequest request) {

        // 아이디어 존재 확인 및 소유자 검증
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(request.ideaId())
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));

        idea.validateOwner(userId);

        // 전문가 프로필 존재 및 검증 완료 여부 확인
        ExpertProfile expertProfile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        if (!expertProfile.isVerified()) {
            throw new CustomException(ErrorCode.EXPERT_NOT_VERIFIED);
        }

        // 동일한 아이디어-전문가 조합 중복 요청 방어
        if (expertMatchRepository.existsByIdeaIdAndExpertProfile_Id(request.ideaId(), expertProfileId)) {
            throw new CustomException(ErrorCode.MATCH_ALREADY_REQUESTED);
        }

        ExpertMatch match = ExpertMatch.create(request.ideaId(), expertProfile);
        expertMatchRepository.save(match);

        return ExpertMatchResponse.from(match);
    }
}