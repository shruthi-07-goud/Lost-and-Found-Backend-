package com.lostandfound.dto;

import com.lostandfound.model.Claim;
import com.lostandfound.model.ClaimStatus;
import com.lostandfound.model.Item;
import com.lostandfound.model.ItemStatus;
import com.lostandfound.model.ItemType;
import java.time.LocalDateTime;

public class ClaimResponseDTO {
    private Long id;
    private Long itemId;
    private String itemTitle;
    private ItemType itemType;
    private String itemCategory;
    private String itemLocation;
    private String itemPhotoUrl;
    private ItemStatus itemStatus;
    private UserDTO claimedBy;
    private UserDTO reporter;
    private ClaimStatus status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean contactRevealed;

    public ClaimResponseDTO() {
    }

    public static ClaimResponseDTO fromClaim(Claim claim, Long currentUserId) {
        if (claim == null) return null;

        ClaimResponseDTO dto = new ClaimResponseDTO();
        dto.setId(claim.getId());

        Item item = claim.getItem();
        if (item != null) {
            dto.setItemId(item.getId());
            dto.setItemTitle(item.getTitle());
            dto.setItemType(item.getType());
            dto.setItemCategory(item.getCategory());
            dto.setItemLocation(item.getLocation());
            dto.setItemPhotoUrl(item.getPhotoUrl());
            dto.setItemStatus(item.getStatus());
        }

        dto.setStatus(claim.getStatus());
        dto.setMessage(claim.getMessage());
        dto.setCreatedAt(claim.getCreatedAt());
        dto.setUpdatedAt(claim.getUpdatedAt());

        // PRIVACY ENFORCEMENT:
        // Only when the claim is APPROVED can the claimant and reporter see each other's contact details (phone and email).
        boolean isApproved = claim.getStatus() == ClaimStatus.APPROVED;
        dto.setContactRevealed(isApproved);

        if (claim.getClaimedBy() != null) {
            dto.setClaimedBy(UserDTO.fromUser(claim.getClaimedBy(), isApproved));
        }

        if (item != null && item.getReportedBy() != null) {
            dto.setReporter(UserDTO.fromUser(item.getReportedBy(), isApproved));
        }

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }

    public String getItemLocation() {
        return itemLocation;
    }

    public void setItemLocation(String itemLocation) {
        this.itemLocation = itemLocation;
    }

    public String getItemPhotoUrl() {
        return itemPhotoUrl;
    }

    public void setItemPhotoUrl(String itemPhotoUrl) {
        this.itemPhotoUrl = itemPhotoUrl;
    }

    public ItemStatus getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(ItemStatus itemStatus) {
        this.itemStatus = itemStatus;
    }

    public UserDTO getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(UserDTO claimedBy) {
        this.claimedBy = claimedBy;
    }

    public UserDTO getReporter() {
        return reporter;
    }

    public void setReporter(UserDTO reporter) {
        this.reporter = reporter;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public boolean isContactRevealed() {
        return contactRevealed;
    }

    public void setContactRevealed(boolean contactRevealed) {
        this.contactRevealed = contactRevealed;
    }
}
