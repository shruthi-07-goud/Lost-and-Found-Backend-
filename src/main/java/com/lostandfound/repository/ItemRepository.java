package com.lostandfound.repository;

import com.lostandfound.model.Item;
import com.lostandfound.model.ItemStatus;
import com.lostandfound.model.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByTypeAndStatusIn(ItemType type, Collection<ItemStatus> statuses);

    List<Item> findByReportedByIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT i FROM Item i WHERE " +
           "(:type IS NULL OR i.type = :type) AND " +
           "(:category IS NULL OR LOWER(i.category) = LOWER(:category)) AND " +
           "(:location IS NULL OR LOWER(i.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:search IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY i.createdAt DESC")
    List<Item> searchItems(
            @Param("type") ItemType type,
            @Param("category") String category,
            @Param("location") String location,
            @Param("status") ItemStatus status,
            @Param("search") String search
    );
}
