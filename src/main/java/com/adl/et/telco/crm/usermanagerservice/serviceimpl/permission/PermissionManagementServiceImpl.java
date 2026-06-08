package com.adl.et.telco.crm.usermanagerservice.serviceimpl.permission;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.*;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.menuandcomponents.ComponentsItem;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.menuandcomponents.MenuComponents;
import com.adl.et.telco.crm.usermanagerservice.dto.user.TableFilterRequest;
import com.adl.et.telco.crm.usermanagerservice.filter.FilterValueExtractor;
import com.adl.et.telco.crm.usermanagerservice.mapper.common.MySQLEventMapper;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.*;
import com.adl.et.telco.crm.usermanagerservice.repository.*;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.permission.PermissionManagementInterface;
import com.adl.et.telco.crm.usermanagerservice.util.PageUtils;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionManagementServiceImpl implements PermissionManagementInterface {

    private final PermissionsRepository permissionsRepository;
    private final ResponseHandler       responseHandler;
    private final MenusRepository       menusRepository;
    private final ComponentRepository   componentRepository;
    private final ActionsRepository     actionsRepository;
    private final AttributeRepository   attributeRepository;
    private final TenantsRepository     tenantsRepository;
    private final StatusRepository      statusRepository;
    private final FilterValueExtractor  filterValueExtractor;
    private final EntityManager         entityManager;
    private final MySQLEventMapper      mysqlEventMapper;   // ← Kafka publisher

    // =========================================================================
    // Read-only methods — UNCHANGED
    // =========================================================================

    @Override
    public CommonAdaptorResp<List<PermissionView>> getPermissionList(String permissionName, Long menuId, Long componentId, int limit, int offset) throws BaseException {
        try {
            List<PermissionView> permissionViewList = permissionsRepository.getPermissionList(Long.parseLong(MDC.get(Constants.TENANT_ID)), permissionName, menuId, componentId, limit, offset);
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), permissionViewList,
                    PageUtils.buildPageDetails(limit, offset, !permissionViewList.isEmpty() ? permissionViewList.get(0).getTotalCount() : 0, permissionViewList.size()));
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<PermissionDetails> getPermissionDetails(Long permissionId) throws BaseException {
        try {
            Optional<Permission> searchedPermission = permissionsRepository.findById(permissionId);
            PermissionDetails permissionDetails = new PermissionDetails();

            if (searchedPermission.isPresent()) {
                Permission permission = searchedPermission.get();
                permissionDetails.setPermissionId(permission.getId().toString());
                permissionDetails.setDescription(permission.getDescription());
                permissionDetails.setPermissionName(permission.getName());
                permissionDetails.setMenuId(permission.getMenus().getId().toString());
                permissionDetails.setMenuName(permission.getMenus().getName());
                permissionDetails.setComponentId(permission.getComponents().getId().toString());
                permissionDetails.setComponentName(permission.getComponents().getName());

                List<PermissionDetailsResultSet> permissionDetailsResultSets = permissionsRepository.findPermissionDetails(Long.parseLong(MDC.get(Constants.TENANT_ID)), permission.getId(), permission.getComponents().getId());
                Map<Long, List<PermissionDetailsResultSet>> mainActions = permissionDetailsResultSets.stream().filter(e -> e.getIsManinAction() == 1).collect(Collectors.groupingBy(PermissionDetailsResultSet::getActionId));
                Map<Long, Map<Long, List<PermissionDetailsResultSet>>> subActions = permissionDetailsResultSets.stream().filter(e -> e.getIsManinAction() == 0).collect(Collectors.groupingBy(PermissionDetailsResultSet::getMainActionId, Collectors.groupingBy(PermissionDetailsResultSet::getActionId)));
                List<MainActionsItem> mainActionsList = new ArrayList<>();
                for (Map.Entry<Long, List<PermissionDetailsResultSet>> entry : mainActions.entrySet()) {
                    List<SubActionsItem> subActionsList = new ArrayList<>();
                    if (subActions != null && !subActions.isEmpty() && subActions.get(entry.getKey()) != null)
                        for (Map.Entry<Long, List<PermissionDetailsResultSet>> subEntry : subActions.get(entry.getKey()).entrySet()) {
                            subActionsList.add(SubActionsItem.builder()
                                    .actionId(subEntry.getKey().toString())
                                    .isSelected(subEntry.getValue().get(0).getActionIsSelected() == 1)
                                    .actionName(subEntry.getValue().get(0).getActionName())
                                    .attributes(subEntry.getValue().stream().filter(e -> e.getAttributeId() != null).map(e -> AttributesItem.builder().attributeId(e.getAttributeId().toString()).attributeName(e.getAttributeName()).isSelected(e.getAttributeIsSelected() == 1).build()).collect(Collectors.toList())).build());
                        }
                    mainActionsList.add(MainActionsItem.builder()
                            .actionId(entry.getKey().toString())
                            .isSelected(entry.getValue().get(0).getActionIsSelected() == 1)
                            .actionName(entry.getValue().get(0).getActionName())
                            .subActions(subActionsList)
                            .attributes(entry.getValue().stream().filter(e -> e.getAttributeId() != null).map(e -> AttributesItem.builder().attributeId(e.getAttributeId().toString()).attributeName(e.getAttributeName()).isSelected(e.getAttributeIsSelected() == 1).build()).collect(Collectors.toList())).build());
                }
                permissionDetails.setMainActions(mainActionsList);
            }
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), permissionDetails);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<AllActions> getActionHierarchy(Long componentId) throws BaseException {
        try {
            AllActions actions = new AllActions();
            List<ActionHierarchyResultSet> permissionDetailsResultSets = permissionsRepository.getActionHierarchy(Long.parseLong(MDC.get(Constants.TENANT_ID)), componentId);
            Map<Long, List<ActionHierarchyResultSet>> mainActions = permissionDetailsResultSets.stream().filter(e -> e.getIsManinAction() == 1).collect(Collectors.groupingBy(ActionHierarchyResultSet::getActionId));
            Map<Long, Map<Long, List<ActionHierarchyResultSet>>> subActions = permissionDetailsResultSets.stream().filter(e -> e.getIsManinAction() == 0).collect(Collectors.groupingBy(ActionHierarchyResultSet::getMainActionId, Collectors.groupingBy(ActionHierarchyResultSet::getActionId)));
            List<MainActionsItem> mainActionsList = new ArrayList<>();
            for (Map.Entry<Long, List<ActionHierarchyResultSet>> entry : mainActions.entrySet()) {
                List<SubActionsItem> subActionsList = new ArrayList<>();
                if (subActions != null && !subActions.isEmpty() && subActions.containsKey(entry.getKey()))
                    for (Map.Entry<Long, List<ActionHierarchyResultSet>> subEntry : subActions.get(entry.getKey()).entrySet()) {
                        subActionsList.add(SubActionsItem.builder()
                                .actionId(subEntry.getKey().toString())
                                .actionName(subEntry.getValue().get(0).getActionName())
                                .attributes(subEntry.getValue().stream().filter(e -> e.getAttributeId() != null).map(e -> AttributesItem.builder().attributeId(e.getAttributeId().toString()).attributeName(e.getAttributeName()).build()).collect(Collectors.toList())).build());
                    }
                mainActionsList.add(MainActionsItem.builder()
                        .actionId(entry.getKey().toString())
                        .actionName(entry.getValue().get(0).getActionName())
                        .subActions(subActionsList)
                        .attributes(entry.getValue().stream().filter(e -> e.getAttributeId() != null).map(e -> AttributesItem.builder().attributeId(e.getAttributeId().toString()).attributeName(e.getAttributeName()).build()).collect(Collectors.toList())).build());
            }
            actions.setMainActions(mainActionsList);
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), actions);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<List<MenuComponents>> getAllMenuAndComponents() throws BaseException {
        try {
            List<Menus> menus = menusRepository.findAll().stream()
                    .filter(e -> e.getTenantsList().stream()
                            .anyMatch(f -> f.getId() == Long.parseLong(MDC.get(Constants.TENANT_ID))))
                    .collect(Collectors.toList());
            List<MenuComponents> response = menus.stream().map(x ->
                    MenuComponents.builder()
                            .menuId(x.getId().toString())
                            .menuName(x.getName())
                            .components(x.getComponentsList().stream()
                                    .map(t -> ComponentsItem.builder().componentId(t.getId().toString()).componentName(t.getName()).build())
                                    .collect(Collectors.toList()))
                            .build()
            ).collect(Collectors.toList());
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), response);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<List<PermissionMetaData>> getPermissionMetaData() throws BaseException {
        try {
            List<Permission> allPermissions = permissionsRepository.findAllByTenantId(Long.parseLong(MDC.get(Constants.TENANT_ID)));
            List<PermissionMetaData> response = allPermissions.stream().map(e ->
                    PermissionMetaData.builder()
                            .permissionDescription(e.getDescription())
                            .permissionId(e.getId().toString())
                            .permissionName(e.getName())
                            .menuId(e.getMenus().getId().toString())
                            .menuName(e.getMenus().getName())
                            .componentId(e.getComponents().getId().toString())
                            .componentName(e.getComponents().getName())
                            .build()).collect(Collectors.toList());
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), response);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    // =========================================================================
    // createPermission  ← CHANGED: no permissionsRepository.save(); uses Kafka
    // =========================================================================

    @Override
    public CommonAdaptorResp<String> createPermission(CreatePermission createPermission) throws BaseException {
        try {
            long currentTenantId = Long.parseLong(MDC.get(Constants.TENANT_ID));

            // Duplicate check (read-only)
            if (permissionsRepository.isAlreadyExist(
                    createPermission.getName(),
                    Long.parseLong(createPermission.getMenuId()),
                    Long.parseLong(createPermission.getComponentId()),
                    currentTenantId) > 0) {
                throw new BaseException(
                        ResponseCodeEnum.NAME_ALREADY_EXISTS.description(),
                        ResponseCodeEnum.NAME_ALREADY_EXISTS.description(),
                        HttpStatus.BAD_REQUEST,
                        ResponseCodeEnum.NAME_ALREADY_EXISTS.code(),
                        null
                );
            }

            // Validate referenced entities exist (read-only)
            boolean componentExists = componentRepository.findById(Long.parseLong(createPermission.getComponentId())).isPresent();
            boolean menuExists      = menusRepository.findById(Long.parseLong(createPermission.getMenuId())).isPresent();
            boolean tenantExists    = tenantsRepository.findById(currentTenantId).isPresent();

            if (!componentExists || !menuExists || !tenantExists) {
                throw new BaseException(
                        ResponseCodeEnum.BAD_REQUEST.description(),
                        ResponseCodeEnum.BAD_REQUEST.description(),
                        HttpStatus.BAD_REQUEST,
                        ResponseCodeEnum.BAD_REQUEST.code(),
                        null
                );
            }

            // actor: no standard createdBy field on CreatePermission — use a generic identity
            // Adjust this if your CreatePermission DTO gains a createdBy field later.
            String actor = "system";

            // ── Kafka publish (replaces permissionsRepository.save) ───────
            mysqlEventMapper.publishCreatePermission(createPermission, currentTenantId, actor);

            return responseHandler.responseBuilder(
                    ResponseCodeEnum.PERMISSION_CREATION_SUCCESSFUL.code(),
                    ResponseCodeEnum.PERMISSION_CREATION_SUCCESSFUL.description()
            );

        } catch (BaseException ex) {
            throw new BaseException(ex.getMessage(), ex.getReason(), ex.getHttpStatus(), ex.getResultCode(), ex.getStackTraceElements());
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    // =========================================================================
    // editPermission  ← CHANGED: no permissionsRepository.save(); uses Kafka
    // =========================================================================

    @Override
    public CommonAdaptorResp<String> editPermission(EditPermission editPermission) throws BaseException {
        try {
            long currentTenantId = Long.parseLong(MDC.get(Constants.TENANT_ID));

            // Validate permission + referenced entities exist (read-only)
            boolean permissionExists  = permissionsRepository.findById(Long.parseLong(editPermission.getPermissionId())).isPresent();
            boolean componentExists   = componentRepository.findById(Long.parseLong(editPermission.getComponentId())).isPresent();
            boolean menuExists        = menusRepository.findById(Long.parseLong(editPermission.getMenuId())).isPresent();
            boolean tenantExists      = tenantsRepository.findById(currentTenantId).isPresent();

            if (!permissionExists || !componentExists || !menuExists || !tenantExists) {
                throw new BaseException(
                        ResponseCodeEnum.BAD_REQUEST.description(),
                        ResponseCodeEnum.BAD_REQUEST.description(),
                        HttpStatus.BAD_REQUEST,
                        ResponseCodeEnum.BAD_REQUEST.code(),
                        null
                );
            }

            // actor: no standard updatedBy field on EditPermission — use a generic identity
            // Adjust this if your EditPermission DTO gains an updatedBy / actor field.
            String actor = "system";

            // ── Kafka publish (replaces permissionsRepository.save) ───────
            mysqlEventMapper.publishEditPermission(editPermission, currentTenantId, actor);

            return responseHandler.responseBuilder(
                    ResponseCodeEnum.PERMISSION_EDIT_SUCCESSFUL.code(),
                    ResponseCodeEnum.PERMISSION_EDIT_SUCCESSFUL.description()
            );

        } catch (BaseException ex) {
            throw new BaseException(ex.getMessage(), ex.getReason(), ex.getHttpStatus(), ex.getResultCode(), ex.getStackTraceElements());
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    // =========================================================================
    // getFilteredPermissionList — UNCHANGED
    // =========================================================================

    @Override
    public CommonAdaptorResp<List<PermissionView>> getFilteredPermissionList(TableFilterRequest tableFilterRequest) throws BaseException {
        try {
            Map<String, String> filterDataMap = filterValueExtractor.extractFilterValues(tableFilterRequest.getFilterValues());

            String sql = "SELECT\n" +
                    "    TO_CHAR(p.id) AS permissionId,\n" +
                    "    p.name,\n" +
                    "    p.description,\n" +
                    "    c.name AS componentName,\n" +
                    "    m.name AS menuName,\n" +
                    "    COUNT(*) OVER() AS totalCount\n" +
                    "FROM\n" +
                    "    permission p\n" +
                    "LEFT JOIN ums_components c ON c.id = p.component_id\n" +
                    "LEFT JOIN ums_menus m ON m.id = p.menu_id\n" +
                    "WHERE\n" +
                    "    p.tenant_id = :tenantId\n" +
                    "    AND (\n" +
                    "        (:permissionId IS NULL AND :permissionIdFilterType IS NULL) OR\n" +
                    "        (:permissionIdFilterType = 'Equal' AND TO_CHAR(p.id) = :permissionId) OR\n" +
                    "        (:permissionIdFilterType = 'Include' AND TO_CHAR(p.id) LIKE '%' || :permissionId || '%') OR\n" +
                    "        (:permissionIdFilterType = 'Is empty' AND (p.id IS NULL OR p.id = ''))\n" +
                    "    )\n" +
                    "    AND (\n" +
                    "        (:name IS NULL AND :nameFilterType IS NULL) OR\n" +
                    "        (:nameFilterType = 'Equal' AND LOWER(p.name) = LOWER(:name)) OR\n" +
                    "        (:nameFilterType = 'Include' AND LOWER(p.name) LIKE '%' || LOWER(:name) || '%') OR\n" +
                    "        (:nameFilterType = 'Is empty' AND (p.name IS NULL OR p.name = ''))\n" +
                    "    )\n" +
                    "    AND (\n" +
                    "        (:description IS NULL AND :descriptionFilterType IS NULL) OR\n" +
                    "        (:descriptionFilterType = 'Equal' AND LOWER(p.description) = LOWER(:description)) OR\n" +
                    "        (:descriptionFilterType = 'Include' AND LOWER(p.description) LIKE '%' || LOWER(:description) || '%') OR\n" +
                    "        (:descriptionFilterType = 'Is empty' AND (p.description IS NULL OR p.description = ''))\n" +
                    "    )\n" +
                    "    AND (\n" +
                    "        (:componentName IS NULL AND :componentNameFilterType IS NULL) OR\n" +
                    "        (:componentNameFilterType = 'Equal' AND LOWER(c.name) = LOWER(:componentName)) OR\n" +
                    "        (:componentNameFilterType = 'Include' AND LOWER(c.name) LIKE '%' || LOWER(:componentName) || '%') OR\n" +
                    "        (:componentNameFilterType = 'Is empty' AND (c.name IS NULL OR c.name = ''))\n" +
                    "    )\n" +
                    "    AND (\n" +
                    "        (:menuName IS NULL AND :menuNameFilterType IS NULL) OR\n" +
                    "        (:menuNameFilterType = 'Equal' AND LOWER(m.name) = LOWER(:menuName)) OR\n" +
                    "        (:menuNameFilterType = 'Include' AND LOWER(m.name) LIKE '%' || LOWER(:menuName) || '%') OR\n" +
                    "        (:menuNameFilterType = 'Is empty' AND (m.name IS NULL OR m.name = ''))\n" +
                    "    )\n" +
                    "ORDER BY\n" +
                    "    p.id DESC\n" +
                    "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";

            List<Object[]> results = entityManager.createNativeQuery(sql)
                    .setParameter("permissionId",          filterDataMap.get("permissionId"))
                    .setParameter("permissionId" + Constants.FILTER_TYPE, filterDataMap.get("permissionId" + Constants.FILTER_TYPE))
                    .setParameter("name",                  filterDataMap.get("name"))
                    .setParameter("name" + Constants.FILTER_TYPE, filterDataMap.get("name" + Constants.FILTER_TYPE))
                    .setParameter("description",           filterDataMap.get("description"))
                    .setParameter("description" + Constants.FILTER_TYPE, filterDataMap.get("description" + Constants.FILTER_TYPE))
                    .setParameter("menuName",              filterDataMap.get("menuName"))
                    .setParameter("menuName" + Constants.FILTER_TYPE, filterDataMap.get("menuName" + Constants.FILTER_TYPE))
                    .setParameter("componentName",         filterDataMap.get("componentName"))
                    .setParameter("componentName" + Constants.FILTER_TYPE, filterDataMap.get("componentName" + Constants.FILTER_TYPE))
                    .setParameter("limit",                 tableFilterRequest.getLimit())
                    .setParameter("offset",                tableFilterRequest.getOffset())
                    .setParameter("tenantId",              Long.parseLong(MDC.get(Constants.TENANT_ID)))
                    .getResultList();

            List<PermissionView> views = results.stream()
                    .map(row -> PermissionView.builder()
                            .permissionId((String) row[0])
                            .name((String) row[1])
                            .description((String) row[2])
                            .componentName((String) row[3])
                            .menuName((String) row[4])
                            .totalCount(((Number) row[5]).intValue())
                            .build())
                    .collect(Collectors.toList());

            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), views,
                    PageUtils.buildPageDetails(tableFilterRequest.getLimit(), tableFilterRequest.getOffset(),
                            !views.isEmpty() ? views.get(0).getTotalCount() : 0, views.size()));
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }
}