package com.lostandfound.controller;

import com.lostandfound.dto.MatchResultDTO;
import com.lostandfound.model.Item;
import com.lostandfound.model.User;
import com.lostandfound.service.ItemService;
import com.lostandfound.service.MatchingService;
import com.lostandfound.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class MatchingController {

    private final ItemService itemService;
    private final MatchingService matchingService;
    private final UserService userService;

    @Autowired
    public MatchingController(ItemService itemService,
                              MatchingService matchingService,
                              UserService userService) {
        this.itemService = itemService;
        this.matchingService = matchingService;
        this.userService = userService;
    }

    /**
     * GET /api/items/{id}/matches
     * For a given item (LOST or FOUND), returns a ranked list of opposite-type candidates
     * scored and sorted by compatibility using the smart item matching algorithm.
     */
    @GetMapping("/{id}/matches")
    public ResponseEntity<List<MatchResultDTO>> getMatchesForItem(@PathVariable Long id) {
        Item item = itemService.getItemEntity(id);
        User currentUser = userService.getCurrentUserOrNull();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        List<MatchResultDTO> matches = matchingService.findMatchesForItem(item, currentUserId);
        return ResponseEntity.ok(matches);
    }
}
