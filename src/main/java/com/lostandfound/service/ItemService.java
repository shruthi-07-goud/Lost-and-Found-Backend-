package com.lostandfound.service;

import com.lostandfound.dto.ItemRequestDTO;
import com.lostandfound.dto.ItemResponseDTO;
import com.lostandfound.exception.ResourceNotFoundException;
import com.lostandfound.exception.UnauthorizedException;
import com.lostandfound.model.Item;
import com.lostandfound.model.ItemStatus;
import com.lostandfound.model.ItemType;
import com.lostandfound.model.Role;
import com.lostandfound.model.User;
import com.lostandfound.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final MatchingService matchingService;
    private final ClaimService claimService;

    @Autowired
    public ItemService(ItemRepository itemRepository,
                       MatchingService matchingService,
                       ClaimService claimService) {
        this.itemRepository = itemRepository;
        this.matchingService = matchingService;
        this.claimService = claimService;
    }

    @Transactional
    public ItemResponseDTO createItem(ItemRequestDTO request, User currentUser) {
        Item item = new Item();
        item.setType(request.getType());
        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription().trim());
        item.setCategory(request.getCategory().trim());
        item.setLocation(request.getLocation().trim());
        item.setDateReported(request.getDateReported() != null ? request.getDateReported() : LocalDate.now());
        item.setPhotoUrl(request.getPhotoUrl());
        item.setStatus(ItemStatus.OPEN);
        item.setReportedBy(currentUser);

        Item savedItem = itemRepository.save(item);

        // Run smart item matching on creation to detect early matches
        matchingService.evaluateMatchesOnItemCreation(savedItem);

        return ItemResponseDTO.fromItem(savedItem, currentUser.getId(), false);
    }

    @Transactional(readOnly = true)
    public List<ItemResponseDTO> getItems(ItemType type, String category, String location,
                                         ItemStatus status, String search, User currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        // Normalize empty string parameters to null for query flexibility
        String normCategory = (category != null && !category.isBlank()) ? category.trim() : null;
        String normLocation = (location != null && !location.isBlank()) ? location.trim() : null;
        String normSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        List<Item> items = itemRepository.searchItems(type, normCategory, normLocation, status, normSearch);

        return items.stream()
                .map(item -> {
                    boolean hasApproved = currentUserId != null && claimService.hasApprovedClaim(item.getId(), currentUserId);
                    return ItemResponseDTO.fromItem(item, currentUserId, hasApproved);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ItemResponseDTO getItemById(Long id, User currentUser) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        boolean hasApproved = currentUserId != null && claimService.hasApprovedClaim(item.getId(), currentUserId);

        return ItemResponseDTO.fromItem(item, currentUserId, hasApproved);
    }

    @Transactional(readOnly = true)
    public Item getItemEntity(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ItemResponseDTO> getMyReportedItems(User currentUser) {
        return itemRepository.findByReportedByIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(item -> ItemResponseDTO.fromItem(item, currentUser.getId(), false))
                .collect(Collectors.toList());
    }

    @Transactional
    public ItemResponseDTO updateItem(Long id, ItemRequestDTO request, User currentUser) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        // Role-based authorization check: Only the reporter or Admin can edit
        if (!item.getReportedBy().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("You can only edit items you reported.");
        }

        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription().trim());
        item.setCategory(request.getCategory().trim());
        item.setLocation(request.getLocation().trim());
        if (request.getType() != null) {
            item.setType(request.getType());
        }
        if (request.getDateReported() != null) {
            item.setDateReported(request.getDateReported());
        }
        if (request.getPhotoUrl() != null) {
            item.setPhotoUrl(request.getPhotoUrl());
        }

        Item updated = itemRepository.save(item);
        return ItemResponseDTO.fromItem(updated, currentUser.getId(), false);
    }

    @Transactional
    public void deleteItem(Long id, User currentUser) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        // Role-based authorization check: Only the reporter or Admin can delete
        if (!item.getReportedBy().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("You can only delete items you reported.");
        }

        itemRepository.delete(item);
    }
}
