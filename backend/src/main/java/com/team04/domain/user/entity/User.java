package com.team04.domain.user.entity;

import com.team04.domain.user.status.UserStatus;
import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private int age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    public static User create(String email, String password, String name,
                              String nickname, int age, Role role) {
        User user = new User();
        user.email = email;
        user.password = password;
        user.name = name;
        user.nickname = nickname;
        user.age = age;
        user.role = role;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public void update(String nickname) {
        this.nickname = nickname;
    }

    public void changePassword(String encoderPassword) {
        this.password = encoderPassword;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }
}
