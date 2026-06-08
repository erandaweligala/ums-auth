package com.adl.et.telco.crm.usermanagerservice.dto.permission;

import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AllActions {
    private List<MainActionsItem> mainActions;
}
