package org.farmsystem.homepage.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.farmsystem.homepage.domain.common.entity.BaseTimeEntity;
import org.farmsystem.homepage.domain.common.entity.Track;
import org.hibernate.annotations.ColumnDefault;

import java.util.List;
@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
@Table(name = "user")
@Entity
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    @Column
    private String socialId;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(nullable = false, length = 20)
    private String studentNumber;

    @Setter
    @Column(nullable = false, length = 50)
    private String major;

    @Setter
    @Column(length = 500)
    private String profileImageUrl;

    @Setter
    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 100)
    private String notionAccount;

    @Column(nullable = false, length = 100)
    private String githubAccount;

    @Enumerated(EnumType.STRING)
    private Track track;

    @Column
    private Integer generation;

    @ColumnDefault("false")
    private boolean isDeleted;

    @ColumnDefault("0")
    private int currentSeed;

    @ColumnDefault("0")
    private int totalSeed;

    @OneToMany(mappedBy = "user")
    private List<TrackHistory> trackHistories;

}
