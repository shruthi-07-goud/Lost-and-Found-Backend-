package com.lostandfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ClaimRequestDTO {

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotBlank(message = "Claim message / proof description is required")
    @Size(min = 10, max = 1500, message = "Message must be between 10 and 1500 characters")
    private String message;

    public ClaimRequestDTO() {
    }

    public ClaimRequestDTO(Long itemId, String message) {
        this.itemId = itemId;
        this.message = message;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
