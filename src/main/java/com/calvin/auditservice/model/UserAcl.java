package com.calvin.auditservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "user_acl",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id")
        }
)
public class UserAcl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;
    private boolean isAdmin;

    @OneToMany(mappedBy = "userAcl")
    private List<UserAclAllowedEntities> allowedEntities;
}
