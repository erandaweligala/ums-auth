package com.adl.et.telco.crm.usermanagerservice.mapper.role;

import com.adl.et.telco.crm.usermanagerservice.dto.permission.menuandcomponents.ComponentsItem;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.menuandcomponents.MenuComponents;
import com.adl.et.telco.crm.usermanagerservice.dto.role.PermissionsItem;
import com.adl.et.telco.crm.usermanagerservice.dto.role.RoleDetails;
import com.adl.et.telco.crm.usermanagerservice.dto.role.RoleDetailsRepoDTO;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Roles;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class RoleMapper {
    public RoleDetails mapRoleDetails(List<RoleDetailsRepoDTO> roleList, Roles role, List<MenuComponents> menuComponents) throws BaseException {
        try {

            RoleDetails roleDetails = new RoleDetails();
            roleDetails.setRoleName(role.getName());
            roleDetails.setDescription(role.getDescription());
            roleDetails.setRoleId(String.valueOf(role.getId()));

            List<PermissionsItem> permissions = new ArrayList<>();

            for (MenuComponents mc : menuComponents) {
                for (ComponentsItem ci : mc.getComponents()) {
                    PermissionsItem permissionsItem = new PermissionsItem();
                    permissionsItem.setComponentId(ci.getComponentId());
                    permissionsItem.setMenuId(mc.getMenuId());
                    permissionsItem.setMenuName(mc.getMenuName());
                    permissionsItem.setComponentName(ci.getComponentName());
                    Optional<RoleDetailsRepoDTO> permission = roleList.stream().filter(e -> e.getComponentId().equals(ci.getComponentId()) && e.getMenuId().equals(mc.getMenuId())).findFirst();
                    if (permission.isPresent()) {
                        RoleDetailsRepoDTO pm = permission.get();
                        permissionsItem.setPermissionId(pm.getPermissionId());
                        permissionsItem.setPermissionDescription(pm.getPermissionDescription());
                        permissionsItem.setPermissionName(pm.getPermissionName());
                    }

                    permissions.add(permissionsItem);

                }
            }
            roleDetails.setPermissions(permissions);
            return roleDetails;
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());

        }
    }
}

