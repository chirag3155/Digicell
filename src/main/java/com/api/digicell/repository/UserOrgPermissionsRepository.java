package com.api.digicell.repository;

import com.api.digicell.entities.UserAccount;
import com.api.digicell.entities.UserOrgPermissions;
import com.api.digicell.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserOrgPermissionsRepository extends JpaRepository<UserOrgPermissions, Long> {
    
    Set<UserOrgPermissions> findByUserAccount(UserAccount userAccount);
    
    Set<UserOrgPermissions> findByUserAccountUserId(Long userId);
    
    @Query("SELECT DISTINCT uop.organization FROM UserOrgPermissions uop WHERE uop.userAccount = :userAccount")
    Set<Organization> findDistinctOrganizationsByUserAccount(@Param("userAccount") UserAccount userAccount);
} 