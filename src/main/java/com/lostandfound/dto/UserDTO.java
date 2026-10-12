package com.lostandfound.dto;

import com.lostandfound.model.Role;
import com.lostandfound.model.User;

public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private Role role;

    public UserDTO() {
    }

    public UserDTO(Long id, String name, String email, String phoneNumber, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    public static UserDTO fromUser(User user, boolean includeContactDetails) {
        if (user == null) return null;
        return new UserDTO(
                user.getId(),
                user.getName(),
                includeContactDetails ? user.getEmail() : null,
                includeContactDetails ? user.getPhoneNumber() : null,
                user.getRole()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
