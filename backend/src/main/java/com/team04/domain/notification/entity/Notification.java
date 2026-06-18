package com.team04.domain.notification.entity;

import com.team04.domain.user.entity.User;
import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    @Column
    private Long referenceId;

    @Column(nullable = false)
    private boolean isRead = false;

    public void read() {
        this.isRead = true;
    }

    public static Notification create(User user, NotificationType type,
                                      String title, String message, Long referenceId) {
        Notification n = new Notification();
        n.user = user;
        n.notificationType = type;
        n.title = title;
        n.message = message;
        n.referenceId = referenceId;
        return n;
    }

}
