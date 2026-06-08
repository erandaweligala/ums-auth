package com.adl.et.telco.crm.usermanagerservice.model.metadata;

import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;

@Getter
@Setter
@ToString
@Entity
@Table(name = "ums_default_group")
@SqlResultSetMapping(
        name = "getDefaultGroupMetaDataMapping",
        classes = {
                @ConstructorResult(
                        targetClass = MetaData.class,
                        columns = {
                                @ColumnResult(name = "label"),
                                @ColumnResult(name = "value")
                        }
                )
        }
)


@NamedNativeQuery(
        name = "DefaultGroup.getDefaultGroupMetaData",
        resultSetMapping = "getDefaultGroupMetaDataMapping",
        query = "select TO_CHAR(u.id) as value , u.name as label from ums_default_group u"
)
public class DefaultGroup {
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Long id;
    private String name;
    private String description;
}
