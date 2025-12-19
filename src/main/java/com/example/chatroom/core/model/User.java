package com.example.chatroom.core.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {

    private Integer id;
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    private String username;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    private String fullName;
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    private String email;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    private String gender;
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    private LocalDate dob;
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    private String address;
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    private LocalDateTime createdAt;
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User() {}

    public User(
            Integer id,
            String username,
            String fullName,
            String email,
            String gender,
            LocalDate dob,
            String address,
            String status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.gender = gender;
        this.dob = dob;
        this.address = address;
        this.status = status;
        this.createdAt = createdAt;
    }
}
