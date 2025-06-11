package com.api.digicell.repository;

import com.api.digicell.entities.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRepository extends JpaRepository<UserAccount, Long> {
} 