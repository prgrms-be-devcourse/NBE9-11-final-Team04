package com.team04.domain.dispute.repository;

import com.team04.domain.dispute.entity.Dispute;
import com.team04.domain.dispute.entity.DisputeCategory;
import com.team04.domain.dispute.entity.DisputeStatus;
import com.team04.domain.dispute.entity.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
            Long reporterId, TargetType targetType, Long targetId, List<DisputeStatus> statuses);


    @Query("SELECT d FROM Dispute d JOIN FETCH d.reporter JOIN FETCH d.reported LEFT JOIN FETCH d.appeal WHERE d.id = :id")
    Optional<Dispute> findByIdWithDetails(@Param("id") Long id);

    @Query(
            value = """
                    SELECT d FROM Dispute d JOIN FETCH d.reporter JOIN FETCH d.reported
                    WHERE (:status IS NULL OR d.status = :status)
                      AND (:category IS NULL OR d.category = :category)
                      AND (:targetType IS NULL OR d.targetType = :targetType)
                    ORDER BY d.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(d) FROM Dispute d
                    WHERE (:status IS NULL OR d.status = :status)
                      AND (:category IS NULL OR d.category = :category)
                      AND (:targetType IS NULL OR d.targetType = :targetType)
                    """
    )
    Page<Dispute> findAllByFilters(
            @Param("status") DisputeStatus status,
            @Param("category") DisputeCategory category,
            @Param("targetType") TargetType targetType,
            Pageable pageable
    );

    @Query("SELECT d FROM Dispute d JOIN FETCH d.reporter JOIN FETCH d.reported WHERE d.reporter.id = :reporterId ORDER BY d.createdAt DESC")
    Page<Dispute> findAllByReporterId(@Param("reporterId") Long reporterId, Pageable pageable);

    @Query("SELECT d FROM Dispute d JOIN FETCH d.reporter JOIN FETCH d.reported WHERE d.reported.id = :reportedId ORDER BY d.createdAt DESC")
    Page<Dispute> findAllByReportedId(@Param("reportedId") Long reportedId, Pageable pageable);

    @Query("SELECT d.status, COUNT(d) FROM Dispute d GROUP BY d.status")
    List<Object[]> countGroupByStatus();
}
