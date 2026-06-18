package com.team04.domain.auth.entity;

import com.team04.domain.auth.provider.Provider;
import com.team04.domain.user.entity.User;
import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "social_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false)
    private String providerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",  nullable = false)
    private User user;

    public static SocialAccount create(Provider provider, String providerId, User user) {
        SocialAccount account = new SocialAccount();
        account.provider = provider;
        account.providerId = providerId;
        account.user = user;
        return account;
    }
}
