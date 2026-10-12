package com.lostandfound.controller;

import com.lostandfound.dto.ApiResponse;
import com.lostandfound.dto.ItemRequestDTO;
import com.lostandfound.dto.ItemResponseDTO;
import com.lostandfound.model.ItemStatus;
import com.lostandfound.model.ItemType;
import com.lostandfound.model.User;
import com.lostandfound.service.ItemService;
import com.lostandfound.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final UserService userService;

    @Autowired
    public ItemController(ItemService itemService, UserService userService) {
        this.itemService = itemService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<ItemResponseDTO>> getItems(
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) String search
    ) {
        User currentUser = userService.getCurrentUserOrNull();
        List<ItemResponseDTO> items = itemService.getItems(type, category, location, status, search, currentUser);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDTO> getItemById(@PathVariable Long id) {
        User currentUser = userService.getCurrentUserOrNull();
        ItemResponseDTO item = itemService.getItemById(id, currentUser);
        return ResponseEntity.ok(item);
    }

    @PostMapping
    public ResponseEntity<ItemResponseDTO> createItem(@Valid @RequestBody ItemRequestDTO request) {
        User currentUser = userService.getCurrentUser();
        ItemResponseDTO created = itemService.createItem(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemResponseDTO> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemRequestDTO request
    ) {
        User currentUser = userService.getCurrentUser();
        ItemResponseDTO updated = itemService.updateItem(id, request, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        itemService.deleteItem(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Item deleted successfully"));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ItemResponseDTO>> getMyReportedItems() {
        User currentUser = userService.getCurrentUser();
        List<ItemResponseDTO> items = itemService.getMyReportedItems(currentUser);
        return ResponseEntity.ok(items);
    }
}
