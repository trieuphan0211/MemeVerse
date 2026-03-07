package vn.stephenphan.memeservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.stephenphan.memeservice.model.Report;
import vn.stephenphan.memeservice.model.ReportStatus;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, String> {

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    @Query("SELECT r FROM Report r WHERE r.memeId = :memeId")
    List<Report> findByMemeId(@Param("memeId") String memeId);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.memeId = :memeId AND r.status = 'PENDING'")
    long countPendingReportsByMemeId(@Param("memeId") String memeId);

    boolean existsByMemeIdAndReporterId(String memeId, String reporterId);

    @Query("SELECT r.memeId, COUNT(r) as reportCount FROM Report r WHERE r.status = 'PENDING' GROUP BY r.memeId ORDER BY reportCount DESC")
    List<Object[]> findMostReportedMemes(org.springframework.data.domain.Pageable pageable);
}
