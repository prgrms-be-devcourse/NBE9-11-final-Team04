package com.team04.domain.user.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String intro;

    @Column(length = 500)
    private String portfolioUrl;

    @Column(length = 500)
    private String profileImage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static Profile create(User user) {
        Profile profile = new Profile();
        profile.user = user;
        return profile;
    }

    public void update(String intro, String portfolioUrl) {
        this.intro = intro;
        this.portfolioUrl = portfolioUrl;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
