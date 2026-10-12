package com.lostandfound.dto;

import java.util.List;

public class MatchResultDTO {
    private ItemResponseDTO item;
    private int score;
    private List<String> matchReasons;

    public MatchResultDTO() {
    }

    public MatchResultDTO(ItemResponseDTO item, int score, List<String> matchReasons) {
        this.item = item;
        this.score = score;
        this.matchReasons = matchReasons;
    }

    public ItemResponseDTO getItem() {
        return item;
    }

    public void setItem(ItemResponseDTO item) {
        this.item = item;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<String> getMatchReasons() {
        return matchReasons;
    }

    public void setMatchReasons(List<String> matchReasons) {
        this.matchReasons = matchReasons;
    }
}
