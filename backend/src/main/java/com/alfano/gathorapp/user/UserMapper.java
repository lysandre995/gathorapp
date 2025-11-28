package com.alfano.gathorapp.user;

import com.alfano.gathorapp.user.dto.CreateUserRequest;
import com.alfano.gathorapp.user.dto.UserResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for conversion between Entity User and DTO.
 */
@Component
public class UserMapper {

    /**
     * Converts User entity in UserResponse DTO.
     */
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Converts CreateUserRequest DTO in User entity.
     */
    public User toEntity(CreateUserRequest request) {
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .build();
    }
}
