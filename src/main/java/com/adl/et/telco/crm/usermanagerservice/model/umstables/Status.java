package com.adl.et.telco.crm.usermanagerservice.model.umstables;

import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import lombok.*;

import jakarta.persistence.*;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@Table(name = "ums_status")
@AllArgsConstructor
@NoArgsConstructor
@SqlResultSetMapping(
        name = "getUserStatusMapping",
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
        name = "Status.getUserStatus",
        resultSetMapping = "getUserStatusMapping",
        query = "select TO_CHAR(s.id) as value , s.name as label from ums_status s where s.id <> 4"
)

public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @OneToMany(mappedBy = "status", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Users> userList;

    @OneToMany(mappedBy = "status", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Permission> permissionList;

    @OneToMany(mappedBy = "status", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Roles> rolesList;
}

