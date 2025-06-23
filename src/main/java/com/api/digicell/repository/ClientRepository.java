package com.api.digicell.repository;

import com.api.digicell.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, String> {

    List<Client> findByIsAssigned(Boolean isAssigned);
    
    Optional<Client> findByEmail(String email);

    @Query("SELECT DISTINCT c.client FROM Conversation c WHERE c.userAccount.userId = :userId")
    List<Client> findByUserAccount_UserId(@Param("userId") Long userId);
} 