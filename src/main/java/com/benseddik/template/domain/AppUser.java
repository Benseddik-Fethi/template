package com.benseddik.template.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_app_user_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_app_user_external_id", columnNames = "external_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser extends AbstractAuditingEntity{

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email", nullable = false, length = 190)
    private String email;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "external_id", length = 64)
    private String externalId;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;
}