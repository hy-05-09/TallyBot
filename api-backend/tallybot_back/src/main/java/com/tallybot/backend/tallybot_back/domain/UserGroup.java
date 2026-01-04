package com.tallybot.backend.tallybot_back.domain;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_group")
public class UserGroup {
    @Id
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "group_name", nullable = false, columnDefinition = "VARCHAR(50)")
    private String groupName;

    public static UserGroup create(Long groupId, String groupName) {
        if (groupId == null) {
            throw new IllegalArgumentException("groupId cannot be null");
        }
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("groupName cannot be blank");
        }

        UserGroup group = new UserGroup();
        group.groupId = groupId;
        group.groupName = groupName;
        return group;
    }
}
