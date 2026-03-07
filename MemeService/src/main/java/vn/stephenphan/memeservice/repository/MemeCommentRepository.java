package vn.stephenphan.memeservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.stephenphan.memeservice.model.MemeComment;

import java.util.List;

@Repository
public interface MemeCommentRepository extends JpaRepository<MemeComment, String> {

    Page<MemeComment> findByMemeIdAndParentIdIsNullOrderByCreatedAtDesc(String memeId, Pageable pageable);

    List<MemeComment> findByParentIdOrderByCreatedAtAsc(String parentId);

    List<MemeComment> findByMemeId(String memeId);

    long countByMemeId(String memeId);

    @Query("SELECT COUNT(c) FROM MemeComment c WHERE c.memeId = :memeId AND c.parentId IS NULL")
    long countRootCommentsByMemeId(@Param("memeId") String memeId);

    List<MemeComment> findByUserIdOrderByCreatedAtDesc(String userId);
}
