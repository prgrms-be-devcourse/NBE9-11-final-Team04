package com.team04.domain.payment.repository;
import com.team04.domain.payment.entity.VirtualAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface VirtualAccountRepository extends JpaRepository<VirtualAccount, Long> {
    Optional<VirtualAccount> findByOrderId(String orderId);
}
