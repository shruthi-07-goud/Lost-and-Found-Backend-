package com.lostandfound.service;

import com.lostandfound.dto.ItemResponseDTO;
import com.lostandfound.dto.MatchResultDTO;
import com.lostandfound.model.Item;
import com.lostandfound.model.ItemStatus;
import com.lostandfound.model.ItemType;
import com.lostandfound.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * MatchingService: Core Smart Item Matching Engine
 * ------------------------------------------------
 * This service implements a deterministic rule-based scoring algorithm in pure Java
 * (no external machine learning dependencies required) to calculate compatibility
 * between LOST and FOUND items reported in a campus or hostel community.
 *
 * Scoring Rules:
 *  1. Category Match: +3 points
 *     - Lost and found items within the exact same category (e.g. Electronics, Keys)
 *       have a high base probability of being the same item.
 *  2. Location Match: +2 points
 *     - Case-insensitive exact string match or containment (e.g. "Library 2nd Floor"
 *       matches "Library").
 *  3. Shared Keywords: +1 point per unique matching keyword
 *     - Descriptions and titles are tokenized into normalized word tokens.
 *     - Stop words (common grammar particles: "the", "a", "an", "is", "in", etc.)
 *       are filtered out to prevent false-positive inflation.
 *     - Every remaining meaningful keyword (e.g. "Casio", "calculator", "silver", "water bottle")
 *       present in both items increments the score.
 *  4. Thresholding:
 *     - Only candidates achieving total score >= MATCH_THRESHOLD (3 points) are returned.
 *     - Results are ranked in descending order of compatibility score.
 */
@Service
public class MatchingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchingService.class);
    private static final int MATCH_THRESHOLD = 3;
    private static final Pattern WORD_SPLIT_PATTERN = Pattern.compile("[\\s,.:;!?'\"()\\[\\]{}\\-_/\\\\]+");

    // Standard English stop words to exclude from keyword extraction
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "all", "am", "an", "and", "any", "are", "as", "at",
            "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
            "could", "did", "do", "does", "doing", "down", "during",
            "each", "few", "for", "from", "further",
            "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him", "himself", "his", "how",
            "i", "if", "in", "into", "is", "it", "its", "itself",
            "just", "me", "more", "most", "my", "myself",
            "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own",
            "same", "she", "should", "so", "some", "such",
            "than", "that", "the", "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too",
            "under", "until", "up", "very",
            "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why", "with", "would",
            "you", "your", "yours", "yourself", "yourselves",
            // Lost and found context specific filler words
            "lost", "found", "item", "please", "help", "anyone", "contact", "hostel", "campus"
    );

    private final ItemRepository itemRepository;

    @Autowired
    public MatchingService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /**
     * Find and rank opposite-type items that match the given target item.
     *
     * @param targetItem The item to find matches for (LOST or FOUND)
     * @param currentUserId The currently logged-in user ID (for DTO permission flags)
     * @return Ranked list of MatchResultDTO with scores and item details
     */
    @Transactional(readOnly = true)
    public List<MatchResultDTO> findMatchesForItem(Item targetItem, Long currentUserId) {
        if (targetItem == null) {
            return Collections.emptyList();
        }

        // Determine opposite type: LOST items match with FOUND items, and vice versa
        ItemType oppositeType = (targetItem.getType() == ItemType.LOST) ? ItemType.FOUND : ItemType.LOST;

        // Query active candidates (OPEN or MATCHED)
        List<ItemStatus> eligibleStatuses = Arrays.asList(ItemStatus.OPEN, ItemStatus.MATCHED);
        List<Item> candidates = itemRepository.findByTypeAndStatusIn(oppositeType, eligibleStatuses);

        List<MatchResultDTO> results = new ArrayList<>();

        for (Item candidate : candidates) {
            // Cannot match an item with itself or an item reported by the same user if desired,
            // but we allow cross-checking if reported by different accounts
            if (candidate.getId().equals(targetItem.getId())) {
                continue;
            }

            int score = 0;
            List<String> reasons = new ArrayList<>();

            // Rule 1: Category Match (+3 points)
            if (isCategoryMatch(targetItem.getCategory(), candidate.getCategory())) {
                score += 3;
                reasons.add("Same Category: " + candidate.getCategory() + " (+3 pts)");
            }

            // Rule 2: Location Match (+2 points)
            if (isLocationMatch(targetItem.getLocation(), candidate.getLocation())) {
                score += 2;
                reasons.add("Matching/Nearby Location: \"" + candidate.getLocation() + "\" (+2 pts)");
            }

            // Rule 3: Shared Keywords in Title & Description (+1 point per keyword)
            Set<String> targetKeywords = extractKeywords(targetItem.getTitle() + " " + targetItem.getDescription());
            Set<String> candidateKeywords = extractKeywords(candidate.getTitle() + " " + candidate.getDescription());

            Set<String> intersection = new HashSet<>(targetKeywords);
            intersection.retainAll(candidateKeywords);

            if (!intersection.isEmpty()) {
                int keywordPoints = intersection.size();
                score += keywordPoints;
                reasons.add("Shared Keywords: [" + String.join(", ", intersection) + "] (+" + keywordPoints + " pts)");
            }

            // Rule 4: Threshold evaluation
            if (score >= MATCH_THRESHOLD) {
                ItemResponseDTO candidateDTO = ItemResponseDTO.fromItem(candidate, currentUserId, false);
                results.add(new MatchResultDTO(candidateDTO, score, reasons));
            }
        }

        // Sort descending by match score
        results.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        return results;
    }

    /**
     * Inspect new item against opposite-type items and automatically mark as MATCHED
     * if strong matches exist.
     */
    @Transactional
    public void evaluateMatchesOnItemCreation(Item newItem) {
        try {
            List<MatchResultDTO> matches = findMatchesForItem(newItem, null);
            if (!matches.isEmpty()) {
                // If candidate has a score of 4 or higher (e.g. category + location or keywords)
                if (matches.get(0).getScore() >= 4 && newItem.getStatus() == ItemStatus.OPEN) {
                    newItem.setStatus(ItemStatus.MATCHED);
                    itemRepository.save(newItem);
                    logger.info("Item #{} marked as MATCHED with highest score of {}", newItem.getId(), matches.get(0).getScore());
                }
            }
        } catch (Exception e) {
            logger.warn("Auto-match evaluation encountered non-critical error: {}", e.getMessage());
        }
    }

    /**
     * Check if categories match (case-insensitive trim match).
     */
    private boolean isCategoryMatch(String cat1, String cat2) {
        if (cat1 == null || cat2 == null) return false;
        return cat1.trim().equalsIgnoreCase(cat2.trim());
    }

    /**
     * Check if locations match (case-insensitive exact or substring containment).
     */
    private boolean isLocationMatch(String loc1, String loc2) {
        if (loc1 == null || loc2 == null) return false;
        String s1 = loc1.trim().toLowerCase();
        String s2 = loc2.trim().toLowerCase();
        return s1.equals(s2) || s1.contains(s2) || s2.contains(s1);
    }

    /**
     * Tokenize text into cleaned, unique lowercase keywords excluding stop words and single letters.
     */
    public Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }

        String[] tokens = WORD_SPLIT_PATTERN.split(text.toLowerCase());
        Set<String> keywords = new HashSet<>();

        for (String token : tokens) {
            token = token.trim();
            // Retain alphanumeric tokens of length >= 3 not present in stop words
            if (token.length() >= 3 && !STOP_WORDS.contains(token) && token.matches("[a-zA-Z0-9]+")) {
                keywords.add(token);
            }
        }

        return keywords;
    }
}
