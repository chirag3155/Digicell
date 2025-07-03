package com.api.digicell.repository;

import com.api.digicell.entities.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginInfoRepo extends JpaRepository<UserAccount, Long> {
    boolean existsByEmail(String email);
} 