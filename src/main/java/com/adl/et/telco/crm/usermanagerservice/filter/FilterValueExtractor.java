package com.adl.et.telco.crm.usermanagerservice.filter;

import com.adl.et.telco.crm.usermanagerservice.dto.user.FilterValue;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class FilterValueExtractor {

    public Map<String, String> extractFilterValues(List<FilterValue> filterValues) {
        Map<String, String> filterDataMap = new HashMap<>();
        if (filterValues == null || filterValues.isEmpty()) {
            return filterDataMap;
        }
        for (FilterValue filterValue : filterValues) {
            filterDataMap.put(filterValue.getColumnName(), (filterValue.getValue() != null && filterValue.getValue().length > 0) ? filterValue.getValue()[0] : null);
            filterDataMap.put(filterValue.getColumnName() + Constants.FILTER_TYPE, filterValue.getOperation());
        }

        return filterDataMap;
    }

    public List<String> findStatusList(List<FilterValue> filterValues) {
        if (filterValues != null && !filterValues.isEmpty()) {
            for (FilterValue filterValue : filterValues) {
                if (filterValue.getColumnName() != null && filterValue.getColumnName().equalsIgnoreCase("status")){
                    return List.of(filterValue.getValue());
                }
            }
        }
        return new ArrayList<>(Arrays.asList("Active", "Blocked", "Expired"));
    }
}

