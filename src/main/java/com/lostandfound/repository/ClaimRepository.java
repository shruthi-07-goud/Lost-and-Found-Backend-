package com.lostandfound.repository;

import com.lostandfound.model.Claim;
import com.lostandfound.model.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByClaimedByIdOrderByCreatedAtDesc(Long userId);

    List<Claim> findByItemReportedByIdOrderByCreatedAtDesc(Long userId);

    List<Claim> findByItemIdOrderByCreatedAtDesc(Long itemId);

    boolean existsByItemIdAndClaimedByIdAndStatus(Long itemId, Long claimedById, ClaimStatus status);

    Optional<Claim> findByItemIdAndStatus(Long itemId, ClaimStatus status);

    List<Claim> findByItemIdAndStatusNot(Long itemId, ClaimStatus status);
}
