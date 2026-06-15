package com.team04.domain.auth.repository;

import com.team04.domain.auth.entity.SocialAccount;
import com.team04.domain.auth.provider.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderId(Provider provider, String providerId);
}
