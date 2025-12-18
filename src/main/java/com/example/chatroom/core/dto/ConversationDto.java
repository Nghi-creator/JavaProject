package com.example.chatroom.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public class ConversationDto {

    private Integer id;
    private String type; // PRIVATE or GROUP, matches server
    private String name;
    private Boolean isEncrypted;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") // ensures ISO format
    private LocalDateTime createdAt;

    private List<MemberDto> members; // full member info from server
    private MessageDto lastMessage;   // last message info

    // --- no-args constructor for Jackson ---
    public ConversationDto() {}

    // --- getters & setters ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getIsEncrypted() { return isEncrypted; }
    public void setIsEncrypted(Boolean isEncrypted) { this.isEncrypted = isEncrypted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<MemberDto> getMembers() { return members; }
    public void setMembers(List<MemberDto> members) { this.members = members; }

    public MessageDto getLastMessage() { return lastMessage; }
    public void setLastMessage(MessageDto lastMessage) { this.lastMessage = lastMessage; }

    // --- nested MemberDto class ---
    public static class MemberDto {
        private Integer id;
        private String username;
        private String fullName;

        public MemberDto() {} // no-args constructor for Jackson

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
    }
}