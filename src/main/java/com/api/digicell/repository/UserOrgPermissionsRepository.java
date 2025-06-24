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
    
    List<UserOrgPermissions> findByUser(UserAccount userAccount);
    
    List<UserOrgPermissions> findByUserUserId(Long userId);
    
    @Query("SELECT DISTINCT uop FROM UserOrgPermissions uop JOIN FETCH uop.organization WHERE uop.user = :userAccount")
    List<UserOrgPermissions> findDistinctByUser(@Param("userAccount") UserAccount userAccount);
    
    @Query("SELECT DISTINCT uop.organization FROM UserOrgPermissions uop WHERE uop.user = :userAccount")
    List<Organization> findDistinctOrganizationsByUserAccount(@Param("userAccount") UserAccount userAccount);
    
    @Query("SELECT DISTINCT org.tenantId FROM UserOrgPermissions uop JOIN uop.organization org WHERE uop.user = :userAccount AND org.tenantId IS NOT NULL")
    List<String> findDistinctTenantIdsByUser(@Param("userAccount") UserAccount userAccount);
} 