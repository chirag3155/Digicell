package com.api.digicell.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/*
 * @Author Rohan_Sharma
*/

@Entity
@Table(name = "resource_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourcePermissions {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "parent_id", nullable = true)
	private Long parentId;

	@Column(name = "icon_url", nullable = true)
	private String iconUrl;

	@Column(name = "route_path", nullable = true)
	private String routePath;

	@Column(name = "redirect_url", nullable = true)
	private String redirectUrl;

	@Column(name = "is_active", columnDefinition = "boolean default true")
	private boolean isActive;

	@Column(name = "position")
	private Long position;
	
	@Column(name = "allowed_routes")
	private String allowedRoutes;

	private String descriptionKey;

	@Column(name = "resource_type", nullable = false)
	@Enumerated(EnumType.STRING)
	private ResourcePermissionType resourcePermissionType;

	@OneToMany(mappedBy = "resourcePermission",cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<UserOrgPermissions> userOrgPermissions;
	
}
