package com.QhomeBase.customerinteractionservice.repository;

import com.QhomeBase.customerinteractionservice.model.NewsImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NewsImageRepository extends JpaRepository<NewsImage, UUID> {
    
    @Query("SELECT ni FROM NewsImage ni WHERE ni.news.id = :newsId ORDER BY ni.sortOrder ASC")
    List<NewsImage> findByNewsId(@Param("newsId") UUID newsId);
}
