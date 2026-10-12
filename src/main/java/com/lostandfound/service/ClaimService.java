package com.lostandfound.service;

import com.lostandfound.dto.ClaimRequestDTO;
import com.lostandfound.dto.ClaimResponseDTO;
import com.lostandfound.exception.BadRequestException;
import com.lostandfound.exception.ResourceNotFoundException;
import com.lostandfound.exception.UnauthorizedException;
import com.lostandfound.model.*;
import com.lostandfound.repository.ClaimRepository;
import com.lostandfound.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ClaimService: Claim Verification Workflow & Privacy Protection
 * ---------------------------------------------------------------
 * This service implements the multi-step verification and handover process
 * while guaranteeing privacy protection across user profiles.
 *
 * Core Privacy & Workflow Rules:
 *  1. Anti-Self-Claim:
 *     - A student who posted an item report CANNOT submit a claim on their own item.
 *  2. Confidentiality & Contact Protection (Zero-Leakage Guarantee):
 *     - Before approval, both claimant and reporter's contact details (phone number,
 *       email address) are strictly masked (returned as null) in all public endpoints
 *       and pending claim payloads.
 *     - Only when the original reporter explicitly APPROVES the claim are the mutual
 *       contact details exposed to both parties, enabling safe contact and physical reunion.
 *  3. Status Progression:
 *     - When a claim is approved, the associated Item's status transitions from
 *       OPEN/MATCHED to CLAIMED.
 *     - Any competing pending claims on that item are marked as REJECTED to prevent
 *       multiple conflicting claims.
 *  4. Authorization Guard:
 *     - Only the original reporter (or an administrator) has the authority to approve
 *       or reject a claim submitted for their item.
 */
@Service
public class ClaimService {

    private static final Logger logger = LoggerFactory.getLogger(ClaimService.class);

    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;

    @Autowired
    public ClaimService(ClaimRepository claimRepository, ItemRepository itemRepository) {
        this.claimRepository = claimRepository;
        this.itemRepository = itemRepository;
    }

    /**
     * Submit a new claim on an item with proof description.
     */
    @Transactional
    public ClaimResponseDTO createClaim(ClaimRequestDTO request, User currentUser) {
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + request.getItemId()));

        // Rule 1: A user cannot claim their own item
        if (item.getReportedBy().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You cannot claim an item you reported yourself.");
        }

        // Rule 2: Cannot claim an already claimed or resolved item
        if (item.getStatus() == ItemStatus.CLAIMED || item.getStatus() == ItemStatus.RESOLVED) {
            throw new BadRequestException("This item has already been claimed or resolved.");
        }

        // Rule 3: Prevent multiple pending claims from the same user on the same item
        if (claimRepository.existsByItemIdAndClaimedByIdAndStatus(item.getId(), currentUser.getId(), ClaimStatus.PENDING)) {
            throw new BadRequestException("You already have an active pending claim for this item.");
        }

        Claim claim = new Claim(item, currentUser, request.getMessage().trim());
        Claim savedClaim = claimRepository.save(claim);

        logger.info("New claim #{} created on item #{} by user #{}", savedClaim.getId(), item.getId(), currentUser.getId());
        return ClaimResponseDTO.fromClaim(savedClaim, currentUser.getId());
    }

    /**
     * Approve a claim:
     * - Verifies reporter permission.
     * - Updates Claim status to APPROVED.
     * - Updates Item status to CLAIMED.
     * - Rejects any other competing pending claims on this item.
     * - Exposes mutual contact details in the response.
     */
    @Transactional
    public ClaimResponseDTO approveClaim(Long claimId, User currentUser) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        Item item = claim.getItem();

        // Rule 4: Only the item's original reporter (or admin) can approve/reject a claim on it
        if (!item.getReportedBy().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("Only the original reporter can approve claims on this item.");
        }

        if (claim.getStatus() == ClaimStatus.APPROVED) {
            return ClaimResponseDTO.fromClaim(claim, currentUser.getId());
        }

        // Update claim to APPROVED
        claim.setStatus(ClaimStatus.APPROVED);
        Claim updatedClaim = claimRepository.save(claim);

        // Update item status to CLAIMED
        item.setStatus(ItemStatus.CLAIMED);
        itemRepository.save(item);

        // Reject competing pending claims on this item
        List<Claim> otherClaims = claimRepository.findByItemIdAndStatusNot(item.getId(), ClaimStatus.APPROVED);
        for (Claim other : otherClaims) {
            if (other.getStatus() == ClaimStatus.PENDING) {
                other.setStatus(ClaimStatus.REJECTED);
                claimRepository.save(other);
            }
        }

        logger.info("Claim #{} APPROVED by reporter #{}. Mutual contact details now unlocked.", claim.getId(), currentUser.getId());
        return ClaimResponseDTO.fromClaim(updatedClaim, currentUser.getId());
    }

    /**
     * Reject a claim:
     * - Verifies reporter permission.
     * - Updates Claim status to REJECTED.
     */
    @Transactional
    public ClaimResponseDTO rejectClaim(Long claimId, User currentUser) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        Item item = claim.getItem();

        // Rule 4: Only the item's original reporter (or admin) can approve/reject a claim on it
        if (!item.getReportedBy().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("Only the original reporter can reject claims on this item.");
        }

        claim.setStatus(ClaimStatus.REJECTED);
        Claim updatedClaim = claimRepository.save(claim);

        logger.info("Claim #{} REJECTED by reporter #{}", claim.getId(), currentUser.getId());
        return ClaimResponseDTO.fromClaim(updatedClaim, currentUser.getId());
    }

    /**
     * Get claims submitted by the logged-in user.
     */
    @Transactional(readOnly = true)
    public List<ClaimResponseDTO> getClaimsMadeByMe(User currentUser) {
        return claimRepository.findByClaimedByIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(c -> ClaimResponseDTO.fromClaim(c, currentUser.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Get claims received on items reported by the logged-in user.
     */
    @Transactional(readOnly = true)
    public List<ClaimResponseDTO> getClaimsReceivedByMe(User currentUser) {
        return claimRepository.findByItemReportedByIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(c -> ClaimResponseDTO.fromClaim(c, currentUser.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Get claims for a specific item (only viewable by the item's reporter).
     */
    @Transactional(readOnly = true)
    public List<ClaimResponseDTO> getClaimsForItem(Long itemId, User currentUser) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        if (!item.getReportedBy().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("Only the reporter can view claims for this item.");
        }

        return claimRepository.findByItemIdOrderByCreatedAtDesc(itemId)
                .stream()
                .map(c -> ClaimResponseDTO.fromClaim(c, currentUser.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Check if a specific user has an approved claim on the item.
     */
    @Transactional(readOnly = true)
    public boolean hasApprovedClaim(Long itemId, Long userId) {
        if (itemId == null || userId == null) return false;
        return claimRepository.findByItemIdAndStatus(itemId, ClaimStatus.APPROVED)
                .map(c -> c.getClaimedBy().getId().equals(userId))
                .orElse(false);
    }
}
