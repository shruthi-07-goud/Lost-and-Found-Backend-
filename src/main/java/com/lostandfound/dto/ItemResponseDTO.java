package com.lostandfound.dto;

import com.lostandfound.model.Item;
import com.lostandfound.model.ItemStatus;
import com.lostandfound.model.ItemType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ItemResponseDTO {
    private Long id;
    private ItemType type;
    private String title;
    private String description;
    private String category;
    private String location;
    private LocalDate dateReported;
    private String photoUrl;
    private ItemStatus status;
    private UserDTO reportedBy;
    private boolean canEdit;
    private boolean canClaim;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ItemResponseDTO() {
    }

    public static ItemResponseDTO fromItem(Item item, Long currentUserId, boolean hasApprovedClaim) {
        if (item == null) return null;

        ItemResponseDTO dto = new ItemResponseDTO();
        dto.setId(item.getId());
        dto.setType(item.getType());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setCategory(item.getCategory());
        dto.setLocation(item.getLocation());
        dto.setDateReported(item.getDateReported());
        dto.setPhotoUrl(item.getPhotoUrl());
        dto.setStatus(item.getStatus());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());

        boolean isReporter = currentUserId != null && item.getReportedBy() != null && currentUserId.equals(item.getReportedBy().getId());
        boolean allowContactDetails = isReporter || hasApprovedClaim;

        if (item.getReportedBy() != null) {
            dto.setReportedBy(UserDTO.fromUser(item.getReportedBy(), allowContactDetails));
        }

        dto.setCanEdit(isReporter);
        dto.setCanClaim(currentUserId != null && !isReporter && item.getStatus() != ItemStatus.CLAIMED && item.getStatus() != ItemStatus.RESOLVED);

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getDateReported() {
        return dateReported;
    }

    public void setDateReported(LocalDate dateReported) {
        this.dateReported = dateReported;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public UserDTO getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(UserDTO reportedBy) {
        this.reportedBy = reportedBy;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCanClaim() {
        return canClaim;
    }

    public void setCanClaim(boolean canClaim) {
        this.canClaim = canClaim;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
