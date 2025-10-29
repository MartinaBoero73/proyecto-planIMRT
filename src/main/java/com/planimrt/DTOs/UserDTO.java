package com.planimrt.DTOs;

import com.planimrt.model.UserRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private UserRole role;

    public UserDTO(Long id, String username, UserRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }
}
