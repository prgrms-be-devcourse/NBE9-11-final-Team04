package com.team04.domain.settlement.service;

import com.team04.domain.settlement.dto.response.SettlementResponse;
import com.team04.domain.settlement.entity.Settlement;
import com.team04.domain.settlement.repository.SettlementRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;

    @Transactional(readOnly = true)
    public List<SettlementResponse> getSettlementsByIdea(Long ideaId) {
        return settlementRepository.findByIdeaIdOrderByCreatedAtDesc(ideaId)
                .stream()
                .map(SettlementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND));

        return SettlementResponse.from(settlement);
    }
}