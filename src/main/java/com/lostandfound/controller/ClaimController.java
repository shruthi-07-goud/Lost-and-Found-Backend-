package com.lostandfound.controller;

import com.lostandfound.dto.ClaimRequestDTO;
import com.lostandfound.dto.ClaimResponseDTO;
import com.lostandfound.model.User;
import com.lostandfound.service.ClaimService;
import com.lostandfound.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;
    private final UserService userService;

    @Autowired
    public ClaimController(ClaimService claimService, UserService userService) {
        this.claimService = claimService;
        this.userService = userService;
    }

    /**
     * Raise a claim on an item with verification proof.
     */
    @PostMapping
    public ResponseEntity<ClaimResponseDTO> createClaim(@Valid @RequestBody ClaimRequestDTO request) {
        User currentUser = userService.getCurrentUser();
        ClaimResponseDTO claim = claimService.createClaim(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(claim);
    }

    /**
     * Approve a claim. Only the item's original reporter can approve.
     * Reveals mutual contact details in the response.
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ClaimResponseDTO> approveClaim(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        ClaimResponseDTO claim = claimService.approveClaim(id, currentUser);
        return ResponseEntity.ok(claim);
    }

    /**
     * Reject a claim. Only the item's original reporter can reject.
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<ClaimResponseDTO> rejectClaim(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        ClaimResponseDTO claim = claimService.rejectClaim(id, currentUser);
        return ResponseEntity.ok(claim);
    }

    /**
     * Get claims made by and received by the logged-in user.
     */
    @GetMapping("/mine")
    public ResponseEntity<Map<String, List<ClaimResponseDTO>>> getMyClaims() {
        User currentUser = userService.getCurrentUser();
        List<ClaimResponseDTO> sent = claimService.getClaimsMadeByMe(currentUser);
        List<ClaimResponseDTO> received = claimService.getClaimsReceivedByMe(currentUser);

        Map<String, List<ClaimResponseDTO>> result = new HashMap<>();
        result.put("sentClaims", sent);
        result.put("receivedClaims", received);

        return ResponseEntity.ok(result);
    }

    /**
     * Get all claims for a specific item (reporter only).
     */
    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<ClaimResponseDTO>> getClaimsForItem(@PathVariable Long itemId) {
        User currentUser = userService.getCurrentUser();
        List<ClaimResponseDTO> claims = claimService.getClaimsForItem(itemId, currentUser);
        return ResponseEntity.ok(claims);
    }
}
