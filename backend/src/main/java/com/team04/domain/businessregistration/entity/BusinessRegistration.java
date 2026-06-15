package com.team04.domain.businessregistration.entity;

import com.team04.domain.user.entity.User;
import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "business_registration")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessRegistration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String businessNumber;

    @Column
    private boolean verified;

    @Column
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static BusinessRegistration create(User user, String businessNumber) {
        BusinessRegistration br = new BusinessRegistration();
        br.user = user;
        br.businessNumber = businessNumber;
        br.verified = false;
        return br;
    }

    public void verify() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }

}
