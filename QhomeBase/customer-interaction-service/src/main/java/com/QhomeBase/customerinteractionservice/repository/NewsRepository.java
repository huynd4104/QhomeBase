package com.QhomeBase.customerinteractionservice.repository;

import com.QhomeBase.customerinteractionservice.model.News;
import com.QhomeBase.customerinteractionservice.model.NewsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NewsRepository extends JpaRepository<News, UUID> {

    /**
     * Tìm tất cả news có status SCHEDULED và publish_at <= thời điểm hiện tại
     */
    @Query("SELECT n FROM News n WHERE n.status = :status AND n.publishAt <= :now")
    List<News> findByStatusAndPublishAtLessThanEqual(
            @Param("status") NewsStatus status,
            @Param("now") Instant now
    );
}
