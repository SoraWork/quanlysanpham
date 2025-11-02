package com.hoaiphong.quanlysanpham.repository;


import com.hoaiphong.quanlysanpham.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByCategoryCode(String categoryCode);
    Optional<Category> findByCategoryCode(String categoryCode);
    boolean existsByCategoryCodeAndIdNot(String categoryCode, Long id);

        @Query("""
            select c from Category c
            where c.status = '1'
                AND c.id IN :ids
    """)
        List<Category> findAllById(@Param("ids")List<Long> ids);

    @Query("""
        SELECT c FROM Category c
        WHERE c.status = '1'
              AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) ESCAPE '\\')
              AND (:categoryCode IS NULL OR LOWER(c.categoryCode) LIKE LOWER(CONCAT('%', :categoryCode, '%')) ESCAPE '\\')
              AND (:createdFrom IS NULL OR c.createdDate >= :createdFrom)
              AND (:createdTo IS NULL OR c.createdDate <= :createdTo)
        """
    )
    Page<Category> searchCategories(
            String name,
            String categoryCode,
            Date createdFrom,
            Date createdTo,
            Pageable pageable
    );

    @Query("""
    SELECT c FROM Category c
    LEFT JOIN FETCH c.images
    WHERE c.id = :id
      AND c.status = '1'
""")
    Optional<Category> findByIdWithActiveStatus(Long id);

    @Query("""
        SELECT c FROM Category c
        WHERE c.status = '1'
              AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) ESCAPE '\\')
               AND (:categoryCode IS NULL OR LOWER(c.categoryCode) LIKE LOWER(CONCAT('%', :categoryCode, '%')) ESCAPE '\\')
              AND (:createdFrom IS NULL OR c.createdDate >= :createdFrom)
              AND (:createdTo IS NULL OR c.createdDate <= :createdTo)
        """
    )
    List<Category> searchAllForExport(
            String name,
            String categoryCode,
            Date createdFrom,
            Date createdTo,
            Pageable pageable
    );

}