package com.team04.domain.idea.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 로그인 사용자가 관심 프로젝트로 저장한 아이디어 북마크 엔티티입니다. */
@Entity
@Table(
        name = "idea_bookmark",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_idea_bookmark_user_idea",
                columnNames = {"user_id", "idea_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdeaBookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "idea_id", nullable = false)
    private Long ideaId;

    /** 사용자와 아이디어 ID만 저장해 도메인 간 FK 결합 없이 북마크를 생성합니다. */
    public IdeaBookmark(Long userId, Long ideaId) {
        this.userId = userId;
        this.ideaId = ideaId;
    }
}
