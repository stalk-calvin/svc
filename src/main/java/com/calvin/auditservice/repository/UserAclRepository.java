package com.calvin.auditservice.repository;

import com.calvin.auditservice.model.UserAcl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAclRepository extends JpaRepository<UserAcl, Long> {
    UserAcl findByUserId(String userId);
}
