package com.team04.domain.dispute.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "dispute_appeals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DisputeAppeal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false, unique = true)
    private Dispute dispute;

    @Column(columnDefinition ="TEXT", nullable = false)
    private String content;

    @Column
    private String fileUrl;

    public DisputeAppeal(Dispute dispute, String content, String fileUrl) {
        this.dispute = dispute;
        this.content = content;
        this.fileUrl = fileUrl;
    }

    public void update(String content, String fileUrl){
        this.content = content;
        this.fileUrl = fileUrl;
    }
}
