package com.team04.domain.payment.service;

import com.team04.domain.payment.client.PaymentGateway;
import com.team04.domain.payment.dto.response.VirtualAccountIssueResult;
import com.team04.domain.payment.entity.VirtualAccount;
import com.team04.domain.payment.repository.VirtualAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VirtualAccountService {

    private final VirtualAccountRepository virtualAccountRepository;
    private final PaymentGateway paymentGateway;

    @Transactional
    public VirtualAccountIssueResult issueAndSave(String orderId, long amount) {
        VirtualAccountIssueResult issued = paymentGateway.issueVirtualAccount(orderId, amount);

        VirtualAccount saved = virtualAccountRepository.save(VirtualAccount.create(
                orderId,
                issued.bankCode(),
                issued.accountNumber(),
                issued.dueDate(),
                amount
        ));

        return new VirtualAccountIssueResult(
                saved.getId(),
                saved.getBankCode(),
                saved.getAccountNumber(),
                saved.getDueDate()
        );
    }
}
