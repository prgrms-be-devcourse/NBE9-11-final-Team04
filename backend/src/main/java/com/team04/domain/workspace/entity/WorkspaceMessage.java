package com.team04.domain.workspace.entity;

import com.team04.global.entity.BaseEntity;
import com.team04.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 워크스페이스(아이디어) 내 참여자 간 메시지를 저장하는 엔티티입니다. */
@Entity
@Table(name = "workspace_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private WorkspaceMessage(Long ideaId, Long authorId, String content) {
        this.ideaId = ideaId;
        this.authorId = authorId;
        this.content = content;
    }

    public static WorkspaceMessage create(Long ideaId, Long authorId, String content) {
        return new WorkspaceMessage(ideaId, authorId, content);
    }
}
